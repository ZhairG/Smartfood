package com.smartfood.app.feature.recipes.ui.cooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.model.toInventoryError
import com.smartfood.app.feature.recipes.domain.usecase.FinishCookingUseCase
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
 * Descripción: ViewModel de la cocina guiada (spec 009, HU-RIA-04, T019, T020, T021). Carga la receta por ID con
 * [GetRecipeDetailUseCase], gestiona el avance de pasos, el temporizador y el cierre con [FinishCookingUseCase]:
 * "¿Cocinaste esta receta?" → guardando → "Guardado" (o "Guardado en tu teléfono" sin conexión) durante
 * [RESULT_DIALOG_MILLIS] y vuelta al detalle. Si falla, muestra el error y la persona puede volver a terminar.
 *
 * @param savedStateHandle Contiene `recipeId` y `servings` de `Route.CookingIntro` o `Route.Cooking`.
 * @param getRecipeDetail Lectura de la receta del catálogo.
 * @param finishCookingUseCase Descuenta el inventario y guarda el registro de cocción.
 */
@HiltViewModel
class CookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRecipeDetail: GetRecipeDetailUseCase,
    private val finishCookingUseCase: FinishCookingUseCase,
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
    private var alarmJob: Job? = null

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
            CookingEvent.StartGuideClicked -> send(CookingEffect.OpenGuide)
            CookingEvent.NextStepClicked -> changeStep(delta = 1)
            CookingEvent.PreviousStepClicked -> changeStep(delta = -1)
            CookingEvent.ExitClicked -> {
                pauseTimer()
                stopAlarm()
                send(CookingEffect.ExitToDetail)
            }
            CookingEvent.FinishClicked -> _uiState.update { it.copy(dialog = CookingDialog.ConfirmFinish) }
            CookingEvent.DismissDialog -> _uiState.update { it.copy(dialog = null) }
            CookingEvent.ConfirmFinishClicked -> confirmFinish()
            CookingEvent.TimerToggled -> when {
                _uiState.value.isAlarmRinging -> stopAlarm()
                _uiState.value.isTimerRunning -> pauseTimer()
                else -> startTimer()
            }
            CookingEvent.ResetTimerClicked -> resetTimer()
        }
    }

    private fun send(effect: CookingEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun confirmFinish() {
        if (_uiState.value.dialog == CookingDialog.Saving) return
        pauseTimer()
        stopAlarm()
        _uiState.update { it.copy(dialog = CookingDialog.Saving) }
        viewModelScope.launch {
            finishCookingUseCase(recipeId, servings).fold(
                onSuccess = { outcome ->
                    _uiState.update { it.copy(dialog = CookingDialog.Saved(onPhone = outcome == WriteOutcome.PENDING)) }
                    delay(RESULT_DIALOG_MILLIS)
                    _uiState.update { it.copy(dialog = null) }
                    _effects.send(CookingEffect.ExitToDetail)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(dialog = CookingDialog.Failed(error.toInventoryError())) }
                    delay(RESULT_DIALOG_MILLIS)
                    _uiState.update { if (it.dialog is CookingDialog.Failed) it.copy(dialog = null) else it }
                }
            )
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
            if (_uiState.value.timerSecondsRemaining == 0) startAlarm()
        }
    }

    /** Suena hasta que la persona la detiene; si nadie la atiende, se calla sola a los [ALARM_MAX_MILLIS]. */
    private suspend fun startAlarm() {
        _uiState.update { it.copy(isAlarmRinging = true) }
        _effects.send(CookingEffect.PlayTimerFinishedAlert)
        alarmJob?.cancel()
        alarmJob = viewModelScope.launch {
            delay(ALARM_MAX_MILLIS)
            _uiState.update { it.copy(isAlarmRinging = false) }
        }
    }

    private fun stopAlarm() {
        if (!_uiState.value.isAlarmRinging) return
        alarmJob?.cancel()
        alarmJob = null
        _uiState.update { it.copy(isAlarmRinging = false) }
        send(CookingEffect.StopTimerAlert)
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    private fun resetTimer() {
        pauseTimer()
        stopAlarm()
        val step = _uiState.value.currentStep
        _uiState.update { it.copy(timerSecondsRemaining = (step?.minutes ?: 0) * 60) }
    }

    private fun changeStep(delta: Int) {
        pauseTimer()
        stopAlarm()
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

        /** Lo mismo que suena [TimerAlarm]: pasado ese tiempo la tarjeta deja de decir "¡Tiempo!". */
        const val ALARM_MAX_MILLIS = TimerAlarm.MAX_MILLIS

        /** Lo que se ve "Guardado" o el error antes de seguir, como en el editor y el Perfil. */
        const val RESULT_DIALOG_MILLIS = 2_000L
    }
}
