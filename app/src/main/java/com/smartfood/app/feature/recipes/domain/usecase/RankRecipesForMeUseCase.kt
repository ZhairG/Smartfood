package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.agent.PantryStock
import com.smartfood.app.feature.recipes.domain.agent.RecipeEvaluator
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.domain.model.RecipeSuggestion
import javax.inject.Inject

/**
 * Nombre: RankRecipesForMeUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Ordena las recetas según el inventario actual (spec 009, CA-RIA-01-02 y 01-03, T007). Empareja el
 * inventario con el catálogo y evalúa cada receta como si se cocinara hoy: costo de lo que falta, ingrediente por
 * vencer que rescata y utilidad. "Para ti" ordena por utilidad, "≤ 30 min" filtra por tiempo y "Económicas" ordena
 * de menor a mayor costo.
 *
 * @param matcher Emparejamiento inventario ↔ ingredientes (RF-RIA-02).
 */
class RankRecipesForMeUseCase @Inject constructor(
    private val matcher: IngredientMatcher,
) {

    /**
     * @param recipes Recetas que pasaron la búsqueda.
     * @param ingredients Ingredientes del catálogo por `id`.
     * @param inventory Productos del inventario con sesión.
     * @param filter Filtro elegido en la pantalla.
     */
    operator fun invoke(
        recipes: List<Recipe>,
        ingredients: Map<String, Ingredient>,
        inventory: List<InventoryItem>,
        filter: RecipeFilter,
    ): List<RecipeSuggestion> {
        if (recipes.isEmpty()) return emptyList()
        val evaluator = RecipeEvaluator(ingredients)
        val stock = PantryStock.of(matcher.match(inventory, ingredients))
        val suggestions = recipes.map { recipe ->
            val evaluation = evaluator.evaluate(recipe, recipe.servings, day = 1, stock = stock)
            val rescued = evaluation.uses.firstOrNull { it.rescuedItems.isNotEmpty() }
            RecipeSuggestion(
                recipe = recipe,
                costCop = evaluation.costCop,
                utility = evaluation.utility,
                expiringIngredientName = rescued?.let { ingredients[it.ingredientId]?.name },
            )
        }
        return when (filter) {
            RecipeFilter.FOR_YOU -> suggestions.sortedWith(BY_UTILITY)
            RecipeFilter.UNDER_30 -> suggestions
                .filter { it.recipe.totalMinutes <= MAX_QUICK_MINUTES }
                .sortedWith(BY_UTILITY)

            RecipeFilter.ECONOMIC -> suggestions.sortedWith(BY_COST)
        }
    }

    private companion object {
        /** "≤ 30 min" (CA-RIA-01-03). */
        const val MAX_QUICK_MINUTES = 30

        /** Mayor utilidad primero; a igual utilidad, el `id` mantiene el orden estable. */
        val BY_UTILITY: Comparator<RecipeSuggestion> =
            compareByDescending<RecipeSuggestion> { it.utility }.thenBy { it.recipe.id }

        /** Menor costo de compra primero (CA-RIA-01-03). */
        val BY_COST: Comparator<RecipeSuggestion> =
            compareBy<RecipeSuggestion> { it.costCop }
                .thenByDescending { it.utility }
                .thenBy { it.recipe.id }
    }
}
