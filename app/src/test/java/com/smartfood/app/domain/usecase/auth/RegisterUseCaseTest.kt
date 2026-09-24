package com.smartfood.app.domain.usecase.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: RegisterUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para el caso de uso [RegisterUseCase].
 */
class RegisterUseCaseTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var validatePasswordUseCase: ValidatePasswordUseCase
    private lateinit var registerUseCase: RegisterUseCase

    @Before
    fun prepararEntornoTest() {
        fakeRepository = FakeAuthRepository()
        validatePasswordUseCase = ValidatePasswordUseCase()
        registerUseCase = RegisterUseCase(fakeRepository, validatePasswordUseCase)
    }

    @Test
    fun `cuando el correo esta vacio, retorna fallo`() = runTest {
        val result = registerUseCase.execute("", "SecurePass#1", "SecurePass#1")
        assertTrue(result.isFailure)
        assertEquals(0, fakeRepository.registerCallCount)
    }

    @Test
    fun `cuando las contrasenas no coinciden, retorna fallo`() = runTest {
        val result = registerUseCase.execute("user@test.com", "SecurePass#1", "OtherPass#2")
        assertTrue(result.isFailure)
        assertEquals(0, fakeRepository.registerCallCount)
    }

    @Test
    fun `cuando la contrasena es debil, retorna fallo`() = runTest {
        val result = registerUseCase.execute("user@test.com", "weak", "weak")
        assertTrue(result.isFailure)
        assertEquals(0, fakeRepository.registerCallCount)
    }

    @Test
    fun `cuando los datos son validos, delega al repositorio`() = runTest {
        fakeRepository.registerResult = Result.success(Unit)

        val result = registerUseCase.execute("user@test.com", "SecurePass#1", "SecurePass#1")

        assertTrue(result.isSuccess)
        assertEquals(1, fakeRepository.registerCallCount)
        assertEquals("user@test.com", fakeRepository.lastEmail)
        assertEquals("SecurePass#1", fakeRepository.lastPassword)
    }
}
