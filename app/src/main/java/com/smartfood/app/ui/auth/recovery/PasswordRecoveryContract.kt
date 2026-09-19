package com.smartfood.app.ui.auth.recovery

/**
 * Estado de la UI para Recuperación de contraseña (UDF).
 */
data class PasswordRecoveryUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Eventos que fluyen desde la vista hacia el ViewModel de Recuperación.
 */
sealed interface PasswordRecoveryUiEvent {
    data class EmailChanged(val email: String) : PasswordRecoveryUiEvent
    data object SubmitRecovery : PasswordRecoveryUiEvent
    data object DismissError : PasswordRecoveryUiEvent
}
