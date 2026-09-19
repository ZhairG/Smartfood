package com.smartfood.app.ui.auth

/**
 * Eventos y acciones que el usuario puede realizar en la pantalla de Autenticación.
 */
sealed interface AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent
    data class PasswordChanged(val password: String) : AuthEvent
    data class ConfirmPasswordChanged(val confirmPassword: String) : AuthEvent
    
    data object LoginClicked : AuthEvent
    data object RegisterClicked : AuthEvent
    data object RecoverPasswordClicked : AuthEvent
    data object RetryConnectionClicked : AuthEvent
    
    data object DismissError : AuthEvent
}
