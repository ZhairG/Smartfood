package com.smartfood.app.feature.profile.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import com.smartfood.app.feature.profile.domain.usecase.ChangePasswordUseCase
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
 * Nombre: ChangePasswordViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de la pantalla Cambiar contraseña (spec 008, HU-PER-05).
 *
 * @param changePasswordUseCase Actualiza la contraseña con reautenticación previa (CA-PER-05-04).
 * @param validatePasswordUseCase Evalúa las reglas de la nueva contraseña en tiempo real (CA-PER-05-02).
 */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ChangePasswordEffect>(Channel.BUFFERED)
    val effects: Flow<ChangePasswordEffect> = _effects.receiveAsFlow()

    fun onEvent(event: ChangePasswordEvent) {
        when (event) {
            is ChangePasswordEvent.CurrentPasswordChanged -> {
                _uiState.update {
                    it.copy(
                        currentPassword = event.value,
                        currentPasswordError = null,
                        generalError = null
                    )
                }
            }
            is ChangePasswordEvent.NewPasswordChanged -> {
                val validation = validatePasswordUseCase.execute(event.value)
                _uiState.update {
                    it.copy(
                        newPassword = event.value,
                        passwordValidation = validation,
                        generalError = null
                    )
                }
            }
            is ChangePasswordEvent.ConfirmPasswordChanged -> {
                _uiState.update {
                    it.copy(
                        confirmPassword = event.value,
                        generalError = null
                    )
                }
            }
            ChangePasswordEvent.SubmitClicked -> submitChangePassword()
            ChangePasswordEvent.SuccessDialogDismissed -> dismissSuccessAndNavigate()
        }
    }

    private fun submitChangePassword() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isLoading = true, currentPasswordError = null, generalError = null) }

        viewModelScope.launch {
            val result = changePasswordUseCase(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword,
                confirmPassword = state.confirmPassword
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSavedSuccess = true) }
                    delay(SUCCESS_DELAY_MS)
                    dismissSuccessAndNavigate()
                },
                onFailure = { throwable ->
                    val error = throwable.toAuthError()
                    _uiState.update {
                        if (error == AuthError.InvalidCredentials) {
                            it.copy(isLoading = false, currentPasswordError = error)
                        } else {
                            it.copy(isLoading = false, generalError = error)
                        }
                    }
                }
            )
        }
    }

    private fun dismissSuccessAndNavigate() {
        if (_uiState.value.isSavedSuccess) {
            _uiState.update { it.copy(isSavedSuccess = false) }
            viewModelScope.launch {
                _effects.send(ChangePasswordEffect.NavigateBack)
            }
        }
    }

    private companion object {
        const val SUCCESS_DELAY_MS = 2000L
    }
}
