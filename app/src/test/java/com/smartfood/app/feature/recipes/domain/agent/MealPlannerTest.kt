package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.model.FoodTag
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.MealPlan
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
import com.smartfood.app.feature.recipes.domain.model.PlannerWeights
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: MealPlannerTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias del agente [MealPlanner] (spec 009 §2, plan §3.4, T011): cada restricción del CSP, la
 * optimalidad del plan contra una búsqueda exhaustiva (fuerza bruta) en catálogos aleatorios con semilla fija, el
 * determinismo, las sugerencias cuando no hay plan y las restricciones alimentarias (T012a).
 */
class MealPlannerTest {

    private val ingredients = listOf(
        ingredient("pollo", 14_000, IngredientUnit.GRAM, FoodTag.MEAT, FoodTag.ANIMAL_PRODUCT),
        ingredient("arroz", priceCop = 6_000),
        ingredient("pasta", 8_000, IngredientUnit.GRAM, FoodTag.GLUTEN),
        ingredient("tomate", priceCop = 4_000),
        ingredient("huevo", 12_000, IngredientUnit.GRAM, FoodTag.ANIMAL_PRODUCT),
        ingredient("aji", priceCop = 3_000),
        ingredient("queso", 20_000, IngredientUnit.GRAM, FoodTag.LACTOSE, FoodTag.ANIMAL_PRODUCT),
        ingredient("lenteja", priceCop = 7_000),
    ).associateBy { it.id }
    private val planner = MealPlanner(ingredients)

    private val catalogo = listOf(
        recipe("arroz-pollo", "pollo" to 200.0, "arroz" to 300.0, minutes = 40),
        recipe("pasta-tomate", "pasta" to 250.0, "tomate" to 300.0, minutes = 25),
        recipe("huevos-arroz", "huevo" to 200.0, "arroz" to 200.0, minutes = 15),
        recipe("pollo-picante", "pollo" to 300.0, "aji" to 50.0, minutes = 30, spicy = true),
        recipe("tortilla", "huevo" to 300.0, "tomate" to 100.0, minutes = 20),
    )
    private val despensa = listOf(
        pantry("p1", "pollo", 500.0, 1),
        pantry("a1", "arroz", 1000.0, 20),
        pantry("t1", "tomate", 400.0, 2),
        pantry("h1", "huevo", null, 5),
    )

    private fun ctx(
        budget: Long = 50_000,
        maxMinutes: Int? = null,
        noSpicy: Boolean = false,
        diet: Set<DietRule> = emptySet(),
    ) = PlanContext(budgetCop = budget, servings = 4, maxMinutes = maxMinutes, noSpicy = noSpicy, diet = diet)

    private fun found(result: PlanResult): MealPlan = (result as PlanResult.Found).plan

    @Test
    fun `CA-RIA-02-03 CA-RIA-02-05 el plan tiene 3 recetas distintas que cumplen tiempo, picante, faltantes y presupuesto`() {
        val plan = found(planner.plan(catalogo, despensa, ctx(maxMinutes = 35, noSpicy = true)))

        assertEquals(listOf(1, 2, 3), plan.days.map { it.day })
        assertEquals("distintas", 3, plan.days.map { it.recipe.id }.toSet().size)
        plan.days.forEach { d ->
            assertTrue("tiempo ${d.recipe.id}", d.recipe.totalMinutes <= 35)
            assertTrue("picante ${d.recipe.id}", !d.recipe.spicy)
            assertTrue("faltantes ${d.recipe.id}", d.uses.count { it.isMissing } <= 3)
        }
        assertTrue("presupuesto", plan.totalCostCop <= 50_000)
        assertEquals(50_000 - plan.totalCostCop, plan.remainingBudgetCop)
    }

    @Test
    fun `CA-RIA-02-05 el inventario es compartido entre los dias y el presupuesto es total`() {
        // Solo 500 g de pollo: dos recetas con pollo no pueden sacarlo las dos del inventario.
        val plan = found(planner.plan(catalogo, despensa, ctx()))
        val polloDelInventario = plan.days.flatMap { it.uses }.filter { it.ingredientId == "pollo" }
            .sumOf { it.fromPantry["p1"] ?: 0.0 }
        assertTrue("no usa más pollo del que hay", polloDelInventario <= 500.0 + 1e-9)

        // Con un presupuesto que solo alcanza para una receta con compras, no hay plan de 3.
        val apretado = planner.plan(catalogo, emptyList(), ctx(budget = 6_000))
        assertTrue(apretado is PlanResult.NoPlan)
    }

    @Test
    fun `CA-RIA-02-04 el plan es el de mayor utilidad comparado contra la fuerza bruta`() {
        var withPlan = 0
        repeat(40) { seed ->
            val s = randomScenario(seed, recipeCount = 6 + seed % 7)
            val context = PlanContext(
                budgetCop = listOf(25_000L, 60_000L, 150_000L)[seed % 3],
                servings = listOf(2, 4, 6)[seed % 3],
                maxMinutes = if (seed % 4 == 0) 45 else null,
                noSpicy = seed % 5 == 0,
                store = if (seed % 2 == 0) Store.EXITO else Store.D1,
                diet = when (seed % 6) {
                    1 -> setOf(DietRule.VEGETARIAN)
                    4 -> setOf(DietRule.GLUTEN_FREE)
                    else -> emptySet()
                },
            )
            val expected = bruteForce(s, context)
            val actual = MealPlanner(s.ingredients).search(s.recipes, s.pantry, context)

            if (expected == null) {
                assertNull("semilla $seed: no debería haber plan", actual)
            } else {
                withPlan++
                assertEquals("semilla $seed: utilidad", expected.first, actual!!.utility, 1e-9)
                assertEquals("semilla $seed: plan", expected.second, actual.days.map { it.recipe.id })
            }
        }
        // La comparación debe cubrir sobre todo escenarios con plan, no solo los que no tienen.
        assertTrue("escenarios con plan: $withPlan de 40", withPlan >= 20)
    }

    @Test
    fun `RNF-RIA-01 el mismo inventario y contexto dan siempre el mismo plan`() {
        val a = planner.plan(catalogo, despensa, ctx())
        val b = MealPlanner(ingredients).plan(catalogo.reversed(), despensa.reversed(), ctx())
        assertEquals(found(a).days.map { it.recipe.id }, found(b).days.map { it.recipe.id })
    }

    @Test
    fun `CA-RIA-02-04 prioriza lo que vence antes y lo usa el dia en que todavia sirve`() {
        // El pollo vence mañana: la receta con pollo debe ir el día 1 o 2.
        val plan = found(planner.plan(catalogo, despensa, ctx(noSpicy = true)))
        val diaPollo = plan.days.first { d -> d.uses.any { it.ingredientId == "pollo" && "p1" in it.fromPantry } }.day
        assertTrue("el pollo se usa a tiempo (día $diaPollo)", diaPollo <= 2)
    }

    @Test
    fun `CA-RIA-02-07 sin plan sugiere el presupuesto minimo, quitar picante o mas tiempo`() {
        val sinPresupuesto = planner.plan(catalogo, emptyList(), ctx(budget = 2_000)) as PlanResult.NoPlan
        val subir = sinPresupuesto.suggestions.filterIsInstance<PlanSuggestion.RaiseBudget>().single()
        // El presupuesto sugerido sí produce un plan.
        assertTrue(planner.plan(catalogo, emptyList(), ctx(budget = subir.minimumBudgetCop)) is PlanResult.Found)
        assertTrue("redondeado a mil", subir.minimumBudgetCop % 1000 == 0L)

        val soloPicantes = listOf(
            recipe("a", "aji" to 10.0, spicy = true), recipe("b", "aji" to 20.0, spicy = true), recipe("c", "aji" to 30.0, spicy = true)
        )
        val picante = planner.plan(soloPicantes, emptyList(), ctx(noSpicy = true)) as PlanResult.NoPlan
        assertTrue(PlanSuggestion.AllowSpicy in picante.suggestions)

        val lentas = catalogo.map { it.copy(cookMinutes = 60) }
        val tiempo = planner.plan(lentas, despensa, ctx(maxMinutes = 30)) as PlanResult.NoPlan
        assertTrue(tiempo.suggestions.any { it is PlanSuggestion.AllowMoreTime })

        val pocas = planner.plan(catalogo.take(2), despensa, ctx()) as PlanResult.NoPlan
        assertEquals(listOf(PlanSuggestion.NotEnoughRecipes), pocas.suggestions)
    }

    @Test
    fun `CA-RIA-02-09 vegetariano excluye las recetas con carne y vegano tambien las de huevo o queso`() {
        val conVerduras = catalogo + listOf(
            recipe("lentejas", "lenteja" to 300.0, "tomate" to 100.0),
            recipe("arroz-tomate", "arroz" to 300.0, "tomate" to 200.0),
            recipe("pasta-queso", "pasta" to 250.0, "queso" to 100.0),
        )

        val vegetariano = found(planner.plan(conVerduras, despensa, ctx(diet = setOf(DietRule.VEGETARIAN))))
        assertTrue(vegetariano.days.none { d -> d.recipe.ingredients.any { it.ingredientId == "pollo" } })

        val vegano = found(planner.plan(conVerduras, despensa, ctx(diet = setOf(DietRule.VEGAN))))
        assertEquals(setOf("lentejas", "arroz-tomate", "pasta-tomate"), vegano.days.map { it.recipe.id }.toSet())
    }

    @Test
    fun `CA-RIA-02-09 sin gluten y sin lactosa excluyen la pasta y el queso, y las reglas se suman`() {
        val pastaQueso = recipe("pasta-queso", "pasta" to 250.0, "queso" to 100.0)

        assertTrue(planner.fitsDiet(pastaQueso, ctx()))
        assertFalse(planner.fitsDiet(pastaQueso, ctx(diet = setOf(DietRule.GLUTEN_FREE))))
        assertFalse(planner.fitsDiet(pastaQueso, ctx(diet = setOf(DietRule.LACTOSE_FREE))))
        assertFalse(planner.fitsDiet(pastaQueso, ctx(diet = setOf(DietRule.NUT_FREE, DietRule.VEGAN))))
        assertTrue(planner.fitsDiet(pastaQueso, ctx(diet = setOf(DietRule.NUT_FREE, DietRule.VEGETARIAN))))
    }

    @Test
    fun `CA-RIA-02-07 CA-RIA-02-09 sin plan por la dieta no sugiere quitarla`() {
        // Con el catálogo base, vegano solo admite una receta (pasta con tomate): aun con presupuesto y tiempo de sobra no hay plan.
        val resultado = planner.plan(catalogo, despensa, ctx(budget = 1_000_000, diet = setOf(DietRule.VEGAN)))

        assertEquals(listOf(PlanSuggestion.NotEnoughRecipes), (resultado as PlanResult.NoPlan).suggestions)
    }

    /**
     * Búsqueda exhaustiva: prueba todas las secuencias de recetas distintas, simulando el inventario y el presupuesto
     * día a día con el mismo evaluador. Devuelve la utilidad máxima y sus ids (desempate por ids menores).
     */
    private fun bruteForce(scenario: RandomScenario, context: PlanContext): Pair<Double, List<String>>? {
        val recipes = scenario.recipes
        val evaluator = RecipeEvaluator(scenario.ingredients, PlannerWeights())
        var best: Pair<Double, List<String>>? = null
        fun go(seq: List<Recipe>, stock: PantryStock, spent: Long, partial: Double) {
            if (seq.size == context.days) {
                val u = partial - PlannerWeights().waste * evaluator.waste(stock)
                val ids = seq.map { it.id }
                val b = best
                if (b == null || u > b.first + 1e-9 || (u >= b.first - 1e-9 && ids.joinToString() < b.second.joinToString())) {
                    best = u to ids
                }
                return
            }
            for (r in recipes) {
                if (seq.any { it.id == r.id }) continue
                if (context.maxMinutes != null && r.totalMinutes > context.maxMinutes) continue
                if (context.noSpicy && r.spicy) continue
                val forbidden = context.diet.flatMap { it.forbidden }
                if (r.ingredients.any { ri -> scenario.ingredients.getValue(ri.ingredientId).tags.any { it in forbidden } }) continue
                val e = evaluator.evaluate(r, context.servings, seq.size + 1, stock, context.store)
                if (e.missingCount > context.maxMissing || spent + e.costCop > context.budgetCop) continue
                go(seq + r, e.remaining, spent + e.costCop, partial + e.utility)
            }
        }
        go(emptyList(), PantryStock.of(scenario.pantry), 0, 0.0)
        return best
    }
}
