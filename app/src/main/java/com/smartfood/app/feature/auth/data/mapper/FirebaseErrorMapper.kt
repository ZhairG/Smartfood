package com.smartfood.app.feature.auth.data.mapper

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException

/**
 * Nombre: FirebaseErrorMapper.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Traduce las excepciones de Firebase a errores de negocio [AuthError] (RF-AUTH-08).
 * Es el único lugar del proyecto que conoce los tipos de excepción de Firebase (ver plan.md §6.1).
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
        throwable is FirebaseException &&
            throwable.message.orEmpty().contains(CONFIGURATION_NOT_FOUND) -> AuthError.ServiceUnavailable
        else -> AuthError.Unknown(throwable.message)
    }

    /** Envuelve [throwable] en un [AuthException] con el error ya clasificado. */
    fun toAuthException(throwable: Throwable): AuthException =
        throwable as? AuthException ?: AuthException(map(throwable))

    private const val CONFIGURATION_NOT_FOUND = "CONFIGURATION_NOT_FOUND"
}

/** Convierte cualquier fallo de este [Result] en un [AuthException] clasificado. */
fun <T> Result<T>.mapFirebaseError(): Result<T> =
    exceptionOrNull()?.let { Result.failure(FirebaseErrorMapper.toAuthException(it)) } ?: this
