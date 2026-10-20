package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: MealPlannerPerformanceTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Rendimiento del agente (spec 009, CA-RIA-02-10, RNF-RIA-02, T013): con las 50 recetas del catálogo real,
 * el plan de cada uno de los 30 inventarios sintéticos se calcula en 2 segundos o menos. En la JVM es orientativo; la
 * medición real se hace en el teléfono con el APK release (T028).
 */
class MealPlannerPerformanceTest {

    private val planner = MealPlanner(RealCatalog.ingredients)
    private val context = PlanContext(budgetCop = 100_000, servings = 4, store = Store.D1)

    @Test
    fun `CA-RIA-02-10 RNF-RIA-02 con 50 recetas cada plan se calcula en 2 segundos o menos`() {
        val recipes = RealCatalog.recipes
        val inventories = RealCatalog.syntheticInventories()
        assertEquals(50, recipes.size)
        planner.plan(recipes, inventories.first(), context) // Calentamiento de la JVM.

        val millis = inventories.map { pantry ->
            val start = System.nanoTime()
            planner.plan(recipes, pantry, context)
            (System.nanoTime() - start) / 1_000_000
        }

        println("\nRendimiento del agente (CA-RIA-02-10): máximo ${millis.max()} ms, promedio ${millis.average().toLong()} ms")
        assertTrue("El plan más lento tardó ${millis.max()} ms", millis.max() <= 2_000)
    }
}
