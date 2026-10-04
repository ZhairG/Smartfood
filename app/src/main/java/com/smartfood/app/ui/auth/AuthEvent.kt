package com.smartfood.app.ui.auth

/**
 * Nombre: AuthEvent.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Contrato de eventos de usuario para las pantallas de Autenticación (Login y Registro).
 * Centraliza las acciones para desacoplar la UI de la lógica de negocio mediante Unidirectional Data Flow (UDF).
 */
sealed interface AuthEvent {
    /** Cambio en el campo de texto de nombre completo. */
    data class FullNameChanged(val fullName: String) : AuthEvent

    /** Cambio en el campo de texto de correo electrónico. */
    data class EmailChanged(val email: String) : AuthEvent

    /** Cambio en el campo de texto de contraseña. */
    data class PasswordChanged(val password: String) : AuthEvent

    /** Cambio en el campo de confirmación de contraseña. */
    data class ConfirmPasswordChanged(val confirmPassword: String) : AuthEvent

    /** Cambio en la opción "Recordarme". */
    data class RememberMeChanged(val isChecked: Boolean) : AuthEvent

    /** Cambio en la casilla de aceptación de Términos y Condiciones. */
    data class AcceptedTermsChanged(val isChecked: Boolean) : AuthEvent

    /** Clic en el botón principal de Iniciar Sesión. */
    data object LoginClicked : AuthEvent

    /** Clic en el botón o enlace de Registro. */
    data object RegisterClicked : AuthEvent

    /** Clic en el botón de reintento de conexión desde el diálogo modal. */
    data object RetryConnectionClicked : AuthEvent

    /** Clic para descartar el mensaje de error visible. */
    data object DismissError : AuthEvent

    // --- Eventos de Recuperación de Contraseña ---
    /** Cambio en el código OTP de 4 dígitos. */
    data class OtpCodeChanged(val code: String) : AuthEvent

    /** Clic en "Enviar Código" (Paso 1). */
    data object SendRecoveryCodeClicked : AuthEvent

    /** Clic en "Verificar Código" (Paso 2). */
    data object VerifyOtpCodeClicked : AuthEvent

    /** Clic en "Reenviar código" (Paso 2). */
    data object ResendOtpCodeClicked : AuthEvent

    /** Clic en "¿Correo incorrecto? Haz click aqui para cambiarlo" (Paso 2). */
    data object ChangeEmailClicked : AuthEvent

    /** Clic en "Reestablecer Contraseña" (Paso 3). */
    data object ResetPasswordClicked : AuthEvent

    /** Reinicia el estado del flujo de recuperación de contraseña. */
    data object ResetRecoveryFlow : AuthEvent
}
