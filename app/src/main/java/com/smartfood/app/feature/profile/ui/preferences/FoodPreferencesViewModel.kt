package com.smartfood.app.feature.profile.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.usecase.GetFoodPreferencesUseCase
import com.smartfood.app.feature.profile.domain.usecase.SaveFoodPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * Nombre: FoodPreferencesViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de "Preferencias alimentarias" (spec 008, HU-PER-03, T012). Carga las preferencias guardadas,
 * lleva el borrador con las selecciones de los chips (varias en restricciones y cocinas; una en picante y presupuesto),
 * pide confirmar y guarda. "Guardado" se cierra solo a los 2 s, como en "Editar información".
 *
 * @param getPreferences Preferencias guardadas en `users/{uid}`.
 * @param savePreferences Guarda el mapa `preferences` y borra las claves que se quitaron.
 */
@HiltViewModel
class FoodPreferencesViewModel @Inject constructor(
    private val getPreferences: GetFoodPreferencesUseCase,
    private val savePreferences: SaveFoodPreferencesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodPreferencesUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<FoodPreferencesUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FoodPreferencesEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, como volver al Perfil. */
    val effects: Flow<FoodPreferencesEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            getPreferences()
                .onSuccess { preferences ->
                    _uiState.update { it.copy(isLoading = false, saved = preferences, draft = preferences) }
                }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.toAuthError()) } }
        }
    }

    /** Procesa un evento de la UI. */
    fun onEvent(event: FoodPreferencesEvent) {
        when (event) {
            is FoodPreferencesEvent.RestrictionToggled -> edit { preferences ->
                preferences.copy(restrictions = preferences.restrictions.toggle(event.restriction))
            }
            is FoodPreferencesEvent.CuisineToggled -> edit { preferences ->
                preferences.copy(cuisines = preferences.cuisines.toggle(event.cuisine))
            }
            is FoodPreferencesEvent.SpiceSelected -> edit { preferences ->
                // Solo uno: tocar el elegido lo quita (CA-PER-03-02).
                preferences.copy(spice = if (preferences.spice == event.level) null else event.level)
            }
            is FoodPreferencesEvent.BudgetSelected -> edit { preferences ->
                preferences.copy(budget = if (preferences.budget == event.budget) null else event.budget)
            }
            FoodPreferencesEvent.SaveClicked -> if (_uiState.value.isSaveEnabled) {
                _uiState.update { it.copy(dialog = FoodPreferencesDialog.ConfirmSave) }
            }
            FoodPreferencesEvent.SaveConfirmed -> save()
            FoodPreferencesEvent.DialogDismissed -> _uiState.update { it.copy(dialog = null) }
            // El borrador no se guarda: volver lo descarta sin preguntar, como en "Editar información" (CA-PER-02-13).
            FoodPreferencesEvent.BackClicked -> viewModelScope.launch {
                _effects.send(FoodPreferencesEffect.NavigateBack)
            }
        }
    }

    /** Cambia el borrador y quita el error anterior. */
    private fun edit(change: (FoodPreferences) -> FoodPreferences) {
        _uiState.update { it.copy(draft = change(it.draft), error = null) }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.isSaveEnabled) return
        _uiState.update { it.copy(dialog = null, isSaving = true, error = null) }
        viewModelScope.launch {
            savePreferences(state.draft)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, saved = state.draft, dialog = FoodPreferencesDialog.Saved)
                    }
                    closeSavedLater()
                }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false, error = error.toAuthError()) } }
        }
    }

    /** "Guardado" se cierra solo (CA-PER-03-03). */
    private suspend fun closeSavedLater() {
        delay(RESULT_DIALOG_MILLIS)
        _uiState.update { if (it.dialog is FoodPreferencesDialog.Saved) it.copy(dialog = null) else it }
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

    internal companion object {
        /** "Guardado" se cierra solo a los 2 s (CA-PER-03-03), como en "Editar información". */
        const val RESULT_DIALOG_MILLIS = 2_000L
    }
}
