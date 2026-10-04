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

    /** Error por campos vacíos en el formulario de inicio de sesión o registro. */
    EMPTY_FIELDS,

    /** Error por correo electrónico con formato inválido. */
    INVALID_EMAIL,

    /** Error por cuenta no existente en inicio de sesión. */
    ACCOUNT_NOT_FOUND,

    /** Error por correo no encontrado en recuperación de contraseña. */
    ACCOUNT_NOT_FOUND_RECOVERY,

    /** Error por correo vacío en recuperación de contraseña. */
    EMPTY_EMAIL,

    /** Error por código OTP vacío en verificación. */
    EMPTY_OTP_CODE,

    /** Error por código OTP incorrecto en verificación. */
    INVALID_OTP_CODE,

    /** Error por cuenta existente previamente en registro. */
    ACCOUNT_ALREADY_EXISTS,

    /** Error genérico o no clasificado. */
    UNKNOWN_ERROR
}

/**
 * Pasos del flujo de recuperación de contraseña ("Indicador de pasos").
 */
enum class RecoveryStep(val stepIndex: Int) {
    /** Paso 1: Solicitud de código ingresando correo electrónico. */
    EMAIL_REQUEST(0),

    /** Paso 2: Verificación de código de 4 dígitos. */
    OTP_VERIFICATION(1),

    /** Paso 3: Definición y confirmación de nueva contraseña. */
    NEW_PASSWORD(2)
}
