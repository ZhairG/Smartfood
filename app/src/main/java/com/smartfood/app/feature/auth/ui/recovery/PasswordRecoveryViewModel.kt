package com.smartfood.app.feature.auth.ui.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.auth.domain.usecase.RequestRecoveryCodeUseCase
import com.smartfood.app.feature.auth.domain.usecase.ResetPasswordUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import com.smartfood.app.feature.auth.domain.usecase.VerifyRecoveryCodeUseCase
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
 * Nombre: PasswordRecoveryViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel del flujo de recuperación de contraseña en 3 pasos (HU-04).
 * Al salir de la pantalla el ViewModel se destruye, por lo que el flujo siempre vuelve a empezar en el
 * paso 1 (CA-04-12). Si el paso 3 falla porque el código ya no vale, regresa al paso 2 (CA-REC-02-03).
 *
 * @param requestCode Caso de uso del paso 1.
 * @param verifyCode Caso de uso del paso 2.
 * @param resetPassword Caso de uso del paso 3.
 * @param validatePassword Validador usado para la lista de requisitos en tiempo real.
 */
@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val requestCode: RequestRecoveryCodeUseCase,
    private val verifyCode: VerifyRecoveryCodeUseCase,
    private val resetPassword: ResetPasswordUseCase,
    private val validatePassword: ValidatePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PasswordRecoveryEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, como la navegación. */
    val effects: Flow<PasswordRecoveryEffect> = _effects.receiveAsFlow()

    /** Procesa un evento de la UI. */
    fun onEvent(event: PasswordRecoveryEvent) {
        when (event) {
            is PasswordRecoveryEvent.EmailChanged -> _uiState.update { it.copy(email = event.email, error = null) }
            is PasswordRecoveryEvent.OtpCodeChanged -> _uiState.update { it.copy(otpCode = event.code, error = null) }
            is PasswordRecoveryEvent.NewPasswordChanged -> _uiState.update {
                it.copy(
                    newPassword = event.password,
                    passwordValidation = validatePassword.execute(event.password),
                    error = null
                )
            }
            is PasswordRecoveryEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.password, error = null)
            }
            PasswordRecoveryEvent.SendCodeClicked, PasswordRecoveryEvent.ResendCodeClicked -> sendCode()
            PasswordRecoveryEvent.VerifyCodeClicked -> verify()
            PasswordRecoveryEvent.ChangeEmailClicked -> _uiState.update {
                it.copy(step = RecoveryStep.EMAIL_REQUEST, otpCode = "", error = null)
            }
            PasswordRecoveryEvent.ResetPasswordClicked -> reset()
        }
    }

    /** Paso 1 y "Reenviar código": en ambos casos se pasa (o se queda) en el paso 2 con el código borrado. */
    private fun sendCode() = runStep(
        action = { requestCode(it.email) },
        onSuccess = { it.copy(step = RecoveryStep.OTP_VERIFICATION, otpCode = "") }
    )

    private fun verify() = runStep(
        action = { verifyCode(it.email, it.otpCode) },
        onSuccess = { it.copy(step = RecoveryStep.NEW_PASSWORD) }
    )

    private fun reset() = runStep(
        action = { resetPassword(it.email, it.otpCode, it.newPassword, it.confirmPassword) },
        onSuccess = { it },
        onFailure = { state, error ->
            // El servidor vuelve a validar el código: si venció o se agotó, hay que escribir uno nuevo.
            if (error.isOtpCodeError) state.copy(step = RecoveryStep.OTP_VERIFICATION, otpCode = "") else state
        },
        effect = PasswordRecoveryEffect.NavigateToSuccess
    )

    /**
     * Ejecuta un paso del flujo con el manejo común de carga y errores.
     *
     * @param action Operación del caso de uso, que recibe el estado vigente.
     * @param onSuccess Transformación del estado si la operación tiene éxito.
     * @param onFailure Transformación opcional del estado según el error, además de mostrarlo.
     * @param effect Efecto opcional a emitir tras el éxito.
     */
    private fun runStep(
        action: suspend (PasswordRecoveryUiState) -> Result<Unit>,
        onSuccess: (PasswordRecoveryUiState) -> PasswordRecoveryUiState,
        onFailure: (PasswordRecoveryUiState, AuthError) -> PasswordRecoveryUiState = { state, _ -> state },
        effect: PasswordRecoveryEffect? = null
    ) {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            action(state)
                .onSuccess {
                    _uiState.update { onSuccess(it).copy(isLoading = false) }
                    effect?.let { _effects.send(it) }
                }
                .onFailure { throwable ->
                    val error = throwable.toAuthError()
                    _uiState.update { onFailure(it, error).copy(isLoading = false, error = error) }
                }
        }
    }
}
