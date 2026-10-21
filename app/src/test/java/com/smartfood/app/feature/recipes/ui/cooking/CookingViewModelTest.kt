package com.smartfood.app.feature.recipes.ui.cooking

import androidx.lifecycle.SavedStateHandle
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.DeleteItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.UpdateItemUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ValidateItemUseCase
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.Recipe
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import com.smartfood.app.feature.recipes.domain.model.Store
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.usecase.FinishCookingUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetPreferredStoreUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetRecipeDetailUseCase
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: CookingViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [CookingViewModel] (spec 009, HU-RIA-04, T019, T020, T021): pasos, temporizador
 * (tocar la tarjeta lo inicia o pausa, avisa al llegar a 0), "Salir" y el cierre con "Guardado" o el error.
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

    private inner class FakeCookingHistoryRepository : CookingHistoryRepository {
        val saved = mutableListOf<CookingRecord>()
        override fun observeHistory(): Flow<List<CookingRecord>> = flowOf(saved)
        override fun newRecordId(): String = "record1"
        var result: Result<WriteOutcome> = Result.success(WriteOutcome.SYNCED)
        override suspend fun saveRecord(record: CookingRecord): Result<WriteOutcome> {
            saved.add(record)
            return result
        }
    }

    private inner class NoOpPhotoSync : PhotoSync {
        override fun cancelUpload(itemId: String) {}
        override fun enqueueUpload(itemId: String, photo: LocalPhoto, replacedPath: String?) {}
        override fun enqueueDelete(path: String) {}
        override fun pendingPhotos(): Flow<Map<String, String>> = flowOf(emptyMap())
        override fun hasPending(): Boolean = false
        override fun clearAll() {}
    }

    private lateinit var getRecipeDetailUseCase: GetRecipeDetailUseCase
    private lateinit var finishCookingUseCase: FinishCookingUseCase
    private lateinit var inventoryRepository: FakeInventoryRepository
    private lateinit var historyRepository: FakeCookingHistoryRepository
    private val today = LocalDate.of(2026, 10, 20)

    private fun viewModel() = CookingViewModel(
        SavedStateHandle(mapOf("recipeId" to "arepas_hogao", "servings" to 2)),
        getRecipeDetailUseCase,
        finishCookingUseCase
    )

    @Before
    fun setUp() {
        val clock = fixedClock(today)
        inventoryRepository = FakeInventoryRepository()
        historyRepository = FakeCookingHistoryRepository()
        val photoSync = NoOpPhotoSync()
        val classifyExpiry = ClassifyExpiryUseCase(clock)

        getRecipeDetailUseCase = GetRecipeDetailUseCase(FakeRecipeCatalog())
        val observeInventory = ObserveInventoryUseCase(inventoryRepository, photoSync, classifyExpiry)
        val getPreferredStore = GetPreferredStoreUseCase(GetFoodPreferencesUseCase(FakeProfileRepository()))
        val validateItem = ValidateItemUseCase(clock)
        val updateItem = UpdateItemUseCase(inventoryRepository, photoSync, validateItem)
        val deleteItem = DeleteItemUseCase(inventoryRepository, photoSync)
        val matcher = IngredientMatcher(clock)

        finishCookingUseCase = FinishCookingUseCase(
            getRecipeDetail = getRecipeDetailUseCase,
            observeInventory = observeInventory,
            getPreferredStore = getPreferredStore,
            updateItemUseCase = updateItem,
            deleteItemUseCase = deleteItem,
            cookingHistoryRepository = historyRepository,
            ingredientMatcher = matcher,
            clock = clock
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `carga la receta e inicializa los pasos`() = runTest {
        val handle = SavedStateHandle(mapOf("recipeId" to "arepas_hogao", "servings" to 2))
        val viewModel = CookingViewModel(handle, getRecipeDetailUseCase, finishCookingUseCase)

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
        val viewModel = CookingViewModel(handle, getRecipeDetailUseCase, finishCookingUseCase)
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
        val viewModel = CookingViewModel(handle, getRecipeDetailUseCase, finishCookingUseCase)
        runCurrent()

        viewModel.onEvent(CookingEvent.StartGuideClicked)
        val effect = viewModel.effects.first()

        assertEquals(CookingEffect.OpenGuide, effect)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CA-RIA-04-04 tocar la tarjeta inicia y pausa el temporizador y Reiniciar vuelve al tiempo del paso`() = runTest {
        val handle = SavedStateHandle(mapOf("recipeId" to "arepas_hogao", "servings" to 2))
        val viewModel = CookingViewModel(handle, getRecipeDetailUseCase, finishCookingUseCase)
        runCurrent()

        assertEquals(300, viewModel.uiState.value.timerSecondsRemaining)
        assertFalse(viewModel.uiState.value.isTimerRunning)

        viewModel.onEvent(CookingEvent.TimerToggled)
        assertTrue(viewModel.uiState.value.isTimerRunning)

        viewModel.onEvent(CookingEvent.TimerToggled)
        assertFalse(viewModel.uiState.value.isTimerRunning)

        viewModel.onEvent(CookingEvent.ResetTimerClicked)
        assertEquals(300, viewModel.uiState.value.timerSecondsRemaining)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CA-RIA-04-04 al llegar a 0 el temporizador avisa con sonido y vibracion`() = runTest {
        val viewModel = viewModel()
        runCurrent()

        viewModel.onEvent(CookingEvent.TimerToggled)
        advanceTimeBy(300_001)

        assertEquals(0, viewModel.uiState.value.timerSecondsRemaining)
        assertFalse(viewModel.uiState.value.isTimerRunning)
        assertEquals(CookingEffect.PlayTimerFinishedAlert, viewModel.effects.first())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CA-RIA-04-04 Salir vuelve al detalle sin descontar nada`() = runTest {
        val viewModel = viewModel()
        runCurrent()

        viewModel.onEvent(CookingEvent.ExitClicked)

        assertEquals(CookingEffect.ExitToDetail, viewModel.effects.first())
        assertTrue(historyRepository.saved.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CA-RIA-04-05 Terminar pregunta, guarda, muestra Guardado y vuelve al detalle`() = runTest {
        inventoryRepository.createItem(
            InventoryItem("item1", "Arepa de maíz", FoodCategory.GRAINS, today.plusDays(1), Quantity(5.0, QuantityUnit.UNITS))
        )
        val viewModel = viewModel()
        runCurrent()

        viewModel.onEvent(CookingEvent.FinishClicked)
        assertEquals(CookingDialog.ConfirmFinish, viewModel.uiState.value.dialog)

        viewModel.onEvent(CookingEvent.ConfirmFinishClicked)
        runCurrent()
        assertEquals(CookingDialog.Saved(onPhone = false), viewModel.uiState.value.dialog)

        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.dialog)
        assertEquals(CookingEffect.ExitToDetail, viewModel.effects.first())
        assertEquals(1, historyRepository.saved.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CA-RIA-04-05 sin conexion muestra Guardado en tu telefono`() = runTest {
        historyRepository.result = Result.success(WriteOutcome.PENDING)
        val viewModel = viewModel()
        runCurrent()

        viewModel.onEvent(CookingEvent.ConfirmFinishClicked)
        runCurrent()

        assertEquals(CookingDialog.Saved(onPhone = true), viewModel.uiState.value.dialog)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CA-RIA-04-05 si falla muestra el error y se queda en la guia`() = runTest {
        historyRepository.result = Result.failure(IllegalStateException("sin permiso"))
        val viewModel = viewModel()
        runCurrent()

        viewModel.onEvent(CookingEvent.ConfirmFinishClicked)
        runCurrent()
        assertTrue(viewModel.uiState.value.dialog is CookingDialog.Failed)

        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.dialog)
    }
}
