package com.smartfood.app.feature.auth.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.functions.FirebaseFunctionsException.Code
import com.smartfood.app.feature.auth.data.mapper.FirebaseErrorMapper
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.testutil.authError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: FirebaseErrorMapperTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [FirebaseErrorMapper] (RF-AUTH-08, plan.md §6.1) y de la clasificación de los
 * errores de Cloud Functions (spec 003, plan §3), como tablas de casos (#105).
 */
class FirebaseErrorMapperTest {

    @Test
    fun `CA-06-01 CA-02-03 CA-02-04 CA-01-06 cada excepcion de Firebase se clasifica en su error`() {
        listOf(
            FirebaseNetworkException("sin red") to AuthError.Network,
            FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "clave incorrecta") to AuthError.InvalidCredentials,
            FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no existe") to AuthError.AccountNotFound,
            FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "en uso") to AuthError.EmailAlreadyInUse,
            // La contraseña débil de Firebase no se confunde con credenciales inválidas.
            FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "debil", "corta") to AuthError.WeakPassword,
            FirebaseException("An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]") to AuthError.ServiceUnavailable,
            // Un AuthException conserva su error y cualquier otra excepción es Unknown con su mensaje.
            AuthException(AuthError.InvalidOtpCode) to AuthError.InvalidOtpCode,
            IllegalStateException("boom") to AuthError.Unknown("boom")
        ).forEach { (exception, error) ->
            assertEquals(exception::class.simpleName, error, FirebaseErrorMapper.map(exception))
        }

        // mapFirebaseError convierte el fallo y conserva el éxito.
        assertEquals(AuthError.Network, Result.failure<Unit>(FirebaseNetworkException("sin red")).mapFirebaseError().authError)
        assertTrue(Result.success(1).mapFirebaseError().isSuccess)
    }

    @Test
    fun `CA-REC-01-04 CA-REC-02-04 CA-REC-03-01 CA-REC-03-02 CA-REC-03-03 CA-REC-04-02 CA-04-02 CA-06-01 cada error de las funciones se clasifica`() {
        listOf(
            functionsError(Code.INVALID_ARGUMENT, "INVALID_CODE") to AuthError.InvalidOtpCode,
            functionsError(Code.FAILED_PRECONDITION, "CODE_EXPIRED") to AuthError.OtpCodeExpired,
            functionsError(Code.RESOURCE_EXHAUSTED, "TOO_MANY_ATTEMPTS") to AuthError.TooManyAttempts,
            // TOO_MANY_REQUESTS conserva la espera informada por el servidor o usa 60 segundos.
            functionsError(Code.RESOURCE_EXHAUSTED, "TOO_MANY_REQUESTS", retryAfterSeconds = 2_232) to
                AuthError.TooManyRequests(2_232.seconds),
            functionsError(Code.RESOURCE_EXHAUSTED, "TOO_MANY_REQUESTS") to AuthError.TooManyRequests(60.seconds),
            functionsError(Code.INVALID_ARGUMENT, "WEAK_PASSWORD") to AuthError.WeakPassword,
            functionsError(Code.INVALID_ARGUMENT, "INVALID_EMAIL") to AuthError.InvalidEmail,
            // Una función inalcanzable es un error de red; una no desplegada, servicio no disponible.
            functionsError(Code.INTERNAL, reason = null, isNetworkFailure = true) to AuthError.Network,
            functionsError(Code.UNAVAILABLE, reason = null) to AuthError.Network,
            functionsError(Code.DEADLINE_EXCEEDED, reason = null) to AuthError.Network,
            functionsError(Code.NOT_FOUND, reason = null) to AuthError.ServiceUnavailable,
            functionsError(Code.INTERNAL, reason = null) to AuthError.Unknown("INTERNAL")
        ).forEachIndexed { caso, (obtenido, esperado) -> assertEquals("caso $caso", esperado, obtenido) }
    }

    private fun functionsError(
        code: Code,
        reason: String?,
        retryAfterSeconds: Long? = null,
        isNetworkFailure: Boolean = false
    ): AuthError = FirebaseErrorMapper.mapFunctionsError(code, reason, retryAfterSeconds, isNetworkFailure)
}
