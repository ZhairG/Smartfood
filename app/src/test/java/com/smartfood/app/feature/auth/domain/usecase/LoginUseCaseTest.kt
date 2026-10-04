package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeSessionPreferences
import com.smartfood.app.testutil.authError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: LoginUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [LoginUseCase] (HU-02 y HU-03).
 */
class LoginUseCaseTest {

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var sessionPreferences: FakeSessionPreferences
    private lateinit var login: LoginUseCase

    @Before
    fun prepararEntornoTest() {
        authRepository = FakeAuthRepository()
        sessionPreferences = FakeSessionPreferences()
        login = LoginUseCase(authRepository, sessionPreferences, ValidateEmailUseCase())
    }

    @Test
    fun `CA-02-01 con campos vacios falla con EmptyFields sin llamar al repositorio`() = runTest {
        val result = login("", "", rememberMe = true)

        assertEquals(AuthError.EmptyFields(setOf(AuthField.EMAIL, AuthField.PASSWORD)), result.authError)
        assertEquals(0, authRepository.loginCalls)
    }

    @Test
    fun `CA-02-01 solo marca el campo vacio`() = runTest {
        val result = login("user@test.com", "", rememberMe = true)

        assertEquals(AuthError.EmptyFields(setOf(AuthField.PASSWORD)), result.authError)
    }

    @Test
    fun `CA-02-02 con correo mal formado falla con InvalidEmail`() = runTest {
        val result = login("pepito@", "Secreta#1", rememberMe = true)

        assertEquals(AuthError.InvalidEmail, result.authError)
        assertEquals(0, authRepository.loginCalls)
    }

    @Test
    fun `CA-02-03 propaga el error de credenciales del repositorio`() = runTest {
        authRepository.loginResult = authFailure(AuthError.InvalidCredentials)

        val result = login("user@test.com", "incorrecta", rememberMe = true)

        assertEquals(AuthError.InvalidCredentials, result.authError)
    }

    @Test
    fun `CA-02-05 con datos validos inicia sesion con el correo sin espacios`() = runTest {
        val result = login("  user@test.com ", "Secreta#1", rememberMe = true)

        assertTrue(result.isSuccess)
        assertEquals("user@test.com", authRepository.lastEmail)
        assertEquals("Secreta#1", authRepository.lastPassword)
    }

    @Test
    fun `CA-03-02 guarda la preferencia Recuerdame tras un inicio exitoso`() = runTest {
        login("user@test.com", "Secreta#1", rememberMe = false)

        assertFalse(sessionPreferences.rememberMe)
    }

    @Test
    fun `si el inicio falla no modifica la preferencia Recuerdame`() = runTest {
        authRepository.loginResult = authFailure(AuthError.Network)

        login("user@test.com", "Secreta#1", rememberMe = false)

        assertTrue(sessionPreferences.rememberMe)
    }
}
