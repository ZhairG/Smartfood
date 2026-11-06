package com.smartfood.app.feature.recipes.ui.saved

import com.smartfood.app.feature.recipes.domain.model.Favorite
import com.smartfood.app.feature.recipes.domain.model.FavoriteSource
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.SavedFilter
import com.smartfood.app.feature.recipes.domain.model.SavedSort
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.usecase.DeletedRecipeStore
import com.smartfood.app.feature.recipes.domain.usecase.ObserveSavedRecipesUseCase
import com.smartfood.app.feature.recipes.domain.usecase.RestoreOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ToggleFavoriteUseCase
import com.smartfood.app.feature.recipes.domain.model.DeletedOwnRecipe
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.ownRecipe
import com.smartfood.app.testutil.FakeOwnRecipesRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SavedRecipesViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre de 2026
 * Descripción: Pruebas de [SavedRecipesViewModel] (spec 010, T008): ♡, orden que se conserva, efectos de navegación y
 * error de lectura (CA-SAV-01-03, 02-05, 02-06, 02-10 y 02-11). La búsqueda y el orden en sí se prueban en
 * `ObserveSavedRecipesUseCaseTest`.
 */
class SavedRecipesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val favoritas = FakeFavoritesRepository().apply {
        favorites.value = listOf(Favorite("arepas", FavoriteSource.CATALOG, 1_000L, isPendingSync = false))
    }

    private val recipe = Recipe(
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

    private val recetasPropias = FakeOwnRecipesRepository()
    private val eliminadas = DeletedRecipeStore()

    private fun viewModel() = SavedRecipesViewModel(
        toggleFavorite = ToggleFavoriteUseCase(favoritas),
        observeSavedRecipes = ObserveSavedRecipesUseCase(favoritas, FakeCatalog(listOf(recipe)), recetasPropias),
        restoreRecipe = RestoreOwnRecipeUseCase(recetasPropias, favoritas),
        deletedStore = eliminadas,
    )

    /** Mantiene el estado observado, como la pantalla, y espera a que carguen las tarjetas. */
    private suspend fun TestScope.cargado(viewModel: SavedRecipesViewModel): SavedRecipesViewModel {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }

    @Test
    fun `CA-SAV-01-03 tocar el corazon quita la favorita del catalogo`() = runTest {
        val viewModel = cargado(viewModel())

        viewModel.onEvent(SavedRecipesEvent.FavoriteToggled("arepas"))
        viewModel.uiState.first { it.cards.isEmpty() }

        assertEquals(
            listOf(FakeFavoritesRepository.FavoriteCall("arepas", FavoriteSource.CATALOG, false)),
            favoritas.setCalls
        )
    }

    @Test
    fun `CA-SAV-02-02 al entrar el chip Favoritas esta elegido y no hay menu abierto`() = runTest {
        val state = cargado(viewModel()).uiState.value

        assertEquals(SavedFilter.FAVORITES, state.filter)
        assertEquals(SavedSort.RECENT, state.sort)
        assertFalse(state.isSortMenuOpen)
    }

    @Test
    fun `CA-SAV-02-05 Ordenar abre el menu y la eleccion se conserva al cambiar de chip`() = runTest {
        val viewModel = cargado(viewModel())

        viewModel.onEvent(SavedRecipesEvent.SortClicked)
        assertTrue(viewModel.uiState.first { it.isSortMenuOpen }.isSortMenuOpen)

        viewModel.onEvent(SavedRecipesEvent.SortChanged(SavedSort.NAME))
        viewModel.onEvent(SavedRecipesEvent.FilterChanged(SavedFilter.RECENT))

        val state = viewModel.uiState.first { it.filter == SavedFilter.RECENT }
        assertEquals(SavedSort.NAME, state.sort)
        assertFalse(state.isSortMenuOpen)
    }

    @Test
    fun `CA-SAV-02-05 cerrar el menu sin confirmar no cambia el orden`() = runTest {
        val viewModel = cargado(viewModel())
        viewModel.onEvent(SavedRecipesEvent.SortClicked)
        viewModel.uiState.first { it.isSortMenuOpen }

        viewModel.onEvent(SavedRecipesEvent.SortDismissed)

        assertEquals(SavedSort.RECENT, viewModel.uiState.first { !it.isSortMenuOpen }.sort)
    }

    @Test
    fun `CA-SAV-02-06 tocar una tarjeta del catalogo abre su detalle`() = runTest {
        val viewModel = cargado(viewModel())

        viewModel.onEvent(SavedRecipesEvent.CardClicked("arepas"))

        assertEquals(SavedRecipesEffect.OpenRecipe("arepas"), viewModel.effects.first())
    }

    @Test
    fun `CA-SAV-02-10 tocar el boton de crear abre Nueva receta`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(SavedRecipesEvent.AddClicked)

        assertEquals(SavedRecipesEffect.OpenNewRecipe, viewModel.effects.first())
    }

    @Test
    fun `CA-SAV-02-11 si las recetas no se pueden leer muestra el error y no se cierra`() = runTest {
        favoritas.observeFailure = IllegalStateException("PERMISSION_DENIED")

        val state = viewModel().uiState.first { !it.isLoading }

        assertTrue(state.hasError)
        assertFalse(state.isEmptyOverall)
    }

    @Test
    fun `CA-SAV-05-03 CA-SAV-05-05 el aviso cuenta de 8 a 1 y se cierra solo`() = runTest {
        val viewModel = cargado(viewModel())

        eliminadas.publish(DeletedOwnRecipe(ownRecipe(id = "r1"), wasFavorite = false, favoriteSavedAtMillis = 0L))
        runCurrent()
        assertEquals(8, viewModel.uiState.value.undoSecondsLeft)
        assertEquals("r1", viewModel.uiState.value.deleted?.recipe?.id)

        advanceTimeBy(7_000)
        runCurrent()
        assertEquals(1, viewModel.uiState.value.undoSecondsLeft)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(null, viewModel.uiState.value.deleted)
        assertTrue("CA-SAV-05-05: la eliminación queda definitiva", recetasPropias.recipes.value.isEmpty())
    }

    @Test
    fun `CA-SAV-05-04 deshacer devuelve la receta y cierra el aviso`() = runTest {
        val viewModel = cargado(viewModel())
        eliminadas.publish(DeletedOwnRecipe(ownRecipe(id = "r1"), wasFavorite = true, favoriteSavedAtMillis = 5L))
        runCurrent()

        viewModel.onEvent(SavedRecipesEvent.UndoClicked)
        runCurrent()

        assertEquals(null, viewModel.uiState.value.deleted)
        assertEquals(listOf("r1"), recetasPropias.recipes.value.map { it.id })
        assertEquals(FavoriteSource.OWN, favoritas.setCalls.last().source)
    }

    private class FakeCatalog(private val recipes: List<Recipe>) : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = recipes
        override suspend fun ingredients(): Map<String, com.smartfood.app.feature.recipes.domain.model.Ingredient> =
            emptyMap()
    }
}
