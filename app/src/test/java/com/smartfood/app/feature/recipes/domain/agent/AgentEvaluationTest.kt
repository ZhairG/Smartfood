package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.Store
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: AgentEvaluationTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Evaluación del agente con las métricas del Avance 2 (spec 009, RNF-RIA-03, T013). En 30 inventarios
 * sintéticos con semilla fija ([RealCatalog]) y el catálogo real, compara el plan de [MealPlanner] con dos líneas base
 * que arman el plan día por día con las mismas restricciones duras, pero ordenando por un solo criterio:
 * - **Por coincidencias:** la receta con más ingredientes en el inventario.
 * - **Por vencimiento:** la receta que usa el producto que vence antes.
 *
 * Métricas, impresas como tabla para el informe de IA:
 * - **Aprovechamiento del plan:** % de los productos que vencen en ≤ 3 días y alguna receta usa, que el plan usa antes
 *   de que venzan. Meta: ≥ 80 % y más que las dos líneas base.
 * - **Aprovechamiento del Día 1** (métrica del Avance 2, que recomendaba una sola receta): % de los productos por vencer
 *   que usa la receta del Día 1, en los escenarios donde una sola receta los usa todos. Se reporta sin meta: con el plan
 *   de 3 días, lo que vence en 2 o 3 días puede cocinarse después sin perderse (decisión de Zhair del 2026-10-20).
 * - **Presupuesto:** % de planes cuyas compras caben en el presupuesto (restricción dura: 100 %).
 * - Desperdicio y costo medios.
 */
class AgentEvaluationTest {

    private val recipes = RealCatalog.recipes
    private val evaluator = RecipeEvaluator(RealCatalog.ingredients)
    private val planner = MealPlanner(RealCatalog.ingredients)
    private val context = PlanContext(budgetCop = BUDGET, servings = 4, store = Store.D1)
    private val scenarios = RealCatalog.syntheticInventories()

    /** Plan de un método en un escenario: cada receta con los productos del inventario que usó, el costo y el desperdicio. */
    private data class Outcome(val days: List<Pair<Recipe, Set<String>>>, val costCop: Long, val waste: Double)

    private data class Metrics(
        val name: String,
        val complete: Int,
        val planShare: Double,
        val dayOneShare: Double,
        val withinBudget: Double,
        val waste: Double,
        val cost: Double,
    )

    @Test
    fun `RNF-RIA-03 el plan usa al menos el 80 % de lo que vence y supera a las dos líneas base`() {
        val agent = metrics("Agente CSP") { pantry -> agentPlan(pantry) }
        val byMatches = metrics("Línea base: coincidencias") { pantry -> greedyPlan(pantry, ::matchesScore) }
        val byExpiry = metrics("Línea base: vencimiento") { pantry -> greedyPlan(pantry, ::expiryScore) }

        printTable(listOf(agent, byMatches, byExpiry))

        assertEquals("El agente arma plan en los 30 escenarios", scenarios.size, agent.complete)
        assertTrue("Supera a la línea base por coincidencias", agent.planShare > byMatches.planShare)
        assertTrue("Supera a la línea base por vencimiento", agent.planShare > byExpiry.planShare)
        assertTrue("El plan usa ≥ 80 % de lo que vence (${percent(agent.planShare)})", agent.planShare >= 0.80)
        assertEquals("Todos los planes caben en el presupuesto", 1.0, agent.withinBudget, 0.0)
    }

    private fun metrics(name: String, plan: (List<PantryItem>) -> Outcome?): Metrics {
        val planShares = mutableListOf<Double>()
        val dayOneShares = mutableListOf<Double>()
        val outcomes = scenarios.map { pantry ->
            val outcome = plan(pantry)
            val expiring = pantry.filter { it.isExpiringSoon() }
            val usable = expiring.filter { item -> recipes.any { r -> r.ingredients.any { it.ingredientId == item.ingredientId } } }
            val used = outcome?.days?.flatMapTo(HashSet()) { it.second }.orEmpty()
            if (usable.isNotEmpty()) planShares += usable.count { it.itemId in used }.toDouble() / usable.size
            // Avance 2: solo cuentan los escenarios donde una receta usa todos los productos por vencer.
            val expiringIngredients = expiring.map { it.ingredientId }.toSet()
            if (recipes.any { r -> r.ingredients.map { it.ingredientId }.containsAll(expiringIngredients) }) {
                val dayOne = outcome?.days?.firstOrNull()?.second.orEmpty()
                dayOneShares += expiring.count { it.itemId in dayOne }.toDouble() / expiring.size
            }
            outcome
        }
        val complete = outcomes.filterNotNull().filter { it.days.size == context.days }
        return Metrics(
            name = name,
            complete = complete.size,
            planShare = planShares.average(),
            dayOneShare = dayOneShares.average(),
            withinBudget = complete.count { it.costCop <= context.budgetCop }.toDouble() / complete.size.coerceAtLeast(1),
            waste = complete.map { it.waste }.average(),
            cost = complete.map { it.costCop.toDouble() }.average(),
        )
    }

    private fun agentPlan(pantry: List<PantryItem>): Outcome? {
        val plan = (planner.plan(recipes, pantry, context) as? PlanResult.Found)?.plan ?: return null
        val days = plan.days.map { day -> day.recipe to day.uses.flatMapTo(HashSet()) { it.fromPantry.keys } }
        // El desperdicio se calcula igual que en las líneas base: lo que queda sin usar al final del plan.
        var stock = PantryStock.of(pantry)
        plan.days.forEach { stock = evaluator.evaluate(it.recipe, context.servings, it.day, stock, context.store).remaining }
        return Outcome(days, plan.totalCostCop, evaluator.waste(stock))
    }

    /**
     * Línea base voraz: cada día toma, entre las recetas que todavía cumplen (faltantes y presupuesto), la de mayor
     * [score]; desempata por id. Puede quedarse sin receta antes del Día 3.
     */
    private fun greedyPlan(pantry: List<PantryItem>, score: (Recipe, PantryStock, Int) -> Double): Outcome {
        var stock = PantryStock.of(pantry)
        var budgetLeft = context.budgetCop
        var cost = 0L
        val days = mutableListOf<Pair<Recipe, Set<String>>>()
        for (day in 1..context.days) {
            val chosen = days.map { it.first.id }.toSet()
            val (recipe, evaluation) = recipes.asSequence()
                .filter { it.id !in chosen }
                .map { it to evaluator.evaluate(it, context.servings, day, stock, context.store) }
                .filter { (_, e) -> e.missingCount <= context.maxMissing && e.costCop <= budgetLeft }
                .sortedWith(
                    compareByDescending<Pair<Recipe, RecipeEvaluator.Evaluation>> { score(it.first, stock, day) }
                        .thenBy { it.first.id },
                )
                .firstOrNull() ?: break
            days += recipe to evaluation.uses.flatMapTo(HashSet()) { it.fromPantry.keys }
            stock = evaluation.remaining
            budgetLeft -= evaluation.costCop
            cost += evaluation.costCop
        }
        return Outcome(days, cost, evaluator.waste(stock))
    }

    /** Ingredientes de la receta que todavía están en el inventario y sirven ese día. */
    private fun matchesScore(recipe: Recipe, stock: PantryStock, day: Int): Double =
        recipe.ingredients.count { ri -> usableItems(ri.ingredientId, stock, day).isNotEmpty() }.toDouble()

    /** Urgencia del producto que vence antes entre los que la receta usaría: `1/(días + 1)`, o 0 si no usa ninguno. */
    private fun expiryScore(recipe: Recipe, stock: PantryStock, day: Int): Double =
        recipe.ingredients
            .mapNotNull { ri -> usableItems(ri.ingredientId, stock, day).minOfOrNull { it.daysToExpire } }
            .minOrNull()
            ?.let { 1.0 / (it + 1) }
            ?: 0.0

    private fun usableItems(ingredientId: String, stock: PantryStock, day: Int): List<PantryItem> =
        stock.items.filter { it.ingredientId == ingredientId && it.isUsableOn(day) && stock.isAvailable(it.itemId) }

    private fun printTable(rows: List<Metrics>) {
        println("\nEvaluación del agente (RNF-RIA-03): ${scenarios.size} inventarios sintéticos, COP $BUDGET, 4 porciones, D1")
        println("| Método | Planes completos | Aprovechamiento del plan | Aprovechamiento del Día 1 | En presupuesto | Desperdicio medio | Costo medio |")
        println("|---|---|---|---|---|---|---|")
        rows.forEach { m ->
            val waste = "%.2f".format(Locale.ROOT, m.waste)
            val cost = "%,.0f".format(Locale.ROOT, m.cost)
            println(
                "| ${m.name} | ${m.complete}/${scenarios.size} | ${percent(m.planShare)} | ${percent(m.dayOneShare)} | " +
                    "${percent(m.withinBudget)} | $waste | COP $cost |",
            )
        }
    }

    private fun percent(value: Double): String = "%.1f %%".format(Locale.ROOT, value * 100)

    private companion object {
        const val BUDGET = 100_000L
    }
}
