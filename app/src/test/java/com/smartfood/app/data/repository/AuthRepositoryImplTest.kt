package com.smartfood.app.data.repository

import com.smartfood.app.data.source.BrevoEmailDataSource
import com.smartfood.app.data.source.FirebaseAuthDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: AuthRepositoryImplTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para la implementación del repositorio [AuthRepositoryImpl].
 */
class AuthRepositoryImplTest {

    private lateinit var fakeDataSource: FakeFirebaseAuthDataSource
    private lateinit var fakeBrevoDataSource: FakeBrevoEmailDataSource
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun prepararEntornoTest() {
        fakeDataSource = FakeFirebaseAuthDataSource()
        fakeBrevoDataSource = FakeBrevoEmailDataSource()
        repository = AuthRepositoryImpl(fakeDataSource, fakeBrevoDataSource)
    }

    @Test
    fun `esUsuarioAutenticado retorna falso cuando el usuario actual es nulo`() {
        assertFalse(repository.isUserAuthenticated())
    }

    @Test
    fun `inicio de sesion exitoso retorna Result de exito`() = runTest {
        val result = repository.login("test@correo.com", "password123")
        assertTrue(result.isSuccess)
        assertEquals("test@correo.com", fakeDataSource.lastEmail)
    }

    @Test
    fun `fallo de inicio de sesion retorna Result de fallo de forma segura`() = runTest {
        fakeDataSource.shouldFail = true
        val result = repository.login("test@correo.com", "wrong")
        assertTrue(result.isFailure)
        assertEquals("Error simulado", result.exceptionOrNull()?.message)
    }

    @Test
    fun `envio de codigo de recuperacion via Brevo genera OTP y retorna exito`() = runTest {
        val result = repository.sendPasswordReset("usuario@correo.com")
        assertTrue(result.isSuccess)
        assertEquals("usuario@correo.com", fakeBrevoDataSource.lastEmail)
    }
}

/**
 * Fake de [FirebaseAuthDataSource] para probar el comportamiento de [AuthRepositoryImpl] sin dependencias nativas.
 */
class FakeFirebaseAuthDataSource : FirebaseAuthDataSource(null, null) {
    var shouldFail = false
    var lastEmail: String? = null
    var lastPass: String? = null

    override val currentUser get() = null

    override suspend fun signInWithEmailAndPassword(email: String, pass: String) {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastEmail = email
        lastPass = pass
    }

    override suspend fun createUserWithEmailAndPassword(email: String, pass: String) {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastEmail = email
        lastPass = pass
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastEmail = email
    }

    override suspend fun sendOtpEmailViaSendGrid(email: String): String {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastEmail = email
        return "4422"
    }

    override suspend fun confirmPasswordReset(code: String, newPass: String) {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastPass = newPass
    }

    override suspend fun verifyPasswordResetCode(code: String): String? {
        if (shouldFail) throw RuntimeException("Error simulado")
        return lastEmail
    }

    override suspend fun updatePassword(newPass: String) {
        if (shouldFail) throw RuntimeException("Error simulado")
        lastPass = newPass
    }

    override fun signOut() {
        // No-op para pruebas
    }
}

/**
 * Fake de [BrevoEmailDataSource] para aislar las pruebas de la API de correo Brevo.
 */
class FakeBrevoEmailDataSource : BrevoEmailDataSource(null) {
    var shouldFail = false
    var lastEmail: String? = null
    var lastOtp: String? = null

    override suspend fun sendOtpEmail(email: String, otpCode: String): Boolean {
        if (shouldFail) return false
        lastEmail = email
        lastOtp = otpCode
        return true
    }
}
