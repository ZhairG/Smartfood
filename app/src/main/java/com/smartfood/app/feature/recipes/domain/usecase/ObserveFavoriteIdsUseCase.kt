package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Nombre: ObserveFavoriteIdsUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Expone los ids de todas las favoritas (catálogo y propias) para pintar el ♡ (spec 010,
 * CA-SAV-01-01 y 01-02, T004/T014).
 *
 * @param favorites Favoritas de la sesión activa.
 */
class ObserveFavoriteIdsUseCase @Inject constructor(
    private val favorites: FavoritesRepository,
) {

    /** Ids de las recetas (catálogo y propias) que tienen ♡. */
    operator fun invoke(): Flow<Set<String>> = favorites.observeFavorites().map { values ->
        values.map { it.recipeId }.toSet()
    }
}
