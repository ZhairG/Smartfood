package com.smartfood.app.feature.recipes.ui.discover

import androidx.lifecycle.SavedStateHandle
import com.smartfood.app.core.search.domain.ClearRecentSearchesUseCase
import com.smartfood.app.core.search.domain.ObserveRecentSearchesUseCase
import com.smartfood.app.core.search.domain.RecordRecentSearchUseCase
import com.smartfood.app.core.search.domain.SearchScope
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.domain.usecase.AiFixtures
import com.smartfood.app.feature.recipes.domain.usecase.GetPreferredStoreUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetRecipesUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ObserveFavoriteIdsUseCase
import com.smartfood.app.feature.recipes.domain.usecase.RankRecipesForMeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.SearchRecipesUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ToggleFavoriteUseCase
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.FakeRecentSearchesRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RecipesDiscoverViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de [RecipesDiscoverViewModel] (spec 009, T017): "Ver recetas económicas" del flujo de la IA deja
 * el filtro en el `savedStateHandle` de Descubrir, que ya existe al volver, y la pantalla queda en "Económicas"
 * (CA-RIA-02-07). También las búsquedas recientes (spec 014, T005): se guardan al tocar un resultado o pulsar
 * «Buscar», un chip busca su texto y «Borrar» las quita (CA-LOC-02-01, 02-02, 02-05 y 02-06).
 */
class RecipesDiscoverViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixtures = AiFixtures()
    private val recents = FakeRecentSearchesRepository()

    private fun viewModel(
        favorites: FakeFavoritesRepository = FakeFavoritesRepository().apply { favorites.value = emptyList() },
        handle: SavedStateHandle = SavedStateHandle(),
    ) = RecipesDiscoverViewModel(
        getRecipes = GetRecipesUseCase(fixtures.catalog),
        searchRecipes = SearchRecipesUseCase(),
        rankRecipes = RankRecipesForMeUseCase(IngredientMatcher(fixtures.clock)),
        observeInventory = ObserveInventoryUseCase(fixtures.inventory, FakePhotoSync(), ClassifyExpiryUseCase(fixtures.clock)),
        getPreferredStore = GetPreferredStoreUseCase(GetFoodPreferencesUseCase(fixtures.profile)),
        observeFavoriteIds = ObserveFavoriteIdsUseCase(favorites),
        toggleFavorite = ToggleFavoriteUseCase(favorites),
        savedStateHandle = handle,
        observeRecentSearches = ObserveRecentSearchesUseCase(recents),
        recordRecentSearch = RecordRecentSearchUseCase(recents, fixtures.clock),
        clearRecentSearches = ClearRecentSearchesUseCase(recents),
    )

    @Test
    fun `CA-RIA-02-07 ver recetas economicas llega con el filtro Economicas`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = viewModel(handle = handle)
        assertEquals(RecipeFilter.FOR_YOU, viewModel.uiState.first { !it.isLoading }.filter)

        // El ViewModel ya existe: así queda Descubrir al volver desde "Sin coincidencias".
        handle[RecipesDiscoverViewModel.FILTER_KEY] = RecipeFilter.ECONOMIC.name

        assertEquals(RecipeFilter.ECONOMIC, viewModel.uiState.first { it.filter == RecipeFilter.ECONOMIC }.filter)
        assertNull("La clave se borra al usarla", handle.get<String>(RecipesDiscoverViewModel.FILTER_KEY))
    }

    @Test
    fun `CA-SAV-02-11 si las favoritas no se pueden leer las recetas se ven sin corazon y no se cierra`() = runTest {
        val favorites = FakeFavoritesRepository().apply {
            observeFailure = IllegalStateException("PERMISSION_DENIED")
        }

        val state = viewModel(favorites = favorites).uiState.first { !it.isLoading }

        assertEquals(emptySet<String>(), state.favoriteIds)
    }

    @Test
    fun `CA-LOC-02-02 tocar un resultado guarda la busqueda y escribir no la guarda`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { !it.isLoading }

        viewModel.onEvent(RecipesDiscoverEvent.QueryChanged("po"))
        viewModel.onEvent(RecipesDiscoverEvent.QueryChanged("pollo"))
        advanceUntilIdle()
        assertTrue("Escribir no guarda", recents.texts(SearchScope.RECIPES).isEmpty())

        viewModel.onEvent(RecipesDiscoverEvent.RecipeClicked("arroz-pollo"))
        advanceUntilIdle()

        assertEquals(listOf("pollo"), recents.texts(SearchScope.RECIPES))
    }

    @Test
    fun `CA-LOC-02-02 pulsar Buscar guarda la busqueda`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(RecipesDiscoverEvent.QueryChanged("lentejas"))

        viewModel.onEvent(RecipesDiscoverEvent.SearchSubmitted)
        advanceUntilIdle()

        assertEquals(listOf("lentejas"), recents.texts(SearchScope.RECIPES))
        assertTrue("Las del Inventario no cambian", recents.texts(SearchScope.INVENTORY).isEmpty())
    }

    @Test
    fun `CA-LOC-02-01 CA-LOC-02-05 los chips salen en el estado y tocar uno busca su texto`() = runTest {
        RecordRecentSearchUseCase(recents, fixtures.clock)(SearchScope.RECIPES, "arroz")
        val viewModel = viewModel()
        assertEquals(listOf("arroz"), viewModel.uiState.first { it.recentSearches.isNotEmpty() }.recentSearches)

        viewModel.onEvent(RecipesDiscoverEvent.RecentClicked("arroz"))

        assertEquals("arroz", viewModel.uiState.first { it.query == "arroz" }.query)
    }

    @Test
    fun `CA-LOC-02-06 Borrar quita las recientes de Recetas`() = runTest {
        RecordRecentSearchUseCase(recents, fixtures.clock)(SearchScope.RECIPES, "arroz")
        RecordRecentSearchUseCase(recents, fixtures.clock)(SearchScope.INVENTORY, "leche")
        val viewModel = viewModel()

        viewModel.onEvent(RecipesDiscoverEvent.ClearRecents)
        advanceUntilIdle()

        assertTrue(recents.texts(SearchScope.RECIPES).isEmpty())
        assertEquals(listOf("leche"), recents.texts(SearchScope.INVENTORY))
    }
}
