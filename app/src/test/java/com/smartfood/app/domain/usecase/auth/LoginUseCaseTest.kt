package com.smartfood.app.domain.usecase.auth

import com.smartfood.app.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: LoginUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para el caso de uso [LoginUseCase].
 */
class LoginUseCaseTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var loginUseCase: LoginUseCase

    @Before
    fun prepararEntornoTest() {
        fakeRepository = FakeAuthRepository()
        loginUseCase = LoginUseCase(fakeRepository)
    }

    @Test
    fun `cuando el correo o la contrasena estan vacios, retorna fallo sin llamar al repositorio`() = runTest {
        val resultEmptyEmail = loginUseCase.execute("", "password123")
        assertTrue(resultEmptyEmail.isFailure)

        val resultEmptyPass = loginUseCase.execute("user@test.com", "")
        assertTrue(resultEmptyPass.isFailure)

        assertEquals(0, fakeRepository.loginCallCount)
    }

    @Test
    fun `cuando los parametros son validos, delega la llamada al repositorio`() = runTest {
        fakeRepository.loginResult = Result.success(Unit)

        val result = loginUseCase.execute("user@test.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals(1, fakeRepository.loginCallCount)
        assertEquals("user@test.com", fakeRepository.lastEmail)
        assertEquals("password123", fakeRepository.lastPassword)
    }
}

/**
 * Implementación Fake de [AuthRepository] para aislar las pruebas de la capa de datos.
 */
class FakeAuthRepository : AuthRepository {
    var isAuth = false
    var loginResult: Result<Unit> = Result.success(Unit)
    var registerResult: Result<Unit> = Result.success(Unit)
    var sendResetResult: Result<Unit> = Result.success(Unit)

    var loginCallCount = 0
    var registerCallCount = 0
    var sendResetCallCount = 0

    var lastEmail: String? = null
    var lastPassword: String? = null

    override fun isUserAuthenticated(): Boolean = isAuth

    override suspend fun login(email: String, pass: String): Result<Unit> {
        loginCallCount++
        lastEmail = email
        lastPassword = pass
        return loginResult
    }

    override suspend fun register(email: String, pass: String): Result<Unit> {
        registerCallCount++
        lastEmail = email
        lastPassword = pass
        return registerResult
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        sendResetCallCount++
        lastEmail = email
        return sendResetResult
    }

    override fun logout() {
        isAuth = false
    }
}
