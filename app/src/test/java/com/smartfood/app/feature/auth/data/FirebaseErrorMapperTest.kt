package com.smartfood.app.feature.auth.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.smartfood.app.feature.auth.data.mapper.FirebaseErrorMapper
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.testutil.authError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: FirebaseErrorMapperTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [FirebaseErrorMapper] (RF-AUTH-08, plan.md §6.1).
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
}
