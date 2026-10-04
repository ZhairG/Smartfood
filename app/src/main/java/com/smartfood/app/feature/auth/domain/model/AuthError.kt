package com.smartfood.app.feature.auth.domain.model

/**
 * Nombre: AuthError.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Errores de negocio tipados del módulo de autenticación (RF-AUTH-08).
 * Reemplazan la comparación de mensajes de texto: la capa de datos traduce cada falla técnica a un [AuthError]
 * y la UI decide qué texto mostrar.
 */

/** Campos de formulario que un error puede señalar. */
enum class AuthField { FULL_NAME, EMAIL, PASSWORD, OTP_CODE }

/** Clasificación semántica de las fallas del flujo de autenticación. */
sealed interface AuthError {
    /** Uno o más campos obligatorios están vacíos. */
    data class EmptyFields(val fields: Set<AuthField>) : AuthError

    /** El correo no tiene un formato válido. */
    data object InvalidEmail : AuthError

    /** La contraseña no cumple la política de seguridad. */
    data object WeakPassword : AuthError

    /** La contraseña y su confirmación no coinciden. */
    data object PasswordsDontMatch : AuthError

    /** El usuario no aceptó los Términos y condiciones. */
    data object TermsNotAccepted : AuthError

    /** Correo o contraseña incorrectos. */
    data object InvalidCredentials : AuthError

    /** No existe una cuenta con el correo indicado. */
    data object AccountNotFound : AuthError

    /** Ya existe una cuenta con el correo indicado. */
    data object EmailAlreadyInUse : AuthError

    /** El código de verificación es incorrecto o está incompleto. */
    data object InvalidOtpCode : AuthError

    /** No hay conexión a internet. */
    data object Network : AuthError

    /** El servicio de autenticación no está configurado en Firebase. */
    data object ServiceUnavailable : AuthError

    /** Error no clasificado. */
    data class Unknown(val cause: String?) : AuthError
}

/**
 * Excepción portadora de un [AuthError], usada como causa de un `Result.failure`.
 *
 * @property error Error de negocio que describe la falla.
 */
class AuthException(val error: AuthError) : Exception(error.toString())

/** Extrae el [AuthError] de una excepción; si no es un [AuthException], la clasifica como [AuthError.Unknown]. */
fun Throwable.toAuthError(): AuthError = (this as? AuthException)?.error ?: AuthError.Unknown(message)

/** Atajo para construir un `Result` fallido a partir de un [AuthError]. */
fun <T> authFailure(error: AuthError): Result<T> = Result.failure(AuthException(error))
