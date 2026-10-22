package com.smartfood.app.feature.recipes.ui.ai

import com.smartfood.app.feature.inventory.domain.usecase.ClassifyExpiryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.recipes.domain.agent.IngredientMatcher
import com.smartfood.app.feature.recipes.domain.model.PlanContext
import com.smartfood.app.feature.recipes.domain.repository.RecipeCatalog
import com.smartfood.app.feature.recipes.domain.usecase.GeneratePlanUseCase
import com.smartfood.app.feature.recipes.domain.usecase.GetPreferredStoreUseCase
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.testutil.FakeInventoryRepository
import com.smartfood.app.testutil.FakePhotoSync
import com.smartfood.app.testutil.FakeProfileRepository
import com.smartfood.app.testutil.MainDispatcherRule
import com.smartfood.app.testutil.fixedClock
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Nombre: AiPlanViewModelTest.kt. Autor: SmartFood Team. Fecha: Octubre 2026. Descripción: Pruebas UDF del flujo T014. */
class AiPlanViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test
    fun `CA-RIA-02-02 cancelar vuelve al contexto sin resultado`() = runTest {
        val catalog = object : RecipeCatalog {
            override suspend fun recipes() = emptyList<com.smartfood.app.feature.recipes.domain.model.Recipe>()
            override suspend fun ingredients() = emptyMap<String, com.smartfood.app.feature.recipes.domain.model.Ingredient>()
        }
        val clock = fixedClock(LocalDate.of(2026, 10, 20))
        val observe = ObserveInventoryUseCase(FakeInventoryRepository(), FakePhotoSync(), ClassifyExpiryUseCase(clock))
        val prefs = GetFoodPreferencesUseCase(FakeProfileRepository())
        val useCase = GeneratePlanUseCase(observe, catalog, IngredientMatcher(clock), prefs, GetPreferredStoreUseCase(prefs))
        val viewModel = AiPlanViewModel(useCase, observe, prefs)
        viewModel.onEvent(AiEvent.GenerateClicked)
        runCurrent()
        assertEquals(AiStep.Loading, viewModel.uiState.value.step)
        viewModel.onEvent(AiEvent.CancelClicked)
        assertEquals(AiStep.Context, viewModel.uiState.value.step)
        advanceTimeBy(1_500)
        assertEquals(null, viewModel.uiState.value.plan)
    }

    @Test
    fun `CA-RIA-02-08 ajustar conserva el contexto`() = runTest {
        val clock = fixedClock(LocalDate.of(2026, 10, 20))
        val observe = ObserveInventoryUseCase(FakeInventoryRepository(), FakePhotoSync(), ClassifyExpiryUseCase(clock))
        val prefs = GetFoodPreferencesUseCase(FakeProfileRepository())
        val catalog = object : RecipeCatalog { override suspend fun recipes() = emptyList<com.smartfood.app.feature.recipes.domain.model.Recipe>(); override suspend fun ingredients() = emptyMap<String, com.smartfood.app.feature.recipes.domain.model.Ingredient>() }
        val vm = AiPlanViewModel(GeneratePlanUseCase(observe, catalog, IngredientMatcher(clock), prefs, GetPreferredStoreUseCase(prefs)), observe, prefs)
        vm.onEvent(AiEvent.BudgetChanged(72_000)); vm.onEvent(AiEvent.ServingsChanged(6)); vm.onEvent(AiEvent.AdjustClicked)
        assertEquals(PlanContext(72_000, 6), vm.uiState.value.context)
        assertEquals(AiStep.Context, vm.uiState.value.step)
    }
}
