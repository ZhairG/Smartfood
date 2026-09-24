package com.smartfood.app.ui.auth

/**
 * Nombre: AuthEvent.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Eventos y acciones que el usuario puede realizar en la pantalla de Autenticación.
 * Centralizar los eventos evita la inyección de múltiples lambdas en los componentes Compose.
 */
sealed interface AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent
    data class PasswordChanged(val password: String) : AuthEvent
    data class ConfirmPasswordChanged(val confirmPassword: String) : AuthEvent
    data class RememberMeChanged(val isChecked: Boolean) : AuthEvent // Elevación de estado
    
    data object LoginClicked : AuthEvent
    data object RegisterClicked : AuthEvent
    data object RecoverPasswordClicked : AuthEvent
    data object RetryConnectionClicked : AuthEvent
    
    data object DismissError : AuthEvent
}
