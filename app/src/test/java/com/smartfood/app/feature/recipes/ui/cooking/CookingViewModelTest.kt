package com.smartfood.app.feature.recipes.ui.cooking

import androidx.lifecycle.SavedStateHandle
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import com.smartfood.app.feature.recipes.domain.model.Store
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.usecase.GetRecipeDetailUseCase
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: CookingViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [CookingViewModel] (spec 009, HU-RIA-04, T019; Figma 557:2819 y 557:2781).
 */
class CookingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recipe = Recipe(
        id = "arepas_hogao",
        name = "Arepas con hogao",
        photo = "receta_arepas_con_hogao",
        servings = 2,
        prepMinutes = 10,
        cookMinutes = 15,
        difficulty = "Fácil",
        kcal = 350,
        ingredients = listOf(RecipeIngredient("arepa", 2.0)),
        beforeStarting = listOf("Ten listas las arepas", "Pica el tomate"),
        steps = listOf(
            RecipeStep("Calentar arepas", "Calienta las arepas en una sartén", minutes = 5),
            RecipeStep("Servir", "Sirve el hogao sobre las arepas", minutes = null)
        )
    )

    private val ingredient = Ingredient(
        id = "arepa",
        name = "Arepa de maíz",
        section = "Cereales",
        unit = IngredientUnit.UNIT,
        pack = 5.0,
        prices = mapOf(Store.D1 to 3000L)
    )

    private inner class FakeRecipeCatalog : RecipeCatalog {
        override suspend fun recipes(): List<Recipe> = listOf(recipe)
        override suspend fun ingredients(): Map<String, Ingredient> = mapOf("arepa" to ingredient)
    }

    private lateinit var getRecipeDetailUseCase: GetRecipeDetailUseCase

    @Before
    fun setUp() {
        getRecipeDetailUseCase = GetRecipeDetailUseCase(FakeRecipeCatalog())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `carga la receta e inicializa los pasos`() = runTest {
        val handle = SavedStateHandle(mapOf("recipeId" to "arepas_hogao", "servings" to 2))
        val viewModel = CookingViewModel(handle, getRecipeDetailUseCase)

        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Arepas con hogao", state.name)
        assertEquals(2, state.beforeStarting.size)
        assertEquals(2, state.steps.size)
        assertEquals(0, state.currentStepIndex)
        assertEquals(300, state.timerSecondsRemaining)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `avanza y retrocede de pasos correctamente`() = runTest {
        val handle = SavedStateHandle(mapOf("recipeId" to "arepas_hogao", "servings" to 2))
        val viewModel = CookingViewModel(handle, getRecipeDetailUseCase)
        runCurrent()

        viewModel.onEvent(CookingEvent.NextStepClicked)
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.isLastStep)

        viewModel.onEvent(CookingEvent.PreviousStepClicked)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertTrue(viewModel.uiState.value.isFirstStep)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `iniciar guia emite el efecto OpenGuide`() = runTest {
        val handle = SavedStateHandle(mapOf("recipeId" to "arepas_hogao", "servings" to 2))
        val viewModel = CookingViewModel(handle, getRecipeDetailUseCase)
        runCurrent()

        viewModel.onEvent(CookingEvent.StartGuideClicked)
        val effect = viewModel.effects.first()

        assertEquals(CookingEffect.OpenGuide, effect)
    }
}
