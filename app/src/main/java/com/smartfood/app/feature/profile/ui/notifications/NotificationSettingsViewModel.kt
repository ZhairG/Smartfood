package com.smartfood.app.feature.profile.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.profile.domain.model.NotificationSettings
import com.smartfood.app.feature.profile.domain.repository.NotificationSettingsRepository
import com.smartfood.app.feature.profile.domain.usecase.SetNotificationSettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Nombre: NotificationSettingsViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de "Notificaciones" (spec 008, HU-PER-04, T017). Lee los ajustes guardados en el teléfono y,
 * al tocar un interruptor, los cambia y los guarda al instante (CA-PER-04-02); el caso de uso programa o cancela el
 * aviso diario (CA-PER-04-03). El permiso de Android 13+ lo gestiona el Route, que avisa aquí si quedó negado
 * (CA-PER-04-06).
 *
 * @param repository Ajustes guardados en este teléfono.
 * @param setNotificationSetting Guarda el ajuste y programa o cancela el aviso diario.
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: NotificationSettingsRepository,
    private val setNotificationSetting: SetNotificationSettingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<NotificationSettingsEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, como volver al Perfil o abrir los ajustes del teléfono. */
    val effects: Flow<NotificationSettingsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { settings ->
                _uiState.update { it.copy(isLoading = false, settings = settings) }
            }
        }
    }

    /** Procesa un evento de la UI. */
    fun onEvent(event: NotificationSettingsEvent) {
        when (event) {
            is NotificationSettingsEvent.ToggleRequested -> change(event.setting, event.enabled)
            NotificationSettingsEvent.PermissionDenied ->
                _uiState.update { it.copy(dialog = NotificationSettingsDialog.PermissionDenied) }
            NotificationSettingsEvent.OpenSettingsClicked -> {
                _uiState.update { it.copy(dialog = null) }
                viewModelScope.launch { _effects.send(NotificationSettingsEffect.OpenAppSettings) }
            }
            NotificationSettingsEvent.DialogDismissed -> _uiState.update { it.copy(dialog = null) }
            NotificationSettingsEvent.BackClicked ->
                viewModelScope.launch { _effects.send(NotificationSettingsEffect.NavigateBack) }
        }
    }

    /** Cambia un interruptor y guarda de inmediato, sin botón "Guardar" (CA-PER-04-02). */
    private fun change(setting: NotificationSetting, enabled: Boolean) {
        val updated = _uiState.value.settings.with(setting, enabled)
        _uiState.update { it.copy(settings = updated) }
        viewModelScope.launch { setNotificationSetting(updated) }
    }

    private fun NotificationSettings.with(setting: NotificationSetting, enabled: Boolean): NotificationSettings =
        when (setting) {
            NotificationSetting.EXPIRING_PRODUCTS -> copy(expiringProducts = enabled)
            NotificationSetting.SUGGESTED_RECIPES -> copy(suggestedRecipes = enabled)
            NotificationSetting.EMPTY_INVENTORY -> copy(emptyInventory = enabled)
        }
}
