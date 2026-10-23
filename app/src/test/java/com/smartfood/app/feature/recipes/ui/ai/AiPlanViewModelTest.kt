package com.smartfood.app.feature.recipes.ui.ai

import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
import com.smartfood.app.feature.recipes.domain.usecase.AiFixtures
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: AiPlanViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de [AiPlanViewModel] (spec 009, T014) con el catálogo real: el contexto cargado (CA-RIA-02-01),
 * "Analizando opciones" al menos 1,5 s y "Cancelar" (CA-RIA-02-02), el plan con sus razones (CA-RIA-02-03, 02-06),
 * "Sin coincidencias" (CA-RIA-02-07) y "Ajustar" con los mismos datos (CA-RIA-02-08).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiPlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(fixtures: AiFixtures = AiFixtures()) =
        AiPlanViewModel(fixtures.getAiContext, fixtures.generatePlan, fixtures.clock)

    @Test
    fun `CA-RIA-02-01 carga el inventario detectado, el que vence primero, las preferencias y la tienda`() = runTest {
        val viewModel = viewModel(AiFixtures(setOf(DietaryRestriction.VEGETARIAN)))
        val state = viewModel.uiState.first { !it.isLoadingContext }

        assertEquals("Pollo", state.detected.first().name)
        assertEquals("Pollo", state.firstExpiring?.name)
        assertEquals(1, state.firstExpiringDays)
        assertEquals(setOf("pollo", "tomate"), state.expiringIds)
        assertEquals(setOf(DietaryRestriction.VEGETARIAN), state.restrictions)
        assertEquals(AiUiState.DEFAULT_BUDGET_COP, state.budgetCop)
        assertNull("El tiempo arranca en Sin límite", state.maxMinutes)
    }

    @Test
    fun `CA-RIA-02-02 CA-RIA-02-03 generar muestra analizando al menos 1,5 s y luego el plan con sus razones`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { !it.isLoadingContext }

        viewModel.onEvent(AiEvent.BudgetChanged(100_000))
        viewModel.onEvent(AiEvent.GenerateClicked)
        runCurrent()
        assertEquals(AiStep.Loading, viewModel.uiState.value.step)
        advanceTimeBy(AiPlanViewModel.MIN_LOADING_MILLIS - 100)
        assertEquals(AiStep.Loading, viewModel.uiState.value.step)

        val state = viewModel.uiState.first { it.step != AiStep.Loading }
        assertEquals(AiStep.Plan, state.step)
        assertEquals(3, state.plan?.days?.size)
        assertTrue("¿Por qué te conviene? sin razones", state.reasons.isNotEmpty())
    }

    @Test
    fun `CA-RIA-02-02 cancelar vuelve al contexto sin resultado`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { !it.isLoadingContext }

        viewModel.onEvent(AiEvent.GenerateClicked)
        runCurrent()
        viewModel.onEvent(AiEvent.CancelClicked)
        advanceTimeBy(AiPlanViewModel.MIN_LOADING_MILLIS * 2)

        assertEquals(AiStep.Context, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.plan)
    }

    @Test
    fun `CA-RIA-02-07 con COP 2000 muestra sin coincidencias con sugerencias`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { !it.isLoadingContext }

        viewModel.onEvent(AiEvent.BudgetChanged(2_000))
        viewModel.onEvent(AiEvent.GenerateClicked)
        val state = viewModel.uiState.first { it.step == AiStep.Plan || it.step == AiStep.NoPlan }

        assertEquals(AiStep.NoPlan, state.step)
        assertTrue(state.suggestions.any { it is PlanSuggestion.RaiseBudget })
    }

    @Test
    fun `CA-RIA-02-08 ajustar vuelve al contexto con los mismos datos`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { !it.isLoadingContext }
        viewModel.onEvent(AiEvent.BudgetChanged(72_400))
        viewModel.onEvent(AiEvent.ServingsClicked)
        viewModel.onEvent(AiEvent.ServingsPicked(6))
        viewModel.onEvent(AiEvent.TimeClicked)
        viewModel.onEvent(AiEvent.TimePicked(45))
        viewModel.onEvent(AiEvent.SpicyToggled)
        viewModel.onEvent(AiEvent.GenerateClicked)
        viewModel.uiState.first { it.step == AiStep.Plan || it.step == AiStep.NoPlan }

        viewModel.onEvent(AiEvent.AdjustClicked)

        val state = viewModel.uiState.value
        assertEquals(AiStep.Context, state.step)
        assertEquals(72_000L, state.budgetCop)
        assertEquals(6, state.servings)
        assertEquals(45, state.maxMinutes)
        assertTrue(state.noSpicy)
        assertNull(state.sheet)
    }
}
