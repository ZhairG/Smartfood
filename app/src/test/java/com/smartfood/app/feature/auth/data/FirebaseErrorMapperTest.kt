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
 * errores de Cloud Functions (spec 003, plan §3).
 */
class FirebaseErrorMapperTest {

    @Test
    fun `CA-06-01 error de red de Firebase se clasifica como Network`() {
        assertEquals(AuthError.Network, FirebaseErrorMapper.map(FirebaseNetworkException("sin red")))
    }

    @Test
    fun `CA-02-03 credenciales invalidas se clasifican como InvalidCredentials`() {
        val exception = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "clave incorrecta")

        assertEquals(AuthError.InvalidCredentials, FirebaseErrorMapper.map(exception))
    }

    @Test
    fun `CA-02-04 usuario inexistente se clasifica como AccountNotFound`() {
        val exception = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no existe")

        assertEquals(AuthError.AccountNotFound, FirebaseErrorMapper.map(exception))
    }

    @Test
    fun `CA-01-06 correo en uso se clasifica como EmailAlreadyInUse`() {
        val exception = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "en uso")

        assertEquals(AuthError.EmailAlreadyInUse, FirebaseErrorMapper.map(exception))
    }

    @Test
    fun `contrasena debil de Firebase se clasifica como WeakPassword y no como credenciales`() {
        val exception = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "debil", "corta")

        assertEquals(AuthError.WeakPassword, FirebaseErrorMapper.map(exception))
    }

    @Test
    fun `configuracion faltante se clasifica como ServiceUnavailable`() {
        val exception = FirebaseException("An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]")

        assertEquals(AuthError.ServiceUnavailable, FirebaseErrorMapper.map(exception))
    }

    @Test
    fun `un AuthException conserva su error`() {
        assertEquals(AuthError.InvalidOtpCode, FirebaseErrorMapper.map(AuthException(AuthError.InvalidOtpCode)))
    }

    @Test
    fun `cualquier otra excepcion se clasifica como Unknown con su mensaje`() {
        assertEquals(AuthError.Unknown("boom"), FirebaseErrorMapper.map(IllegalStateException("boom")))
    }

    @Test
    fun `mapFirebaseError convierte el fallo y conserva el exito`() {
        val failure = Result.failure<Unit>(FirebaseNetworkException("sin red")).mapFirebaseError()

        assertEquals(AuthError.Network, failure.authError)
        assertTrue(Result.success(1).mapFirebaseError().isSuccess)
    }

    @Test
    fun `CA-REC-01-04 la razon INVALID_CODE se clasifica como InvalidOtpCode`() {
        assertEquals(AuthError.InvalidOtpCode, functionsError(Code.INVALID_ARGUMENT, "INVALID_CODE"))
    }

    @Test
    fun `CA-REC-03-01 la razon CODE_EXPIRED se clasifica como OtpCodeExpired`() {
        assertEquals(AuthError.OtpCodeExpired, functionsError(Code.FAILED_PRECONDITION, "CODE_EXPIRED"))
    }

    @Test
    fun `CA-REC-03-02 la razon TOO_MANY_ATTEMPTS se clasifica como TooManyAttempts`() {
        assertEquals(AuthError.TooManyAttempts, functionsError(Code.RESOURCE_EXHAUSTED, "TOO_MANY_ATTEMPTS"))
    }

    @Test
    fun `CA-REC-04-02 la razon TOO_MANY_REQUESTS conserva la espera informada por el servidor`() {
        assertEquals(
            AuthError.TooManyRequests(2_232.seconds),
            functionsError(Code.RESOURCE_EXHAUSTED, "TOO_MANY_REQUESTS", retryAfterSeconds = 2_232)
        )
    }

    @Test
    fun `CA-REC-03-03 TOO_MANY_REQUESTS sin espera informada usa 60 segundos`() {
        assertEquals(AuthError.TooManyRequests(60.seconds), functionsError(Code.RESOURCE_EXHAUSTED, "TOO_MANY_REQUESTS"))
    }

    @Test
    fun `CA-REC-02-04 la razon WEAK_PASSWORD se clasifica como WeakPassword`() {
        assertEquals(AuthError.WeakPassword, functionsError(Code.INVALID_ARGUMENT, "WEAK_PASSWORD"))
    }

    @Test
    fun `CA-04-02 la razon INVALID_EMAIL se clasifica como InvalidEmail`() {
        assertEquals(AuthError.InvalidEmail, functionsError(Code.INVALID_ARGUMENT, "INVALID_EMAIL"))
    }

    @Test
    fun `CA-06-01 una funcion inalcanzable se clasifica como Network`() {
        assertEquals(AuthError.Network, functionsError(Code.INTERNAL, reason = null, isNetworkFailure = true))
        assertEquals(AuthError.Network, functionsError(Code.UNAVAILABLE, reason = null))
        assertEquals(AuthError.Network, functionsError(Code.DEADLINE_EXCEEDED, reason = null))
    }

    @Test
    fun `una funcion no desplegada se clasifica como ServiceUnavailable`() {
        assertEquals(AuthError.ServiceUnavailable, functionsError(Code.NOT_FOUND, reason = null))
    }

    @Test
    fun `un error interno sin razon se clasifica como Unknown`() {
        assertEquals(AuthError.Unknown("INTERNAL"), functionsError(Code.INTERNAL, reason = null))
    }

    private fun functionsError(
        code: Code,
        reason: String?,
        retryAfterSeconds: Long? = null,
        isNetworkFailure: Boolean = false
    ): AuthError = FirebaseErrorMapper.mapFunctionsError(code, reason, retryAfterSeconds, isNetworkFailure)
}
