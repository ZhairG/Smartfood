package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.DeletedOwnRecipe
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import com.smartfood.app.feature.recipes.domain.repository.OwnRecipesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Nombre: DeleteOwnRecipeUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Elimina una receta propia y su ♡, y devuelve una copia para deshacer (spec 010, CA-SAV-05-03, T017). La
 * receta se borra de verdad al confirmar, así que funciona sin conexión y no depende de que la app siga abierta (spec §7).
 *
 * @param recipes Recetas propias.
 * @param favorites Favoritas.
 */
class DeleteOwnRecipeUseCase @Inject constructor(
    private val recipes: OwnRecipesRepository,
    private val favorites: FavoritesRepository,
) {
    /** @param id Receta que se elimina. */
    suspend operator fun invoke(id: String): Result<DeletedOwnRecipe> {
        val recipe = recipes.getRecipe(id).getOrElse { return Result.failure(it) }
        val favorite = favorites.observeFavorites().first().firstOrNull { it.recipeId == id }
        recipes.deleteRecipe(id).getOrElse { return Result.failure(it) }
        if (favorite != null) {
            favorites.setFavorite(id, FavoriteSource.OWN, favorite = false).getOrElse { return Result.failure(it) }
        }
        return Result.success(DeletedOwnRecipe(recipe, favorite != null, favorite?.savedAtMillis ?: 0L))
    }
}

/**
 * Vuelve a escribir una receta eliminada con el mismo id, su fecha de creación y su ♡ (spec 010, CA-SAV-05-04, T017).
 *
 * @param recipes Recetas propias.
 * @param favorites Favoritas.
 */
class RestoreOwnRecipeUseCase @Inject constructor(
    private val recipes: OwnRecipesRepository,
    private val favorites: FavoritesRepository,
) {
    /** @param deleted Copia que devolvió [DeleteOwnRecipeUseCase]. */
    suspend operator fun invoke(deleted: DeletedOwnRecipe): Result<WriteOutcome> {
        val outcome = recipes.saveRecipe(deleted.recipe, isNew = false).getOrElse { return Result.failure(it) }
        if (deleted.wasFavorite) {
            favorites.setFavorite(deleted.recipe.id, FavoriteSource.OWN, favorite = true).getOrElse { return Result.failure(it) }
        }
        return Result.success(outcome)
    }
}

/**
 * Entrega a Guardadas la receta que se acaba de eliminar. La pantalla de confirmación ya no existe cuando empieza el aviso
 * (CA-SAV-05-03), y [DeletedOwnRecipe] no cabe en el `savedStateHandle` sin serializar la receta completa.
 */
@Singleton
class DeletedRecipeStore @Inject constructor() {
    private val _pending = MutableStateFlow<DeletedOwnRecipe?>(null)

    /** Receta eliminada que todavía nadie tomó. */
    val pending: StateFlow<DeletedOwnRecipe?> = _pending.asStateFlow()

    /** Deja [deleted] para Guardadas. */
    fun publish(deleted: DeletedOwnRecipe) {
        _pending.value = deleted
    }

    /** Marca el aviso como tomado, para que no se repita. */
    fun clear() {
        _pending.value = null
    }
}
