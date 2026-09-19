package com.smartfood.app.ui.auth.login

/**
 * Estado de la UI para Login (UDF - Unidirectional Data Flow).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Eventos emitidos desde los elementos UI hacia el ViewModel.
 */
sealed interface LoginUiEvent {
    data class EmailChanged(val email: String) : LoginUiEvent
    data class PasswordChanged(val password: String) : LoginUiEvent
    data object SubmitLogin : LoginUiEvent
    data object DismissError : LoginUiEvent
}
