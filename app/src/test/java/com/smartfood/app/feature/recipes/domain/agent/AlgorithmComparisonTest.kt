package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.agent.PlanSearchProblem.ValueOrder
import com.smartfood.app.feature.recipes.domain.model.DietRule
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Nombre: AlgorithmComparisonTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Compara [MealPlanner] con A\*, recocido simulado, voraz y las variantes sin heurísticas (spec 009,
 * RNF-RIA-03), y comprueba que la cota es admisible. La tabla completa tarda minutos, así que solo corre con la variable
 * de entorno `SMARTFOOD_COMPARISON=1` y deja el resultado en `app/build/comparacion-agente.md`.
 */
class AlgorithmComparisonTest {

    private fun smallProblem(seed: Int): PlanSearchProblem {
        val scenario = randomScenario(seed, recipeCount = 6 + seed % 5)
        val context = PlanContext(
            budgetCop = listOf(25_000L, 60_000L, 150_000L)[seed % 3],
            servings = listOf(2, 4, 6)[seed % 3],
            diet = if (seed % 6 == 1) setOf(DietRule.VEGETARIAN) else emptySet(),
        )
        return PlanSearchProblem(scenario.recipes, scenario.ingredients, scenario.pantry, context)
    }

    @Test
    fun `CA-RIA-02-04 la cota es admisible, nunca promete menos que la mejor utilidad real`() {
        var states = 0
        var exact = 0
        repeat(SMALL_SCENARIOS) { seed ->
            smallProblem(seed).boundsVersusReal().forEach { (bound, real) ->
                states++
                assertTrue("semilla $seed: la cota $bound subestima $real", bound >= real - EPSILON)
                if (bound - real <= EPSILON) exact++
            }
        }
        assertTrue("Debe revisar estados suficientes: $states", states >= MIN_STATES)
        println("Cota admisible: $states estados revisados; $exact con cota exacta")
    }

    @Test
    fun `CA-RIA-02-04 A estrella y las variantes exactas hallan la misma utilidad que MealPlanner`() {
        var withPlan = 0
        repeat(SMALL_SCENARIOS) { seed ->
            val problem = smallProblem(seed)
            val scenario = randomScenario(seed, recipeCount = 6 + seed % 5)
            val expected = MealPlanner(scenario.ingredients).search(scenario.recipes, scenario.pantry, problem.context)
            val results = listOf(
                problem.aStar(),
                problem.backtracking(forwardChecking = false, order = ValueOrder.ID, pruning = false),
                problem.backtracking(forwardChecking = true, order = ValueOrder.UTILITY, pruning = true),
                problem.backtracking(forwardChecking = true, order = ValueOrder.LEAST_CONSTRAINING, pruning = true),
            )
            results.forEach { result ->
                if (expected == null) {
                    assertNull("semilla $seed: no debería haber plan", result.ids)
                } else {
                    assertEquals("semilla $seed", expected.utility, result.utility, EPSILON)
                }
            }
            if (expected != null) withPlan++
        }
        assertTrue("Escenarios con plan: $withPlan", withPlan >= SMALL_SCENARIOS / 2)
    }

    @Test
    fun `CA-RIA-02-09 ningun algoritmo incumple una restriccion dura`() {
        RealCatalog.evaluationProfiles(count = 1).forEach { profile ->
            profile.inventories.forEach { pantry ->
                val problem = PlanSearchProblem(RealCatalog.recipes, RealCatalog.ingredients, pantry, profile.context)
                listOf(problem.greedy(), problem.simulatedAnnealing(seed = 1), problem.aStar()).forEach { result ->
                    result.ids?.let { ids -> assertNotNull("${profile.name}: $ids", problem.replay(ids)) }
                }
            }
        }
    }

    @Test
    fun `RNF-RIA-03 tabla comparativa por perfil de usuario`() {
        assumeTrue(System.getenv("SMARTFOOD_COMPARISON") == "1")
        val out = StringBuilder()
        RealCatalog.evaluationProfiles().forEach { profile -> out.append(table(profile)) }
        File("build/comparacion-agente.md").writeText(out.toString())
        println(out)
    }

    /**
     * Cifras de la Cocina Favorita relajada para `docs/ia/evaluacion-agente.md` §8 (spec 011, T012, CA-COC-02-06): en los
     * perfiles con cocina, cuántos inventarios tenían plan con la restricción dura (mínimo 3 = todas, como en §7.2) y
     * cuántos lo tienen ahora, con cuántas recetas de la cocina, el costo medio y el tiempo en la JVM. Deja el resultado en
     * `app/build/cocina-relajada.md`.
     */
    @Test
    fun `CA-COC-02-06 tabla de la cocina relajada`() {
        assumeTrue(System.getenv("SMARTFOOD_COMPARISON") == "1")
        val planner = MealPlanner(RealCatalog.ingredients)
        val out = StringBuilder()
        out.append("| Cocina elegida | Con plan antes (dura) | Con plan ahora | 3 / 2 / 1 recetas de la cocina | Costo medio | ms medio | ms máximo |\n")
        out.append("|---|---|---|---|---|---|---|\n")
        RealCatalog.evaluationProfiles().filter { it.context.cuisines.isNotEmpty() }.forEach { profile ->
            val total = profile.inventories.size
            var before = 0
            val levels = IntArray(4)
            val costs = mutableListOf<Double>()
            val millis = mutableListOf<Double>()
            profile.inventories.forEach { pantry ->
                val strict = planner.plan(RealCatalog.recipes, pantry, profile.context.copy(minCuisineRecipes = profile.context.days))
                if (strict is PlanResult.Found) before++
                val started = System.nanoTime()
                val result = planner.plan(RealCatalog.recipes, pantry, profile.context)
                millis += (System.nanoTime() - started) / 1_000_000.0
                if (result is PlanResult.Found) {
                    levels[result.cuisineRecipes ?: profile.context.days]++
                    costs += result.plan.totalCostCop.toDouble()
                }
            }
            val found = levels.sum()
            out.append("| ${profile.name} | $before de $total | $found de $total | ${levels[3]} / ${levels[2]} / ${levels[1]} | ")
            out.append("${if (costs.isEmpty()) "—" else "COP " + "%,.0f".format(Locale.ROOT, costs.average()).replace(',', '.')} | ")
            out.append("${n(millis, 1)} | ${"%.1f".format(Locale.ROOT, millis.max())} |\n")
        }
        File("build/cocina-relajada.md").writeText(out.toString())
        println(out)
    }

    private data class Row(val name: String, val results: MutableList<Measured> = mutableListOf())

    private data class Measured(val result: PlanSearchProblem.Outcome, val millis: Double, val optimum: Double?)

    private fun table(profile: RealCatalog.EvaluationProfile): String {
        val methods: List<Pair<String, (PlanSearchProblem, Int) -> PlanSearchProblem.Outcome>> = listOf(
            "Agente CSP (orden + FC + poda)" to { p, _ -> p.backtracking(true, ValueOrder.UTILITY, true) },
            "Backtracking puro" to { p, _ -> p.backtracking(false, ValueOrder.ID, false) },
            "Backtracking + FC" to { p, _ -> p.backtracking(true, ValueOrder.ID, false) },
            "Backtracking + FC + orden" to { p, _ -> p.backtracking(true, ValueOrder.UTILITY, false) },
            "Backtracking + FC + poda + LCV" to { p, _ -> p.backtracking(true, ValueOrder.LEAST_CONSTRAINING, true) },
            "A*" to { p, _ -> p.aStar() },
            "Recocido simulado" to { p, seed -> p.simulatedAnnealing(seed) },
            "Voraz" to { p, _ -> p.greedy() },
        )
        val rows = methods.map { Row(it.first) }
        val shares = rows.associate { it.name to mutableListOf<Double>() }
        val costs = rows.associate { it.name to mutableListOf<Double>() }
        val wastes = rows.associate { it.name to mutableListOf<Double>() }
        profile.inventories.forEachIndexed { index, pantry ->
            val problem = PlanSearchProblem(RealCatalog.recipes, RealCatalog.ingredients, pantry, profile.context)
            val optimum = (MealPlanner(RealCatalog.ingredients).plan(RealCatalog.recipes, pantry, profile.context) as? PlanResult.Found)
                ?.plan?.utility
            methods.forEachIndexed { m, (name, run) ->
                val started = System.nanoTime()
                val result = run(problem, index)
                val millis = (System.nanoTime() - started) / 1_000_000.0
                rows[m].results += Measured(result, millis, optimum)
                result.ids?.let(problem::replay)?.let { replay ->
                    costs.getValue(name) += replay.costCop.toDouble()
                    wastes.getValue(name) += replay.waste
                    expiringShare(pantry, replay.usedItems)?.let { shares.getValue(name) += it }
                }
            }
        }
        val total = profile.inventories.size
        return buildString {
            append("\n### ${profile.name} ($total inventarios)\n\n")
            append("| Algoritmo | Planes | Utilidad media | Brecha con el óptimo | Aprovechamiento | Desperdicio | Costo | Nodos | Frontera | ms |\n")
            append("|---|---|---|---|---|---|---|---|---|---|\n")
            rows.forEach { row ->
                val found = row.results.filter { it.result.ids != null }
                val gaps = found.mapNotNull { m -> m.optimum?.let { it - m.result.utility } }
                append("| ${row.name} | ${found.size}/$total | ${n(found.map { it.result.utility }, 3)} | ${n(gaps, 3)} | ")
                append("${percent(shares.getValue(row.name))} | ${n(wastes.getValue(row.name), 2)} | ")
                append("${n(costs.getValue(row.name), 0)} | ${n(row.results.map { it.result.nodes.toDouble() }, 0)} | ")
                append("${n(row.results.map { it.result.peakFrontier.toDouble() }, 0)} | ")
                append("${n(row.results.map { it.millis }, 1)} |\n")
            }
        }
    }

    /** Fracción de lo que vence en ≤ 3 días que el plan usa; `null` si no hay nada por vencer. */
    private fun expiringShare(pantry: List<PantryItem>, used: Set<String>): Double? {
        val expiring = pantry.filter { it.isExpiringSoon() }
        return if (expiring.isEmpty()) null else expiring.count { it.itemId in used }.toDouble() / expiring.size
    }

    private fun n(values: List<Double>, decimals: Int): String =
        if (values.isEmpty()) "—" else "%.${decimals}f".format(Locale.ROOT, values.average())

    private fun percent(values: List<Double>): String =
        if (values.isEmpty()) "—" else "%.1f %%".format(Locale.ROOT, values.average() * 100)

    private companion object {
        const val EPSILON = 1e-9
        const val SMALL_SCENARIOS = 30
        const val MIN_STATES = 500
    }
}
