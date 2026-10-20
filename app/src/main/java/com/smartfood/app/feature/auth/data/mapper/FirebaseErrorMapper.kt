package com.smartfood.app.feature.auth.data.mapper

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: FirebaseErrorMapper.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Traduce las excepciones de Firebase a errores de negocio [AuthError] (RF-AUTH-08).
 * Es el único lugar del proyecto que conoce los tipos de excepción de Firebase (ver plan.md §6.1).
 * Los errores de Cloud Functions se clasifican por su `details.reason` (spec 003, plan §3).
 */
object FirebaseErrorMapper {

    /** Clasifica [throwable] en un [AuthError]. */
    fun map(throwable: Throwable): AuthError = when {
        throwable is AuthException -> throwable.error
        throwable is FirebaseNetworkException -> AuthError.Network
        // WeakPassword hereda de InvalidCredentials: debe evaluarse antes.
        throwable is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
        throwable is FirebaseAuthInvalidUserException -> AuthError.AccountNotFound
        throwable is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
        throwable is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
        throwable is FirebaseFirestoreException &&
            throwable.code == FirebaseFirestoreException.Code.UNAVAILABLE -> AuthError.Network
        // Va antes que FirebaseException, de la que hereda.
        throwable is FirebaseFunctionsException -> (throwable.details as? Map<*, *>).let { details ->
            mapFunctionsError(
                code = throwable.code,
                reason = details?.get(REASON_KEY) as? String,
                retryAfterSeconds = (details?.get(RETRY_AFTER_KEY) as? Number)?.toLong(),
                isNetworkFailure = throwable.cause is IOException
            )
        }
        throwable is FirebaseException &&
            throwable.message.orEmpty().contains(CONFIGURATION_NOT_FOUND) -> AuthError.ServiceUnavailable
        else -> AuthError.Unknown(throwable.message)
    }

    /**
     * Clasifica un error de las funciones de recuperación (spec 003, plan §3). Se separa de [map] para probarlo en
     * la JVM sin construir la excepción.
     *
     * @param code Código HTTPS de la función.
     * @param reason Razón de negocio enviada por el servidor en `details.reason`, si la hay.
     * @param retryAfterSeconds Espera informada en `details.retryAfterSeconds` con `TOO_MANY_REQUESTS`.
     * @param isNetworkFailure `true` si la llamada no llegó al servidor (el SDK lo reporta como `INTERNAL`).
     */
    internal fun mapFunctionsError(
        code: FirebaseFunctionsException.Code,
        reason: String?,
        retryAfterSeconds: Long?,
        isNetworkFailure: Boolean
    ): AuthError = when (reason) {
        "INVALID_EMAIL" -> AuthError.InvalidEmail
        "INVALID_CODE" -> AuthError.InvalidOtpCode
        "WEAK_PASSWORD" -> AuthError.WeakPassword
        "CODE_EXPIRED" -> AuthError.OtpCodeExpired
        "TOO_MANY_ATTEMPTS" -> AuthError.TooManyAttempts
        "TOO_MANY_REQUESTS" -> AuthError.TooManyRequests((retryAfterSeconds ?: DEFAULT_RETRY_AFTER_SECONDS).seconds)
        // Cambio de correo con código (spec 008, CA-PER-02-11).
        "EMAIL_IN_USE" -> AuthError.EmailAlreadyInUse
        "SAME_EMAIL" -> AuthError.InvalidEmail
        "REQUIRES_RECENT_LOGIN" -> AuthError.InvalidCredentials
        else -> when {
            isNetworkFailure -> AuthError.Network
            code == FirebaseFunctionsException.Code.UNAVAILABLE ||
                code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AuthError.Network
            // La función no está desplegada en el proyecto.
            code == FirebaseFunctionsException.Code.NOT_FOUND -> AuthError.ServiceUnavailable
            else -> AuthError.Unknown(code.name)
        }
    }

    /** Envuelve [throwable] en un [AuthException] con el error ya clasificado. */
    fun toAuthException(throwable: Throwable): AuthException =
        throwable as? AuthException ?: AuthException(map(throwable))

    private const val CONFIGURATION_NOT_FOUND = "CONFIGURATION_NOT_FOUND"
    private const val REASON_KEY = "reason"
    private const val RETRY_AFTER_KEY = "retryAfterSeconds"

    /** Espera si el servidor no la informa: la de 60 s entre envíos. */
    private const val DEFAULT_RETRY_AFTER_SECONDS = 60L
}

/** Convierte cualquier fallo de este [Result] en un [AuthException] clasificado. */
fun <T> Result<T>.mapFirebaseError(): Result<T> =
    exceptionOrNull()?.let { Result.failure(FirebaseErrorMapper.toAuthException(it)) } ?: this
