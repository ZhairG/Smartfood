package com.smartfood.app.feature.recipes.domain.agent

import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.MealPlan
import com.smartfood.app.feature.recipes.domain.model.PantryItem
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
import com.smartfood.app.feature.recipes.domain.model.PlannedDay
import com.smartfood.app.feature.recipes.domain.model.PlannerWeights
import com.smartfood.app.feature.recipes.domain.model.Recipe

/**
 * Nombre: MealPlanner.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Agente de IA de SmartFood (spec 009 §2, plan §3.3, T011). Resuelve un problema de satisfacción de
 * restricciones con optimización:
 * - **Variables:** `D1…Dn` (n = `PlanContext.days`), la receta de cada día.
 * - **Dominio:** las recetas que cumplen las restricciones de una sola receta (consistencia de nodo): tiempo máximo,
 *   picante, restricciones alimentarias (ningún ingrediente con una etiqueta que la dieta prohíbe) y como máximo
 *   `maxMissing` faltantes con el inventario completo.
 * - **Restricciones entre variables:** recetas distintas, inventario compartido (cada receta usa lo que dejaron las
 *   anteriores), presupuesto total y faltantes de cada receta con el inventario que le queda.
 * - **Objetivo:** maximizar la utilidad del plan ([RecipeEvaluator.planUtility]).
 * - **Búsqueda:** backtracking que asigna los días en orden, con **forward checking** (solo se prueban las recetas que
 *   todavía caben en el presupuesto y el inventario restantes) y **ramificación y poda** (se descarta una rama cuya
 *   utilidad máxima posible no alcanza la del mejor plan encontrado).
 *
 * Es determinista (RNF-RIA-01): ante utilidades iguales gana el plan con los ids de receta menores en orden.
 *
 * @param ingredients Ingredientes del catálogo por `id`.
 * @param weights Pesos de la función de utilidad.
 */
class MealPlanner(
    private val ingredients: Map<String, Ingredient>,
    private val weights: PlannerWeights = PlannerWeights(),
) {
    private val evaluator = RecipeEvaluator(ingredients, weights)

    /** Busca el plan de mayor utilidad; si no hay ninguno válido, devuelve sugerencias (CA-RIA-02-03, 02-07). */
    fun plan(recipes: List<Recipe>, pantry: List<PantryItem>, context: PlanContext): PlanResult =
        search(recipes, pantry, context)?.let { PlanResult.Found(it) }
            ?: PlanResult.NoPlan(suggestions(recipes, pantry, context))

    /**
     * `true` si ningún ingrediente de [recipe] tiene una etiqueta que la dieta de [context] prohíbe (CA-RIA-02-09).
     * Un ingrediente que no está en el catálogo no tiene etiquetas.
     */
    fun fitsDiet(recipe: Recipe, context: PlanContext): Boolean {
        val forbidden = context.diet.flatMapTo(mutableSetOf()) { it.forbidden }
        if (forbidden.isEmpty()) return true
        return recipe.ingredients.none { ri -> ingredients[ri.ingredientId]?.tags.orEmpty().any { it in forbidden } }
    }

    /** El plan de mayor utilidad que cumple todas las restricciones, o `null` si no existe. */
    fun search(recipes: List<Recipe>, pantry: List<PantryItem>, context: PlanContext): MealPlan? {
        val full = PantryStock.of(pantry)
        // Consistencia de nodo: las restricciones de una sola receta. Los faltantes con el inventario completo el día 1
        // son el mejor caso posible, así que una receta que ya los excede no cabe en ningún día.
        val domain = recipes
            .filter { context.maxMinutes == null || it.totalMinutes <= context.maxMinutes }
            .filter { !(context.noSpicy && it.spicy) }
            .filter { fitsDiet(it, context) }
            .filter {
                val e = evaluator.evaluate(it, context.servings, day = 1, stock = full)
                e.missingCount <= context.maxMissing && e.costCop <= context.budgetCop
            }
            .sortedBy { it.id }
        if (domain.size < context.days) return null

        val best = Best()
        assign(day = 1, chosen = emptyList(), stock = full, budgetLeft = context.budgetCop, partial = 0.0, domain, context, best)
        return best.plan
    }

    /** Una receta asignada a un día, con su evaluación en ese momento de la búsqueda. */
    private data class Assigned(val recipe: Recipe, val evaluation: RecipeEvaluator.Evaluation)

    /** Mejor plan encontrado hasta ahora. */
    private class Best {
        var utility = Double.NEGATIVE_INFINITY
        var ids: List<String> = emptyList()
        var plan: MealPlan? = null
    }

    private fun assign(
        day: Int,
        chosen: List<Assigned>,
        stock: PantryStock,
        budgetLeft: Long,
        partial: Double,
        domain: List<Recipe>,
        context: PlanContext,
        best: Best,
    ) {
        if (day > context.days) {
            val utility = partial - weights.waste * evaluator.waste(stock)
            val ids = chosen.map { it.recipe.id }
            if (best.plan == null || utility > best.utility + EPSILON ||
                (utility >= best.utility - EPSILON && compareIds(ids, best.ids) < 0)
            ) {
                best.utility = utility
                best.ids = ids
                best.plan = toPlan(chosen, stock, context, utility)
            }
            return
        }
        // Forward checking: con el inventario y el presupuesto que quedan, solo sirven las recetas que todavía cumplen.
        // Como el inventario solo disminuye, una receta que hoy no cabe no cabrá en ningún día siguiente.
        val usedIds = chosen.mapTo(HashSet()) { it.recipe.id }
        val candidates = domain.asSequence()
            .filter { it.id !in usedIds }
            .map { Assigned(it, evaluator.evaluate(it, context.servings, day, stock)) }
            .filter { it.evaluation.missingCount <= context.maxMissing && it.evaluation.costCop <= budgetLeft }
            .sortedWith(compareByDescending<Assigned> { it.evaluation.utility }.thenBy { it.recipe.id })
            .toList()
        val daysLeft = context.days - day + 1
        if (candidates.size < daysLeft) return

        val utilities = candidates.map { it.evaluation.utility }
        for ((index, candidate) in candidates.withIndex()) {
            // Cota optimista: lo que lleva el plan, más esta receta, más las mejores utilidades del resto para los días
            // que faltan (con el inventario actual, que es el mejor caso), sin descontar desperdicio.
            val bound = partial + candidate.evaluation.utility + topSum(utilities, daysLeft - 1, skip = index)
            if (best.plan != null && bound < best.utility - EPSILON) continue
            assign(
                day + 1,
                chosen + candidate,
                candidate.evaluation.remaining,
                budgetLeft - candidate.evaluation.costCop,
                partial + candidate.evaluation.utility,
                domain,
                context,
                best,
            )
        }
    }

    private fun toPlan(chosen: List<Assigned>, finalStock: PantryStock, context: PlanContext, utility: Double): MealPlan {
        val days = chosen.mapIndexed { i, a ->
            PlannedDay(
                day = i + 1,
                recipe = a.recipe,
                uses = a.evaluation.uses,
                costCop = a.evaluation.costCop,
                pantryShare = a.evaluation.pantryShare,
                utility = a.evaluation.utility,
            )
        }
        val total = days.sumOf { it.costCop }
        return MealPlan(
            days = days,
            totalCostCop = total,
            remainingBudgetCop = context.budgetCop - total,
            wastedItems = evaluator.wastedItems(finalStock),
            utility = utility,
        )
    }

    /**
     * Qué cambiar para que exista un plan (CA-RIA-02-07): el presupuesto mínimo (el plan más barato, buscado con la
     * misma búsqueda y pesos que solo miran el costo), quitar "Sin picante" o subir el tiempo máximo.
     */
    private fun suggestions(recipes: List<Recipe>, pantry: List<PantryItem>, context: PlanContext): List<PlanSuggestion> {
        val cheapest = MealPlanner(ingredients, CHEAPEST)
        val unlimited = context.copy(budgetCop = UNLIMITED_BUDGET)
        val out = mutableListOf<PlanSuggestion>()
        cheapest.search(recipes, pantry, unlimited)?.let { out += PlanSuggestion.RaiseBudget(roundUpToThousand(it.totalCostCop)) }
        if (context.noSpicy && search(recipes, pantry, context.copy(noSpicy = false)) != null) out += PlanSuggestion.AllowSpicy
        if (context.maxMinutes != null) {
            search(recipes, pantry, context.copy(maxMinutes = null))?.let { plan ->
                out += PlanSuggestion.AllowMoreTime(plan.days.maxOf { it.recipe.totalMinutes })
            }
        }
        return out.ifEmpty { listOf(PlanSuggestion.NotEnoughRecipes) }
    }

    private companion object {
        const val EPSILON = 1e-9

        /** Presupuesto sin límite para calcular las sugerencias. */
        const val UNLIMITED_BUDGET = Long.MAX_VALUE / 4

        /** Pesos que solo miran el costo: el plan "de mayor utilidad" es el más barato. */
        val CHEAPEST = PlannerWeights(rescue = 0.0, cost = 1.0, waste = 0.0)

        fun roundUpToThousand(cop: Long): Long = (cop + 999) / 1000 * 1000

        /** Suma de los [count] mayores valores de [sortedDesc] sin contar la posición [skip]. */
        fun topSum(sortedDesc: List<Double>, count: Int, skip: Int): Double {
            var sum = 0.0
            var taken = 0
            for (i in sortedDesc.indices) {
                if (taken == count) break
                if (i == skip) continue
                sum += sortedDesc[i]
                taken++
            }
            return sum
        }

        /** Orden lexicográfico de dos listas de ids del mismo tamaño. */
        fun compareIds(a: List<String>, b: List<String>): Int {
            for (i in a.indices) {
                val c = a[i].compareTo(b.getOrElse(i) { "" })
                if (c != 0) return c
            }
            return a.size - b.size
        }
    }
}
