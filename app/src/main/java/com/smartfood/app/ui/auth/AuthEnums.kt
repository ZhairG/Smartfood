package com.smartfood.app.ui.auth

/**
 * Nombre: AuthEnums.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Enums centralizados para la capa de autenticación.
 * Evita validaciones basadas en cadenas de texto hardcodeadas.
 */

/**
 * Clasificación semántica de errores durante el flujo de autenticación.
 */
enum class AuthErrorType {
    /** Error por falta de conectividad a internet o falla de red. */
    NETWORK_ERROR,

    /** Error de validación en los datos ingresados por el usuario. */
    VALIDATION_ERROR,

    /** Error de credenciales incorrectas (correo o contraseña erróneos). */
    CREDENTIALS_ERROR,

    /** Error por campos vacíos en el formulario de inicio de sesión. */
    EMPTY_FIELDS,

    /** Error por correo electrónico con formato inválido. */
    INVALID_EMAIL,

    /** Error genérico o no clasificado. */
    UNKNOWN_ERROR
}
