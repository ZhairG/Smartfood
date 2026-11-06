package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import com.smartfood.app.feature.recipes.domain.repository.OwnRecipesRepository
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * Nombre: CountSavedRecipesUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Cuenta las recetas guardadas para el Perfil (spec 010, CA-SAV-06-01 y 06-02, T020): la unión de las
 * favoritas y las creadas, sin contar dos veces una creada que es favorita ni las favoritas huérfanas (spec §7).
 *
 * @param favorites Favoritas.
 * @param ownRecipes Recetas propias.
 * @param catalog Catálogo, para descartar las favoritas huérfanas.
 */
class CountSavedRecipesUseCase @Inject constructor(
    private val favorites: FavoritesRepository,
    private val ownRecipes: OwnRecipesRepository,
    private val catalog: RecipeCatalog,
) {
    operator fun invoke(): Flow<Int> = combine(
        flow { emit(catalog.recipes().mapTo(HashSet()) { it.id }) },
        favorites.observeFavorites(),
        ownRecipes.observeRecipes(),
    ) { catalogIds, favoriteList, own ->
        val ownIds = own.mapTo(HashSet()) { it.id }
        val validFavorites = favoriteList.filter { favorite ->
            if (favorite.source == FavoriteSource.CATALOG) favorite.recipeId in catalogIds else favorite.recipeId in ownIds
        }
        (ownIds + validFavorites.map { it.recipeId }).size
    }
}
