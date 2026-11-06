package com.smartfood.app.feature.recipes.ui.own

import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.usecase.GetOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.SaveOwnRecipeUseCase
import com.smartfood.app.feature.recipes.domain.usecase.ValidateOwnRecipeUseCase
import com.smartfood.app.testutil.FakeFavoritesRepository
import com.smartfood.app.testutil.FakeOwnRecipesRepository
import com.smartfood.app.testutil.ownRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.smartfood.app.testutil.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class OwnRecipeEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val recipes = FakeOwnRecipesRepository()
    private val favorites = FakeFavoritesRepository()
    private val save = SaveOwnRecipeUseCase(recipes, favorites, ValidateOwnRecipeUseCase())
    private val viewModel by lazy { OwnRecipeEditorViewModel(save, GetOwnRecipeUseCase(recipes)) }

    @get:Rule val mainDispatcherRule = MainDispatcherRule(dispatcher)

    @Test fun `CA-SAV-03-04 guardar emite efecto con receta creada`() = runTest(dispatcher) {
        viewModel.onEvent(OwnRecipeEditorEvent.NameChanged("Arroz casero"))
        viewModel.onEvent(OwnRecipeEditorEvent.CategoryPicked(com.smartfood.app.feature.recipes.domain.model.RecipeCategory.ARROCES))
        viewModel.onEvent(OwnRecipeEditorEvent.CookMinutesChanged("30"))
        viewModel.onEvent(OwnRecipeEditorEvent.PreparationChanged("Cocinar"))
        viewModel.onEvent(OwnRecipeEditorEvent.SaveClicked)
        runCurrent()
        assertEquals(OwnRecipeEditorEffect.Saved("own-recipe-1", "Arroz casero", false), viewModel.effects.first())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test fun `CA-SAV-04-01 inicializar edicion carga la receta`() = runTest(dispatcher) {
        recipes.recipes.value = listOf(ownRecipe(id = "edit-1"))
        viewModel.initialize("edit-1")
        runCurrent()
        assertEquals("Arroz casero", viewModel.uiState.value.draft.name)
        assertEquals("30", viewModel.uiState.value.draft.cookMinutes)
    }
}

