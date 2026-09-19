package com.smartfood.app.ui.auth.register

/**
 * Estado de la UI para Registro (UDF - Unidirectional Data Flow).
 */
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Eventos que fluyen desde la vista hacia el ViewModel de Registro.
 */
sealed interface RegisterUiEvent {
    data class EmailChanged(val email: String) : RegisterUiEvent
    data class PasswordChanged(val password: String) : RegisterUiEvent
    data class ConfirmPasswordChanged(val confirmPass: String) : RegisterUiEvent
    data object SubmitRegister : RegisterUiEvent
    data object DismissError : RegisterUiEvent
}
