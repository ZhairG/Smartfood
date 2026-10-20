package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.recipes.domain.model.RecipeDetail
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import javax.inject.Inject

/**
 * Nombre: GetRecipeDetailUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Lee del catálogo la receta que se abre en el detalle (spec 009, CA-RIA-04-01, T008). Si el `id` no
 * existe o el catálogo no se puede leer, devuelve un fallo para que la pantalla muestre el aviso.
 *
 * @param catalog Catálogo empaquetado en la app.
 */
class GetRecipeDetailUseCase @Inject constructor(
    private val catalog: RecipeCatalog,
) {

    /** @param recipeId Receta que se quiere ver. */
    suspend operator fun invoke(recipeId: String): Result<RecipeDetail> = safeRunCatching {
        val recipes = catalog.recipes()
        val recipe = recipes.firstOrNull { it.id == recipeId }
            ?: throw NoSuchElementException("La receta '$recipeId' no está en el catálogo")
        RecipeDetail(recipe = recipe, ingredients = catalog.ingredients())
    }
}
