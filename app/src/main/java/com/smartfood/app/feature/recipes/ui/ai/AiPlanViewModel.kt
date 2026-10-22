package com.smartfood.app.feature.recipes.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.recipes.domain.model.PlanResult
import com.smartfood.app.feature.recipes.domain.usecase.GeneratePlanUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/** Nombre: AiPlanViewModel.kt. Autor: SmartFood Team. Fecha: Octubre 2026. Descripción: Orquesta el flujo de plan IA. */
@HiltViewModel
class AiPlanViewModel @Inject constructor(
    private val generatePlan: GeneratePlanUseCase,
    observeInventory: ObserveInventoryUseCase,
    private val getFoodPreferences: GetFoodPreferencesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AiUiState())
    val uiState = _state.asStateFlow()
    private val _effects = Channel<AiEffect>(Channel.BUFFERED)
    val effects: Flow<AiEffect> = _effects.receiveAsFlow()
    private var generation: Job? = null

    init { viewModelScope.launch { val items = observeInventory().first().flatMap { section -> section.items }.sortedBy { item -> item.expiresOn }; val restrictions = getFoodPreferences().getOrNull()?.restrictions.orEmpty().map { it.name }; _state.update { it.copy(detected = items, firstExpiring = items.firstOrNull(), restrictions = restrictions) } } }

    fun onEvent(event: AiEvent) {
        when (event) {
            AiEvent.GenerateClicked -> generate()
            AiEvent.CancelClicked -> { generation?.cancel(); _state.update { it.copy(step = AiStep.Context, plan = null, suggestions = emptyList()) } }
            AiEvent.AdjustClicked -> _state.update { it.copy(step = AiStep.Context) }
            AiEvent.HelpClicked -> send(AiEffect.OpenHelp)
            is AiEvent.RecipeClicked -> send(AiEffect.OpenRecipe(event.recipeId))
            AiEvent.EconomicRecipesClicked -> send(AiEffect.OpenEconomicRecipes)
            is AiEvent.BudgetChanged -> _state.update { it.copy(context = it.context.copy(budgetCop = event.value)) }
            is AiEvent.ServingsChanged -> _state.update { it.copy(context = it.context.copy(servings = event.value)) }
            is AiEvent.NoSpicyChanged -> _state.update { it.copy(context = it.context.copy(noSpicy = event.value)) }
            is AiEvent.MaxMinutesChanged -> _state.update { it.copy(context = it.context.copy(maxMinutes = event.value)) }
        }
    }

    private fun generate() { generation?.cancel(); _state.update { it.copy(step = AiStep.Loading) }; generation = viewModelScope.launch { coroutineScope { val result = async { generatePlan(_state.value.context) }; delay(MIN_LOADING_MILLIS); when (val value = result.await()) { is PlanResult.Found -> _state.update { it.copy(step = AiStep.Plan, plan = value.plan) }; is PlanResult.NoPlan -> _state.update { it.copy(step = AiStep.NoPlan, suggestions = value.suggestions) } } } } }
    private fun send(effect: AiEffect) { viewModelScope.launch { _effects.send(effect) } }
    companion object { const val MIN_LOADING_MILLIS = 1_500L }
}
