package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: MealPlannerCatalogTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas del agente [MealPlanner] con el catálogo real de `src/main/assets` (spec 009, T037): con un
 * contexto común (una despensa básica, 4 porciones, COP $100.000, D1), el agente arma un plan de 3 días sin restricción
 * y con cada restricción alimentaria, y el plan la respeta. Con el inventario vacío ninguna receta vegana cumple el
 * máximo de 3 faltantes por día, así que la prueba parte de lo que suele haber en una cocina colombiana. Corre en la JVM con
 * [RealCatalog].
 */
class MealPlannerCatalogTest {

    /** Lo que suele haber en una cocina colombiana, sin cantidad: cada producto alcanza para una receta. */
    private val despensa = listOf("arroz", "papa", "cebolla", "tomate", "ajo", "zanahoria").map { id ->
        PantryItem(itemId = id, ingredientId = id, quantity = null, daysToExpire = 7)
    }

    private fun context(diet: Set<DietRule>) =
        PlanContext(budgetCop = 100_000, servings = 4, diet = diet, store = Store.D1)

    @Test
    fun `CA-RIA-02-09 con el catálogo real hay plan de 3 días sin restricción y con cada restricción`() {
        val recipes = RealCatalog.recipes
        val planner = MealPlanner(RealCatalog.ingredients)
        val casos = listOf(emptySet<DietRule>()) + DietRule.entries.map { setOf(it) }

        casos.forEach { diet ->
            val result = planner.plan(recipes, despensa, context(diet))
            assertTrue("Sin plan con la restricción $diet: $result", result is PlanResult.Found)
            val plan = (result as PlanResult.Found).plan
            assertEquals("$diet: 3 recetas distintas", 3, plan.days.map { it.recipe.id }.toSet().size)
            plan.days.forEach { day ->
                assertTrue("$diet: ${day.recipe.id} no cumple la dieta", planner.fitsDiet(day.recipe, context(diet)))
            }
            assertTrue("$diet: el plan supera el presupuesto", plan.totalCostCop <= 100_000)
        }
    }
}
