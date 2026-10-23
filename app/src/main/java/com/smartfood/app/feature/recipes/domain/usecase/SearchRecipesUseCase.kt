package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.Recipe
import javax.inject.Inject

/**
 * Nombre: SearchRecipesUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Filtra las recetas por texto en el nombre o en los ingredientes (spec 009, CA-RIA-01-04, T007). Usa la
 * misma normalización del inventario (minúsculas, sin tildes) y los sinónimos de cada ingrediente.
 */
class SearchRecipesUseCase @Inject constructor() {

    /**
     * @param recipes Recetas del catálogo.
     * @param ingredients Ingredientes por `id`.
     * @param query Texto escrito por la persona; vacío o solo espacios devuelve todas las recetas.
     */
    operator fun invoke(
        recipes: List<Recipe>,
        ingredients: Map<String, Ingredient>,
        query: String,
    ): List<Recipe> {
        val text = SearchInventoryUseCase.normalize(query)
        if (text.isEmpty()) return recipes
        return recipes.filter { recipe ->
            SearchInventoryUseCase.normalize(recipe.name).contains(text) ||
                recipe.ingredients.any { used ->
                    val ingredient = ingredients[used.ingredientId] ?: return@any false
                    (listOf(ingredient.name) + ingredient.synonyms)
                        .any { SearchInventoryUseCase.normalize(it).contains(text) }
                }
        }
    }
}
