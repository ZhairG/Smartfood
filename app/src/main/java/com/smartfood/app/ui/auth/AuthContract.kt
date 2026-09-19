package com.smartfood.app.ui.auth

/**
 * Estado general de la UI para Autenticación.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordValid: Boolean = false,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Eventos emitidos desde la interfaz hacia el ViewModel.
 */
sealed interface AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent
    data class PasswordChanged(val password: String) : AuthEvent
    data object LoginClicked : AuthEvent
    data object RegisterClicked : AuthEvent
    data object DismissError : AuthEvent
}
