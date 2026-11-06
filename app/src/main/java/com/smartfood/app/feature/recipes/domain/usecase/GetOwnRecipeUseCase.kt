package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.repository.OwnRecipesRepository
import javax.inject.Inject

/** Obtiene una receta propia para editarla o mostrarla (spec 010, T012). */
class GetOwnRecipeUseCase @Inject constructor(private val recipes: OwnRecipesRepository) {
    suspend operator fun invoke(id: String): Result<OwnRecipe> = recipes.getRecipe(id)
}
