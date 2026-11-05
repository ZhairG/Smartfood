package com.smartfood.app.feature.recipes.ui.saved

import com.smartfood.app.feature.recipes.domain.model.Favorite
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.usecase.ObserveFavoriteIdsUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ObserveSavedRecipesUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ToggleFavoriteUseCase
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SavedRecipesViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre de 2026
 * Descripción: Pruebas de [SavedRecipesViewModel] (spec 010, T008): evento de toggle de ♡
 * en la pestaña Guardadas (CA-SAV-01-03).
 */
class SavedRecipesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recipe1 = Recipe(
        id = "arepas",
        name = "Arepas de chócolo",
        photo = "receta_arepas",
        servings = 2,
        prepMinutes = 10,
        cookMinutes = 20,
        difficulty = "Fácil",
        kcal = 400,
        ingredients = listOf(),
        steps = emptyList(),
    )

    @Test
    fun `CA-SAV-01-03 tocar ♡ llama a setFavorite`() = runTest {
        val favRepo = FakeFavoritesRepository().apply {
            this.favorites.value = listOf(
                Favorite("arepas", FavoriteSource.CATALOG, 1_000L, isPendingSync = false),
            )
        }
        val viewModel = SavedRecipesViewModel(
            toggleFavorite = ToggleFavoriteUseCase(favRepo),
            observeFavoriteIds = ObserveFavoriteIdsUseCase(favRepo),
            observeSavedRecipes = ObserveSavedRecipesUseCase(favRepo, FakeCatalog(listOf(recipe1))),
        )

        viewModel.onEvent(SavedRecipesEvent.FavoriteToggled("arepas"))

        assertEquals(1, favRepo.setCalls.size)
        assertEquals("arepas", favRepo.setCalls.first().recipeId)
    }

    private class FakeCatalog(private val recipes: List<Recipe>) : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = recipes
        override suspend fun ingredients(): Map<String, com.smartfood.app.feature.recipes.domain.model.Ingredient> =
            emptyMap()
    }
}