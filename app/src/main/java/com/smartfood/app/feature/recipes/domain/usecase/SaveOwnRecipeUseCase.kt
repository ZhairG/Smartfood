package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import com.smartfood.app.feature.recipes.domain.repository.OwnRecipesRepository
import javax.inject.Inject

/** Resultado de guardar una receta propia (spec 010, T012). */
data class SavedOwnRecipe(val recipe: OwnRecipe, val outcome: WriteOutcome)

/** Valida y guarda una receta; una receta nueva también queda marcada como favorita. */
class SaveOwnRecipeUseCase @Inject constructor(
    private val recipes: OwnRecipesRepository,
    private val favorites: FavoritesRepository,
    private val validate: ValidateOwnRecipeUseCase,
) {
    suspend operator fun invoke(id: String?, draft: OwnRecipeDraft): Result<SavedOwnRecipe> {
        validate(draft).getOrElse { return Result.failure(it) }
        val existing = id?.let { recipes.getRecipe(it).getOrNull() }
        val now = System.currentTimeMillis()
        val recipe = OwnRecipe(id ?: recipes.newRecipeId(), draft.name.trim(), draft.category!!, draft.ingredients,
            draft.cookMinutes.toInt(), draft.preparation, existing?.createdAtMillis ?: now, now)
        val outcome = recipes.saveRecipe(recipe, id == null).getOrElse { return Result.failure(it) }
        if (id == null) favorites.setFavorite(recipe.id, FavoriteSource.OWN, true).getOrElse { return Result.failure(it) }
        return Result.success(SavedOwnRecipe(recipe.copy(isPendingSync = outcome == WriteOutcome.PENDING), outcome))
    }
}
