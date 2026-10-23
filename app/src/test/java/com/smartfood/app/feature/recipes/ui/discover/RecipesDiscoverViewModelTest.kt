package com.smartfood.app.feature.recipes.ui.discover

import androidx.lifecycle.SavedStateHandle
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.RecipeFilter
import com.smartfood.app.feature.recipes.domain.usecase.AiFixtures
import com.smartfood.app.feature.recipes.domain.usecase.GetPreferredStoreUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetRecipesUseCase
import com.smartfood.app.feature.recipes.domain.usecase.RankRecipesForMeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.SearchRecipesUseCase
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RecipesDiscoverViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de [RecipesDiscoverViewModel] (spec 009, T017): "Ver recetas económicas" del flujo de la IA deja
 * el filtro en el `savedStateHandle` de Descubrir, que ya existe al volver, y la pantalla queda en "Económicas"
 * (CA-RIA-02-07).
 */
class RecipesDiscoverViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `CA-RIA-02-07 ver recetas economicas llega con el filtro Economicas`() = runTest {
        val fixtures = AiFixtures()
        val handle = SavedStateHandle()
        val viewModel = RecipesDiscoverViewModel(
            getRecipes = GetRecipesUseCase(fixtures.catalog),
            searchRecipes = SearchRecipesUseCase(),
            rankRecipes = RankRecipesForMeUseCase(IngredientMatcher(fixtures.clock)),
            observeInventory = ObserveInventoryUseCase(fixtures.inventory, FakePhotoSync(), ClassifyExpiryUseCase(fixtures.clock)),
            getPreferredStore = GetPreferredStoreUseCase(GetFoodPreferencesUseCase(fixtures.profile)),
            savedStateHandle = handle,
        )
        assertEquals(RecipeFilter.FOR_YOU, viewModel.uiState.first { !it.isLoading }.filter)

        // El ViewModel ya existe: así queda Descubrir al volver desde "Sin coincidencias".
        handle[RecipesDiscoverViewModel.FILTER_KEY] = RecipeFilter.ECONOMIC.name

        assertEquals(RecipeFilter.ECONOMIC, viewModel.uiState.first { it.filter == RecipeFilter.ECONOMIC }.filter)
        assertNull("La clave se borra al usarla", handle.get<String>(RecipesDiscoverViewModel.FILTER_KEY))
    }
}
