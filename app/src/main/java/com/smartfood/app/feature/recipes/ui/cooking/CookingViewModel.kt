package com.smartfood.app.feature.recipes.ui.cooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.recipes.domain.usecase.GetRecipeDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Nombre: CookingViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de la cocina guiada (spec 009, HU-RIA-04, T019, T020). Carga la receta por ID con [GetRecipeDetailUseCase]
 * y gestiona el avance de pasos y el temporizador.
 *
 * @param savedStateHandle Contiene `recipeId` y `servings` de `Route.CookingIntro` o `Route.Cooking`.
 * @param getRecipeDetail Lectura de la receta del catálogo.
 */
@HiltViewModel
class CookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRecipeDetail: GetRecipeDetailUseCase,
) : ViewModel() {

    val recipeId: String = checkNotNull(savedStateHandle.get<String>(RECIPE_ID_KEY)) {
        "Falta el argumento '$RECIPE_ID_KEY'"
    }

    val servings: Int = savedStateHandle.get<Int>(SERVINGS_KEY) ?: DEFAULT_SERVINGS

    private val _uiState = MutableStateFlow(CookingUiState(recipeId = recipeId, servings = servings))
    val uiState: StateFlow<CookingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CookingEffect>(Channel.BUFFERED)
    val effects: Flow<CookingEffect> = _effects.receiveAsFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            getRecipeDetail(recipeId).fold(
                onSuccess = { detail ->
                    val recipe = detail.recipe
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            name = recipe.name,
                            photo = recipe.photo,
                            beforeStarting = recipe.beforeStarting,
                            steps = recipe.steps,
                            currentStepIndex = 0,
                            timerSecondsRemaining = (recipe.steps.firstOrNull()?.minutes ?: 0) * 60
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                }
            )
        }
    }

    fun onEvent(event: CookingEvent) {
        when (event) {
            CookingEvent.StartGuideClicked -> {
                viewModelScope.launch { _effects.send(CookingEffect.OpenGuide) }
            }
            CookingEvent.NextStepClicked -> changeStep(delta = 1)
            CookingEvent.PreviousStepClicked -> changeStep(delta = -1)
            CookingEvent.ExitClicked -> {
                pauseTimer()
                viewModelScope.launch { _effects.send(CookingEffect.NavigateBack) }
            }
            CookingEvent.FinishClicked -> {
                _uiState.update { it.copy(isFinishConfirmVisible = true) }
            }
            CookingEvent.DismissFinishDialog -> {
                _uiState.update { it.copy(isFinishConfirmVisible = false) }
            }
            CookingEvent.ConfirmFinishClicked -> {
                _uiState.update { it.copy(isFinishConfirmVisible = false) }
            }
            CookingEvent.StartTimerClicked -> startTimer()
            CookingEvent.PauseTimerClicked -> pauseTimer()
            CookingEvent.ResetTimerClicked -> resetTimer()
            CookingEvent.ResultDialogDismissed -> {
                viewModelScope.launch { _effects.send(CookingEffect.NavigateBack) }
            }
        }
    }

    private fun startTimer() {
        if (_uiState.value.isTimerRunning || _uiState.value.timerSecondsRemaining <= 0) return
        _uiState.update { it.copy(isTimerRunning = true) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSecondsRemaining > 0 && _uiState.value.isTimerRunning) {
                delay(1000L)
                _uiState.update { current ->
                    val newTime = current.timerSecondsRemaining - 1
                    if (newTime <= 0) {
                        current.copy(timerSecondsRemaining = 0, isTimerRunning = false)
                    } else {
                        current.copy(timerSecondsRemaining = newTime)
                    }
                }
            }
            if (_uiState.value.timerSecondsRemaining == 0) {
                _effects.send(CookingEffect.PlayTimerFinishedAlert)
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    private fun resetTimer() {
        pauseTimer()
        val step = _uiState.value.currentStep
        _uiState.update { it.copy(timerSecondsRemaining = (step?.minutes ?: 0) * 60) }
    }

    private fun changeStep(delta: Int) {
        pauseTimer()
        val state = _uiState.value
        val newIndex = (state.currentStepIndex + delta).coerceIn(0, state.steps.lastIndex.coerceAtLeast(0))
        val step = state.steps.getOrNull(newIndex)
        _uiState.update {
            it.copy(
                currentStepIndex = newIndex,
                timerSecondsRemaining = (step?.minutes ?: 0) * 60,
                isTimerRunning = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
    }

    companion object {
        const val RECIPE_ID_KEY = "recipeId"
        const val SERVINGS_KEY = "servings"
        const val DEFAULT_SERVINGS = 2
    }
}
