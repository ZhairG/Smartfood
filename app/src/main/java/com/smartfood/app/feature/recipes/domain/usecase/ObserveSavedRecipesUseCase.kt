package com.smartfood.app.feature.recipes.domain.usecase

import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import com.smartfood.app.feature.recipes.domain.model.Favorite
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedRecipeCard
import com.smartfood.app.feature.recipes.domain.model.SavedRecipesData
import com.smartfood.app.feature.recipes.domain.model.SavedSort
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.repository.FavoritesRepository
import com.smartfood.app.feature.recipes.domain.repository.OwnRecipesRepository
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Nombre: ObserveSavedRecipesUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Combina las favoritas con el catálogo y aplica chip, búsqueda y orden de Guardadas (spec 010,
 * CA-SAV-02-03 a 02-05, T004). T004 solo conoce las favoritas del catálogo; T010 incorporará las recetas propias.
 * Las favoritas cuyo id ya no existe en el catálogo se descartan (plan §3).
 *
 * @param favorites Fuente reactiva de favoritas.
 * @param catalog Catálogo empaquetado de recetas.
 */
class ObserveSavedRecipesUseCase @Inject constructor(
    private val favorites: FavoritesRepository,
    private val catalog: RecipeCatalog,
    private val ownRecipes: OwnRecipesRepository,
) {

    /**
     * @param filter Chip elegido.
     * @param query Texto de búsqueda; se normaliza como el inventario.
     * @param sort Orden elegido mientras la app permanece abierta.
     */
    operator fun invoke(
        filter: Flow<SavedFilter>,
        query: Flow<String>,
        sort: Flow<SavedSort>,
    ): Flow<SavedRecipesData> = combine(
        combine(
            flow { emit(catalog.recipes()) },
            favorites.observeFavorites(),
            ownRecipes.observeRecipes(),
        ) { recipes, values, own -> Triple(recipes, values, own) },
        combine(filter, query, sort) { selectedFilter, text, selectedSort ->
            Triple(selectedFilter, text, selectedSort)
        },
    ) { source, options ->
        buildData(source.first, source.second, source.third, options.first, options.second, options.third)
    }

    private fun buildData(
        recipes: List<Recipe>,
        values: List<Favorite>,
        own: List<OwnRecipe>,
        filter: SavedFilter,
        query: String,
        sort: SavedSort,
    ): SavedRecipesData {
        val recipesById = recipes.associateBy { it.id }
        val catalogFavorites = values.filter { it.source == FavoriteSource.CATALOG }
        val validFavorites = catalogFavorites.filter { it.recipeId in recipesById }
        val catalogCards = validFavorites.map { favorite ->
            recipesById.getValue(favorite.recipeId).toSavedCard(favorite)
        }
        val ownCards = own.map { recipe ->
            SavedRecipeCard(recipe.id, recipe.name, null, OwnRecipe.SERVINGS, recipe.cookMinutes,
                values.any { it.recipeId == recipe.id && it.source == FavoriteSource.OWN },
                true, maxOf(recipe.updatedAtMillis, values.firstOrNull { it.recipeId == recipe.id }?.savedAtMillis ?: 0L),
                recipe.isPendingSync || values.any { it.recipeId == recipe.id && it.isPendingSync })
        }
        val allCards = catalogCards + ownCards
        val cardsForFilter = when (filter) {
            SavedFilter.FAVORITES -> allCards.filter { it.isFavorite }
            SavedFilter.RECENT -> allCards
            SavedFilter.OWN -> ownCards
        }
        val normalizedQuery = SearchInventoryUseCase.normalize(query)
        val found = cardsForFilter.filter { card ->
            normalizedQuery.isEmpty() || SearchInventoryUseCase.normalize(card.name).contains(normalizedQuery)
        }
        val ordered = when (sort) {
            SavedSort.RECENT -> found.sortedWith(BY_RECENT)
            SavedSort.NAME -> found.sortedWith(BY_NAME)
        }
        return SavedRecipesData(
            cards = ordered,
            isEmptyOverall = allCards.isEmpty(),
            hasPendingSync = allCards.any { it.isPendingSync },
        )
    }

    private fun Recipe.toSavedCard(favorite: Favorite) = SavedRecipeCard(
        id = id,
        name = name,
        photo = photo,
        servings = servings,
        minutes = totalMinutes,
        isFavorite = true,
        isOwn = false,
        lastChangeMillis = favorite.savedAtMillis,
        isPendingSync = favorite.isPendingSync,
    )

    private companion object {
        val BY_RECENT: Comparator<SavedRecipeCard> =
            compareByDescending<SavedRecipeCard> { it.lastChangeMillis }.thenBy { it.id }
        val BY_NAME: Comparator<SavedRecipeCard> =
            compareBy<SavedRecipeCard> { SearchInventoryUseCase.normalize(it.name) }.thenBy { it.id }
    }
}
