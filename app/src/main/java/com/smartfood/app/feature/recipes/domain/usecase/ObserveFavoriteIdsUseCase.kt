package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Nombre: ObserveFavoriteIdsUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Expone los ids favoritos del catálogo para pintar el ♡ en Descubrir y en el detalle (spec 010,
 * CA-SAV-01-01 y 01-02, T004). Las favoritas de recetas propias no pueden pintar por error una receta del catálogo.
 *
 * @param favorites Favoritas de la sesión activa.
 */
class ObserveFavoriteIdsUseCase @Inject constructor(
    private val favorites: FavoritesRepository,
) {

    /** Ids de las recetas de catálogo que tienen ♡. */
    operator fun invoke(): Flow<Set<String>> = favorites.observeFavorites().map { values ->
        values.asSequence()
            .filter { it.source == FavoriteSource.CATALOG }
            .map { it.recipeId }
            .toSet()
    }
}
