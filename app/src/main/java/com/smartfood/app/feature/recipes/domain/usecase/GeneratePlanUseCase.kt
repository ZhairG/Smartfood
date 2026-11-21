package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.agent.MealPlanner
import com.smartfood.app.feature.recipes.domain.agent.PlanExplainer
import com.smartfood.app.feature.recipes.domain.agent.SearchDeadline
import com.smartfood.app.feature.recipes.domain.model.AiContext
import com.smartfood.app.feature.recipes.domain.model.AiPlanOutcome
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.model.RecipeCuisine
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Nombre: GeneratePlanUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pide al agente el plan de 3 días (spec 009, CA-RIA-02-03, 02-06, 02-07 y 02-09, T014) con lo que la
 * persona eligió en el contexto y el [AiContext] (inventario emparejado, restricciones y tienda). Corre en
 * `Dispatchers.Default` para no trabar la pantalla; si se cancela la corrutina, el cálculo se descarta. Si hay plan,
 * agrega las razones de "¿Por qué te conviene?" con [PlanExplainer].
 *
 * La búsqueda tiene un límite de [PLAN_TIME_LIMIT] (enmienda del 2026-11-20, T048): al vencer devuelve el mejor plan
 * encontrado (CA-RIA-02-13) o [AiPlanOutcome.TimedOut] si no había ninguno (CA-RIA-02-14).
 *
 * @param catalog Recetas e ingredientes.
 * @param timeSource Reloj del límite; las pruebas usan un `TestTimeSource`.
 */
class GeneratePlanUseCase(
    private val catalog: RecipeCatalog,
    private val timeSource: TimeSource,
) {

    @Inject
    constructor(catalog: RecipeCatalog) : this(catalog, TimeSource.Monotonic)

    suspend operator fun invoke(
        context: AiContext,
        budgetCop: Long,
        servings: Int,
        maxMinutes: Int?,
        noSpicy: Boolean,
    ): AiPlanOutcome {
        val ingredients = catalog.ingredients()
        val recipes = catalog.recipes()
        return withContext(Dispatchers.Default) {
            val planContext = PlanContext(
                budgetCop = budgetCop,
                servings = servings,
                maxMinutes = maxMinutes,
                noSpicy = noSpicy,
                diet = context.diet,
                store = context.store,
                cuisines = context.cuisines.mapTo(LinkedHashSet()) { RecipeCuisine.valueOf(it.name) },
            )
            val deadline = SearchDeadline.after(PLAN_TIME_LIMIT, timeSource)
            when (val result = MealPlanner(ingredients).plan(recipes, context.pantry, planContext, deadline)) {
                is PlanResult.Found -> AiPlanOutcome.Found(
                    plan = result.plan,
                    reasons = PlanExplainer().explain(result.plan, context.pantry),
                    complete = result.complete,
                    nodes = result.nodes,
                )
                is PlanResult.NoPlan -> AiPlanOutcome.NoPlan(result.suggestions)
                is PlanResult.TimedOut -> AiPlanOutcome.TimedOut(result.nodes)
            }
        }
    }

    companion object {
        /** Lo máximo que busca el agente: la meta de RNF-RIA-02 (CA-RIA-02-13, 02-14). */
        val PLAN_TIME_LIMIT: Duration = 2.seconds
    }
}
