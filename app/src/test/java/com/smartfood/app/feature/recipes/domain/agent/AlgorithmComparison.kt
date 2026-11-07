package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlannerWeights
import com.smartfood.app.feature.recipes.domain.model.Recipe
import java.util.PriorityQueue
import kotlin.math.exp
import kotlin.random.Random

/**
 * Nombre: AlgorithmComparison.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Algoritmos que compiten con [MealPlanner] en la evaluación del curso (spec 009, RNF-RIA-03). Viven solo en
 * las pruebas: no llegan al APK y todos usan el mismo [RecipeEvaluator], así que comparan la misma función de utilidad y
 * las mismas restricciones. El problema de búsqueda es:
 * - **Estado:** las recetas elegidas hasta ahora, con el inventario y el presupuesto que dejaron.
 * - **Acción:** asignar una receta al siguiente día.
 * - **Meta:** un plan de `context.days` días que cumpla todas las restricciones.
 * - **Objetivo:** la mayor utilidad.
 *
 * @param allRecipes Catálogo completo.
 * @param ingredients Ingredientes por id.
 * @param pantry Inventario.
 * @param context Datos del plan.
 * @param weights Pesos de la utilidad.
 */
internal class PlanSearchProblem(
    allRecipes: List<Recipe>,
    ingredients: Map<String, Ingredient>,
    pantry: List<PantryItem>,
    val context: PlanContext,
    private val weights: PlannerWeights = PlannerWeights(),
) {
    private val planner = MealPlanner(ingredients, weights)
    private val evaluator = RecipeEvaluator(ingredients, weights)
    private val start = PantryStock.of(pantry)

    /** Consistencia de nodo: las mismas restricciones de una sola receta que aplica [MealPlanner.search]. */
    val domain: List<Recipe> = allRecipes
        .filter { context.maxMinutes == null || it.totalMinutes <= context.maxMinutes }
        .filter { !(context.noSpicy && it.spicy) }
        .filter { planner.fitsDiet(it, context) }
        .filter { planner.fitsCuisine(it, context) }
        .filter { evaluate(it, day = 1, start).costCop <= context.budgetCop }
        .sortedBy { it.id }

    private val recipesById = domain.associateBy { it.id }

    /** Orden en que se prueban los valores de cada variable. */
    enum class ValueOrder { ID, UTILITY, LEAST_CONSTRAINING }

    /**
     * Resultado de un algoritmo.
     *
     * @property ids Recetas del plan, o `null` si no encontró ninguno.
     * @property utility Utilidad del plan.
     * @property nodes Estados que expandió, o planes que evaluó en el recocido.
     * @property peakFrontier Estados guardados a la vez en memoria; solo A\* tiene frontera.
     */
    data class Outcome(val ids: List<String>?, val utility: Double, val nodes: Long, val peakFrontier: Long = 0)

    /** Datos de un plan completo, recalculados de la misma forma para todos los algoritmos. */
    data class Replay(val utility: Double, val costCop: Long, val waste: Double, val usedItems: Set<String>)

    private data class Step(val recipe: Recipe, val evaluation: RecipeEvaluator.Evaluation)

    private fun evaluate(recipe: Recipe, day: Int, stock: PantryStock) =
        evaluator.evaluate(recipe, context.servings, day, stock, context.store)

    /** Recetas sin usar que caben en el presupuesto restante. */
    private fun affordable(day: Int, used: Set<String>, stock: PantryStock, budgetLeft: Long): List<Step> =
        domain.asSequence()
            .filter { it.id !in used }
            .map { Step(it, evaluate(it, day, stock)) }
            .filter { it.evaluation.costCop <= budgetLeft }
            .toList()

    private fun List<Step>.allowed() = filter { it.evaluation.missingCount <= context.maxMissing }

    /** Recalcula un plan. Devuelve `null` si incumple una restricción dura. */
    fun replay(ids: List<String>): Replay? {
        if (ids.size != context.days || ids.toSet().size != ids.size) return null
        var stock = start
        var spent = 0L
        var partial = 0.0
        val used = HashSet<String>()
        ids.forEachIndexed { index, id ->
            val recipe = recipesById[id] ?: return null
            val evaluation = evaluate(recipe, index + 1, stock)
            spent += evaluation.costCop
            if (evaluation.missingCount > context.maxMissing || spent > context.budgetCop) return null
            evaluation.uses.flatMapTo(used) { it.fromPantry.keys }
            partial += evaluation.utility
            stock = evaluation.remaining
        }
        return Replay(partial - weights.waste * evaluator.waste(stock), spent, evaluator.waste(stock), used)
    }

    /**
     * Backtracking con cada heurística opcional, para medir cuánto aporta. Con todas activas equivale a [MealPlanner].
     *
     * @param forwardChecking Abandona una rama si ya no quedan recetas para los días que faltan.
     * @param order Orden de los valores.
     * @param pruning Ramificación y poda con la cota optimista.
     */
    fun backtracking(forwardChecking: Boolean, order: ValueOrder, pruning: Boolean): Outcome {
        var nodes = 0L
        var bestIds: List<String>? = null
        var bestUtility = Double.NEGATIVE_INFINITY

        fun go(day: Int, chosen: List<String>, stock: PantryStock, budgetLeft: Long, partial: Double) {
            nodes++
            if (day > context.days) {
                val utility = partial - weights.waste * evaluator.waste(stock)
                if (bestIds == null || utility > bestUtility + EPSILON) {
                    bestUtility = utility
                    bestIds = chosen
                }
                return
            }
            val options = affordable(day, chosen.toSet(), stock, budgetLeft)
            if (forwardChecking && options.size < context.days - day + 1) return
            val candidates = options.allowed().let { allowed ->
                when (order) {
                    ValueOrder.ID -> allowed
                    ValueOrder.UTILITY -> allowed.sortedWith(
                        compareByDescending<Step> { it.evaluation.utility }.thenBy { it.recipe.id },
                    )
                    ValueOrder.LEAST_CONSTRAINING -> allowed.sortedWith(
                        compareByDescending<Step> { step ->
                            if (day == context.days) {
                                0
                            } else {
                                affordable(
                                    day + 1,
                                    chosen.toSet() + step.recipe.id,
                                    step.evaluation.remaining,
                                    budgetLeft - step.evaluation.costCop,
                                ).allowed().size
                            }
                        }.thenBy { it.recipe.id },
                    )
                }
            }
            for (step in candidates) {
                val next = partial + step.evaluation.utility
                if (pruning && bestIds != null && bound(next, step.evaluation.remaining, day) < bestUtility - EPSILON) {
                    continue
                }
                go(day + 1, chosen + step.recipe.id, step.evaluation.remaining, budgetLeft - step.evaluation.costCop, next)
            }
        }
        go(1, emptyList(), start, context.budgetCop, 0.0)
        return Outcome(bestIds, bestUtility, nodes)
    }

    /** La cota de [MealPlanner]: lo acumulado después del día [day] más todo el rescate que todavía se puede lograr. */
    private fun bound(partial: Double, stock: PantryStock, day: Int): Double =
        partial + heuristic(stock, day)

    /** `h`: el máximo optimista de lo que pueden sumar los días después de [day]. */
    private fun heuristic(stock: PantryStock, day: Int): Double =
        if (day < context.days) weights.rescue * evaluator.rescuePotential(stock, day + 1) else 0.0

    /**
     * A\*: expande primero el estado con mayor `f = g + h`, donde `g` es la utilidad acumulada y `h` la misma cota del
     * agente. Como `h` nunca subestima lo que falta por ganar, el primer plan completo que sale de la cola es óptimo.
     */
    fun aStar(): Outcome {
        // Cada nodo guarda solo las recetas elegidas y el inventario se reconstruye al expandirlo: con el inventario
        // en cada nodo, la frontera agotó los 512 MB de la prueba con el catálogo real.
        data class Node(val f: Double, val ids: List<String>)

        val open = PriorityQueue(compareByDescending<Node> { it.f }.thenBy { it.ids.joinToString() })
        open += Node(heuristic(start, 0), emptyList())
        var nodes = 0L
        var peak = 1L
        while (open.isNotEmpty()) {
            val node = open.poll() ?: break
            nodes++
            if (node.ids.size == context.days) return Outcome(node.ids, node.f, nodes, peak)
            var stock = start
            var budgetLeft = context.budgetCop
            var g = 0.0
            node.ids.forEachIndexed { index, id ->
                val evaluation = evaluate(recipesById.getValue(id), index + 1, stock)
                stock = evaluation.remaining
                budgetLeft -= evaluation.costCop
                g += evaluation.utility
            }
            val day = node.ids.size + 1
            val options = affordable(day, node.ids.toSet(), stock, budgetLeft)
            if (options.size < context.days - day + 1) continue
            options.allowed().forEach { step ->
                val next = g + step.evaluation.utility
                val f = if (day == context.days) {
                    next - weights.waste * evaluator.waste(step.evaluation.remaining)
                } else {
                    bound(next, step.evaluation.remaining, day)
                }
                open += Node(f, node.ids + step.recipe.id)
            }
            peak = maxOf(peak, open.size.toLong())
        }
        return Outcome(null, Double.NEGATIVE_INFINITY, nodes, peak)
    }

    /** Voraz: cada día toma la receta de mayor utilidad y nunca retrocede. */
    fun greedy(): Outcome {
        var stock = start
        var budgetLeft = context.budgetCop
        var nodes = 0L
        val chosen = mutableListOf<String>()
        for (day in 1..context.days) {
            nodes++
            val step = affordable(day, chosen.toSet(), stock, budgetLeft).allowed()
                .minWithOrNull(compareByDescending<Step> { it.evaluation.utility }.thenBy { it.recipe.id })
                ?: return Outcome(null, Double.NEGATIVE_INFINITY, nodes)
            chosen += step.recipe.id
            stock = step.evaluation.remaining
            budgetLeft -= step.evaluation.costCop
        }
        return Outcome(chosen, replay(chosen)?.utility ?: Double.NEGATIVE_INFINITY, nodes)
    }

    /**
     * Recocido simulado: búsqueda local que cambia la receta de un día al azar. Acepta un plan peor con probabilidad
     * `exp(Δ/T)`, para poder salir de un óptimo local, y `T` baja en cada iteración. No garantiza el óptimo.
     */
    fun simulatedAnnealing(seed: Int): Outcome {
        val random = Random(seed)
        var nodes = 0L
        val ids = domain.map { it.id }
        if (ids.size < context.days) return Outcome(null, Double.NEGATIVE_INFINITY, nodes)

        var current: List<String>? = null
        var currentUtility = Double.NEGATIVE_INFINITY
        repeat(ANNEALING_START_ATTEMPTS) {
            if (current == null) {
                val attempt = ids.shuffled(random).take(context.days)
                nodes++
                replay(attempt)?.let {
                    current = attempt
                    currentUtility = it.utility
                }
            }
        }
        var plan = current ?: return Outcome(null, Double.NEGATIVE_INFINITY, nodes)
        var bestPlan = plan
        var bestUtility = currentUtility
        var temperature = ANNEALING_START_TEMPERATURE
        repeat(ANNEALING_ITERATIONS) {
            val day = random.nextInt(context.days)
            val replacement = ids[random.nextInt(ids.size)]
            if (replacement !in plan) {
                val neighbor = plan.toMutableList().also { it[day] = replacement }
                nodes++
                replay(neighbor)?.let { result ->
                    val delta = result.utility - currentUtility
                    if (delta >= 0 || random.nextDouble() < exp(delta / temperature)) {
                        plan = neighbor
                        currentUtility = result.utility
                        if (currentUtility > bestUtility) {
                            bestUtility = currentUtility
                            bestPlan = plan
                        }
                    }
                }
            }
            temperature *= ANNEALING_COOLING
        }
        return Outcome(bestPlan, bestUtility, nodes)
    }

    /**
     * Revisa la cota en todos los estados del árbol: compara lo que promete con la mejor utilidad real alcanzable desde
     * ese estado, calculada por fuerza bruta.
     *
     * @return Pares (cota, mejor utilidad real) de los estados parciales que pueden completarse.
     */
    fun boundsVersusReal(): List<Pair<Double, Double>> {
        val pairs = mutableListOf<Pair<Double, Double>>()

        fun bestFrom(day: Int, used: Set<String>, stock: PantryStock, budgetLeft: Long, partial: Double): Double? {
            if (day > context.days) return partial - weights.waste * evaluator.waste(stock)
            var best: Double? = null
            for (step in affordable(day, used, stock, budgetLeft).allowed()) {
                val next = partial + step.evaluation.utility
                val real = bestFrom(
                    day + 1,
                    used + step.recipe.id,
                    step.evaluation.remaining,
                    budgetLeft - step.evaluation.costCop,
                    next,
                ) ?: continue
                if (day < context.days) pairs += bound(next, step.evaluation.remaining, day) to real
                best = maxOf(best ?: real, real)
            }
            return best
        }
        bestFrom(1, emptySet(), start, context.budgetCop, 0.0)
        return pairs
    }

    private companion object {
        const val EPSILON = 1e-9
        const val ANNEALING_START_ATTEMPTS = 2_000
        const val ANNEALING_ITERATIONS = 3_000
        const val ANNEALING_START_TEMPERATURE = 1.0
        const val ANNEALING_COOLING = 0.998
    }
}
