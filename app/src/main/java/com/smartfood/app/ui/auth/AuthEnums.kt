package com.smartfood.app.ui.auth

/**
 * Nombre: AuthEnums.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Enums centralizados para la capa de autenticación, previniendo
 * validaciones frágiles de Strings (Evita Hardcoding).
 */

/** Determina el tipo de error que ocurrió durante el flujo de sesión. */
enum class AuthErrorType {
    NETWORK_ERROR,
    VALIDATION_ERROR,
    CREDENTIALS_ERROR,
    EMPTY_FIELDS,   // Login con correo y/o contraseña vacíos (validación local)
    INVALID_EMAIL,  // Login con un correo con formato inválido (validación local)
    UNKNOWN_ERROR
}
