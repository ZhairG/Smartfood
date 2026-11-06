package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import javax.inject.Inject

/**
 * Nombre: ToggleFavoriteUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Marca o quita una receta de las favoritas (spec 010, CA-SAV-01-03 y 01-04, T004). El repositorio
 * conserva el cambio local aunque el servidor tarde más de tres segundos en confirmarlo.
 *
 * @param favorites Favoritas de la sesión activa.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val favorites: FavoritesRepository,
) {

    /** Marca o quita [recipeId], conservando su [source] para Firestore. */
    suspend operator fun invoke(
        recipeId: String,
        source: FavoriteSource,
        favorite: Boolean,
    ): Result<WriteOutcome> = favorites.setFavorite(recipeId, source, favorite)
}
