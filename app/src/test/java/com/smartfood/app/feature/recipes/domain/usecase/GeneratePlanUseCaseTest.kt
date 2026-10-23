package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.recipes.domain.agent.MealPlanner
import com.smartfood.app.feature.recipes.domain.agent.PlanExplainer
import com.smartfood.app.feature.recipes.domain.agent.RealCatalog
import com.smartfood.app.feature.recipes.domain.model.AiPlanOutcome
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: GeneratePlanUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de [GeneratePlanUseCase] con el catálogo real (spec 009, T014): el plan respeta las restricciones
 * del Perfil (CA-RIA-02-09) y trae las razones de "¿Por qué te conviene?" (CA-RIA-02-06); sin presupuesto suficiente
 * devuelve "Sin coincidencias" con el presupuesto mínimo (CA-RIA-02-07).
 */
class GeneratePlanUseCaseTest {

    @Test
    fun `CA-RIA-02-09 CA-RIA-02-06 con Vegano el plan respeta la dieta y explica por que conviene`() = runTest {
        val fixtures = AiFixtures(setOf(DietaryRestriction.VEGAN))
        val context = fixtures.getAiContext()

        val outcome = fixtures.generatePlan(context, budgetCop = 100_000, servings = 4, maxMinutes = null, noSpicy = false)

        assertTrue("Esperaba un plan: $outcome", outcome is AiPlanOutcome.Found)
        val found = outcome as AiPlanOutcome.Found
        assertEquals(3, found.plan.days.size)
        val planner = MealPlanner(RealCatalog.ingredients)
        val diet = PlanContext(budgetCop = 100_000, servings = 4, diet = context.diet)
        found.plan.days.forEach { assertTrue("${it.recipe.id} no es vegana", planner.fitsDiet(it.recipe, diet)) }
        assertTrue(found.reasons.any { it is PlanExplainer.Reason.UsesPantry })
        assertTrue(found.reasons.any { it is PlanExplainer.Reason.BudgetLeft })
    }

    @Test
    fun `CA-RIA-02-07 con COP 2000 no hay plan y sugiere subir el presupuesto`() = runTest {
        val fixtures = AiFixtures()

        val outcome = fixtures.generatePlan(fixtures.getAiContext(), budgetCop = 2_000, servings = 4, maxMinutes = null, noSpicy = false)

        assertTrue("Esperaba sin coincidencias: $outcome", outcome is AiPlanOutcome.NoPlan)
        assertTrue((outcome as AiPlanOutcome.NoPlan).suggestions.any { it is PlanSuggestion.RaiseBudget })
    }
}
