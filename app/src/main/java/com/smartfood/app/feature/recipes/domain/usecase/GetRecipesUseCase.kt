package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import javax.inject.Inject

/**
 * Nombre: GetRecipesUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Lee una sola vez el catálogo de recetas e ingredientes de `assets/` (spec 009, RF-RIA-01, T007). Si el
 * catálogo no se puede leer, devuelve un fallo para que la pantalla muestre el aviso en lugar de cerrarse.
 *
 * @param catalog Catálogo empaquetado en la app.
 */
class GetRecipesUseCase @Inject constructor(
    private val catalog: RecipeCatalog,
) {

    /** Recetas e ingredientes del catálogo, o el error de lectura. */
    suspend operator fun invoke(): Result<RecipeCatalogData> = safeRunCatching {
        RecipeCatalogData(recipes = catalog.recipes(), ingredients = catalog.ingredients())
    }
}

/**
 * Catálogo completo ya leído.
 *
 * @property recipes Recetas del catálogo.
 * @property ingredients Ingredientes por `id`.
 */
data class RecipeCatalogData(
    val recipes: List<Recipe>,
    val ingredients: Map<String, Ingredient>,
)
