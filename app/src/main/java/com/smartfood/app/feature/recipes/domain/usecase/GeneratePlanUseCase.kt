package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.agent.MealPlanner
import com.smartfood.app.feature.recipes.domain.agent.PlanExplainer
import com.smartfood.app.feature.recipes.domain.model.AiContext
import com.smartfood.app.feature.recipes.domain.model.AiPlanOutcome
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import javax.inject.Inject
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
 * @param catalog Recetas e ingredientes.
 */
class GeneratePlanUseCase @Inject constructor(
    private val catalog: RecipeCatalog,
) {

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
            )
            when (val result = MealPlanner(ingredients).plan(recipes, context.pantry, planContext)) {
                is PlanResult.Found -> AiPlanOutcome.Found(result.plan, PlanExplainer().explain(result.plan, context.pantry))
                is PlanResult.NoPlan -> AiPlanOutcome.NoPlan(result.suggestions)
            }
        }
    }
}
