package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.MealPlan
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlannerWeights
import com.smartfood.app.feature.recipes.domain.model.Store
import java.io.File
import java.util.Locale
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Nombre: AgentSensitivityTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Análisis de sensibilidad del agente (spec 009, RNF-RIA-03, T043), que pidió el informe de IA:
 * - **Pesos de la utilidad** (`1,0 · 0,3 · 0,5`): cambia uno a la vez a la mitad y al doble (7 configuraciones) y mide,
 *   en los 30 inventarios de `AgentEvaluationTest`, los productos usados sobre los elegibles (n/N), la cantidad
 *   consumida, el desperdicio, el costo, la utilidad con los pesos base y cuántos planes son iguales al de los pesos base.
 *   Todas las métricas se calculan con los pesos base, para que las filas se puedan comparar.
 * - **Recocido simulado con varias semillas:** 10 semillas con el presupuesto fijo de 3.000 iteraciones y otras 10 con
 *   tantas iteraciones como nodos visitó el agente en ese inventario (igual presupuesto de cómputo). Reporta la brecha
 *   con el óptimo (media, desviación entre semillas y peor caso) y el % de corridas que llegan al óptimo.
 *
 * Tarda minutos, así que solo corre con `SMARTFOOD_SENSITIVITY=1` y deja el resultado en
 * `app/build/sensibilidad-agente.md`.
 */
class AgentSensitivityTest {

    private val recipes = RealCatalog.recipes
    private val ingredients = RealCatalog.ingredients
    private val context = PlanContext(budgetCop = 100_000, servings = 4, store = Store.D1)
    private val scenarios = RealCatalog.syntheticInventories()
    private val base = PlannerWeights()
    private val baseEvaluator = RecipeEvaluator(ingredients, base)

    @Test
    fun `RNF-RIA-03 sensibilidad de los pesos y del recocido con varias semillas`() {
        assumeTrue(System.getenv("SMARTFOOD_SENSITIVITY") == "1")
        val out = StringBuilder()
        out.append(weightsTable())
        out.append(annealingTable())
        File("build/sensibilidad-agente.md").writeText(out.toString())
        println(out)
    }

    // ---------- Pesos ----------

    private data class WeightRow(
        val name: String,
        val plans: Int,
        val used: Int,
        val eligible: Int,
        val consumed: Double,
        val waste: Double,
        val cost: Double,
        val baseUtility: Double,
        val sameAsBase: Int,
    )

    private fun weightConfigs(): List<Pair<String, PlannerWeights>> = listOf(
        "Base (1,0 · 0,3 · 0,5)" to base,
        "Rescate × 0,5" to base.copy(rescue = base.rescue * 0.5),
        "Rescate × 2" to base.copy(rescue = base.rescue * 2),
        "Costo × 0,5" to base.copy(cost = base.cost * 0.5),
        "Costo × 2" to base.copy(cost = base.cost * 2),
        "Desperdicio × 0,5" to base.copy(waste = base.waste * 0.5),
        "Desperdicio × 2" to base.copy(waste = base.waste * 2),
    )

    private fun weightsTable(): String {
        val basePlans = scenarios.map { MealPlanner(ingredients, base).search(recipes, it, context) }
        val rows = weightConfigs().map { (name, weights) ->
            val planner = MealPlanner(ingredients, weights)
            var used = 0
            var eligible = 0
            val consumed = mutableListOf<Double>()
            val wastes = mutableListOf<Double>()
            val costs = mutableListOf<Double>()
            val utilities = mutableListOf<Double>()
            var plans = 0
            var same = 0
            scenarios.forEachIndexed { index, pantry ->
                val plan = planner.search(recipes, pantry, context)
                val usable = usable(pantry)
                val stock = plan?.let { finalStock(it, pantry) }
                val usedIds = plan?.days?.flatMap { d -> d.uses.flatMap { it.fromPantry.keys } }?.toSet().orEmpty()
                used += usable.count { it.itemId in usedIds }
                eligible += usable.size
                if (usable.isNotEmpty()) consumed += usable.map { 1.0 - (stock?.unusedFraction(it.itemId) ?: 1.0) }.average()
                if (plan != null && stock != null) {
                    plans++
                    wastes += baseEvaluator.waste(stock)
                    costs += plan.totalCostCop.toDouble()
                    utilities += baseUtility(plan, pantry)
                    if (plan.days.map { it.recipe.id } == basePlans[index]?.days?.map { it.recipe.id }) same++
                }
            }
            WeightRow(name, plans, used, eligible, consumed.average(), wastes.average(), costs.average(), utilities.average(), same)
        }
        assertEquals("Con los pesos base, todos los planes son iguales a sí mismos", rows.first().plans, rows.first().sameAsBase)
        return buildString {
            append("\n## Sensibilidad de los pesos (${scenarios.size} inventarios, COP 100.000, 4 porciones, D1)\n\n")
            append("Métricas calculadas con los pesos base. \"Iguales al base\": planes con las mismas 3 recetas en el mismo orden.\n\n")
            append("| Pesos | Planes | Productos usados / elegibles | Cantidad consumida | Desperdicio medio | Costo medio | ")
            append("Utilidad (pesos base) | Iguales al base |\n")
            append("|---|---|---|---|---|---|---|---|\n")
            rows.forEach { r ->
                append("| ${r.name} | ${r.plans}/${scenarios.size} | ${r.used}/${r.eligible} (${pct(r.used.toDouble() / r.eligible)}) | ")
                append("${pct(r.consumed)} | ${num(r.waste, 2)} | COP ${"%,.0f".format(Locale.ROOT, r.cost)} | ")
                append("${num(r.baseUtility, 3)} | ${r.sameAsBase}/${r.plans} |\n")
            }
        }
    }

    /** Productos que vencen en ≤ 3 días y alguna receta del catálogo usa (los elegibles de `AgentEvaluationTest`). */
    private fun usable(pantry: List<PantryItem>): List<PantryItem> =
        pantry.filter { it.isExpiringSoon() }
            .filter { item -> recipes.any { r -> r.ingredients.any { it.ingredientId == item.ingredientId } } }

    /** Inventario al final del plan, recalculado con el evaluador base. */
    private fun finalStock(plan: MealPlan, pantry: List<PantryItem>): PantryStock {
        var stock = PantryStock.of(pantry)
        plan.days.forEach { stock = baseEvaluator.evaluate(it.recipe, context.servings, it.day, stock, context.store).remaining }
        return stock
    }

    /**
     * Utilidad del plan con los pesos base, para comparar planes elegidos con otros pesos. [PlanSearchProblem.replay]
     * la recalcula día por día con el inventario que dejaron los días anteriores.
     */
    private fun baseUtility(plan: MealPlan, pantry: List<PantryItem>): Double =
        PlanSearchProblem(recipes, ingredients, pantry, context, base).replay(plan.days.map { it.recipe.id })!!.utility
    // ---------- Recocido simulado ----------

    private data class AnnealingRun(val gap: Double?, val optimal: Boolean)

    private fun annealingTable(): String {
        val problems = scenarios.map { PlanSearchProblem(recipes, ingredients, it, context) }
        val optimum = scenarios.map { MealPlanner(ingredients).searchOutcome(recipes, it, context) }
        val series = listOf(
            "3.000 iteraciones" to { _: Int -> FIXED_ITERATIONS },
            "Iteraciones = nodos del agente" to { index: Int -> optimum[index].nodes.toInt() },
        )
        val rows = series.map { (name, iterations) ->
            // Brecha media por semilla (sobre los inventarios con plan óptimo) y todas las corridas.
            val perSeed = mutableListOf<Double>()
            val runs = mutableListOf<AnnealingRun>()
            SEEDS.forEach { seed ->
                val gaps = mutableListOf<Double>()
                problems.forEachIndexed { index, problem ->
                    val best = optimum[index].plan?.utility ?: return@forEachIndexed
                    val result = problem.simulatedAnnealing(seed * 1_000 + index, iterations(index))
                    val gap = result.ids?.let { best - result.utility }
                    runs += AnnealingRun(gap, gap != null && gap <= EPSILON)
                    gap?.let { gaps += it }
                }
                perSeed += gaps.average()
            }
            Triple(name, perSeed, runs)
        }
        rows.forEach { (_, _, runs) -> assertTrue("El recocido corrió", runs.isNotEmpty()) }
        val nodes = optimum.map { it.nodes.toDouble() }
        return buildString {
            append("\n## Recocido simulado con ${SEEDS.count()} semillas (${scenarios.size} inventarios)\n\n")
            append("Nodos del agente por inventario: media ${num(nodes.average(), 0)}, máximo ${num(nodes.max(), 0)}. ")
            append("La brecha es la utilidad óptima menos la del recocido (menor es mejor).\n\n")
            append("| Presupuesto | Corridas | Con plan | Brecha media | Desviación entre semillas | Peor brecha | Llega al óptimo |\n")
            append("|---|---|---|---|---|---|---|\n")
            rows.forEach { (name, perSeed, runs) ->
                val withPlan = runs.filter { it.gap != null }
                val worst = withPlan.maxOfOrNull { it.gap!! }
                append("| $name | ${runs.size} | ${withPlan.size} | ${num(perSeed.average(), 4)} | ${num(sd(perSeed), 4)} | ")
                append("${worst?.let { num(it, 4) } ?: "—"} | ${pct(runs.count { it.optimal }.toDouble() / runs.size)} |\n")
            }
        }
    }

    private fun sd(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / (values.size - 1))
    }

    private fun pct(value: Double): String = "%.1f %%".format(Locale.ROOT, value * 100)

    private fun num(value: Double, decimals: Int): String = "%.${decimals}f".format(Locale.ROOT, value)

    private companion object {
        const val FIXED_ITERATIONS = 3_000
        val SEEDS = 1..10
        const val EPSILON = 1e-9
    }
}
