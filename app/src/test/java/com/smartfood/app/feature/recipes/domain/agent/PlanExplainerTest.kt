package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.agent.PlanExplainer.Reason
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: PlanExplainerTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [PlanExplainer] (spec 009, CA-RIA-02-06, T012): las razones de "¿Por qué te
 * conviene?" salen de los datos del plan y van en el orden de la tarjeta.
 */
class PlanExplainerTest {

    private val ingredients = listOf(
        ingredient("pollo", priceCop = 14_000),
        ingredient("arroz", priceCop = 6_000),
        ingredient("cilantro", priceCop = 60_000),
        ingredient("pasta", priceCop = 8_000),
        ingredient("tomate", priceCop = 4_000),
    ).associateBy { it.id }
    private val recetas = listOf(
        recipe("a-pollo-guisado", "pollo" to 200.0, "arroz" to 300.0, "cilantro" to 20.0),
        recipe("b-pasta", "pasta" to 250.0, "tomate" to 200.0),
        recipe("c-arroz-tomate", "arroz" to 200.0, "tomate" to 100.0),
    )
    private val despensa = listOf(
        pantry("pollo-1", "pollo", 200.0, 1),
        pantry("arroz-1", "arroz", 1000.0, 30),
        pantry("tomate-1", "tomate", 300.0, 2),
        pantry("pasta-1", "pasta", 500.0, 10),
        pantry("leche-1", "leche", 1000.0, 1),
    )

    @Test
    fun `CA-RIA-02-06 explica que rescata, que aprovecha, que falta y cuanto queda`() {
        val plan = (MealPlanner(ingredients).plan(recetas, despensa, PlanContext(budgetCop = 15_000, servings = 4))
            as PlanResult.Found).plan
        val reasons = PlanExplainer().explain(plan, despensa)

        val rescates = reasons.filterIsInstance<Reason.RescuesExpiring>()
        assertTrue("rescata el pollo que vence mañana", rescates.any { it.ingredientId == "pollo" && it.daysToExpire == 1 && it.quantity == 200.0 })
        assertTrue("rescata el tomate", rescates.any { it.ingredientId == "tomate" })

        val uso = reasons.filterIsInstance<Reason.UsesPantry>().single()
        assertEquals(7, uso.total)
        assertEquals(6, uso.covered)

        // Solo falta el cilantro: 20 g a 60.000/kg = 1.200.
        assertEquals(listOf(Reason.NeedsToBuy("cilantro", 20.0, 1_200)), reasons.filterIsInstance<Reason.NeedsToBuy>())
        assertEquals(Reason.BudgetLeft(13_800), reasons.filterIsInstance<Reason.BudgetLeft>().single())

        // La leche vence mañana y ninguna receta la usa.
        assertEquals(Reason.NotUsed(listOf("leche-1")), reasons.last())
    }

    @Test
    fun `CA-RIA-02-06 el orden es rescates, aprovechamiento, compras, presupuesto y lo que no se usa`() {
        val plan = (MealPlanner(ingredients).plan(recetas, despensa, PlanContext(budgetCop = 15_000, servings = 4))
            as PlanResult.Found).plan
        val kinds = PlanExplainer().explain(plan, despensa).map { it::class.simpleName }.distinct()
        assertEquals(listOf("RescuesExpiring", "UsesPantry", "NeedsToBuy", "BudgetLeft", "NotUsed"), kinds)
    }

    @Test
    fun `CA-RIA-02-06 si no hay que comprar nada lo dice`() {
        val todo = despensa + pantry("cilantro-1", "cilantro", 50.0, 4)
        val plan = (MealPlanner(ingredients).plan(recetas, todo, PlanContext(budgetCop = 0, servings = 4))
            as PlanResult.Found).plan
        val reasons = PlanExplainer().explain(plan, todo)

        assertTrue(Reason.NothingToBuy in reasons)
        assertEquals(Reason.BudgetLeft(0), reasons.filterIsInstance<Reason.BudgetLeft>().single())
    }
}
