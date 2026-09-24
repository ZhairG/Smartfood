package com.smartfood.app.ui.auth

/**
 * Nombre: AuthEvent.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Contrato de eventos de usuario para la pantalla de Autenticación.
 * Centraliza las acciones para desacoplar la UI de la lógica de negocio mediante UDF.
 */
sealed interface AuthEvent {
    /** Cambio en el campo de texto de correo electrónico. */
    data class EmailChanged(val email: String) : AuthEvent

    /** Cambio en el campo de texto de contraseña. */
    data class PasswordChanged(val password: String) : AuthEvent

    /** Cambio en el campo de confirmación de contraseña. */
    data class ConfirmPasswordChanged(val confirmPassword: String) : AuthEvent

    /** Cambio en la opción "Recordarme". */
    data class RememberMeChanged(val isChecked: Boolean) : AuthEvent

    /** Clic en el botón principal de Iniciar Sesión. */
    data object LoginClicked : AuthEvent

    /** Clic en el botón o enlace de Registro. */
    data object RegisterClicked : AuthEvent

    /** Clic en el enlace de Recuperación de Contraseña. */
    data object RecoverPasswordClicked : AuthEvent

    /** Clic en el botón de reintento de conexión desde el diálogo modal. */
    data object RetryConnectionClicked : AuthEvent

    /** Clic para descartar el mensaje de error visible. */
    data object DismissError : AuthEvent
}
