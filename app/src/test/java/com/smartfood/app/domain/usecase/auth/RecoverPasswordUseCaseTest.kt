package com.smartfood.app.domain.usecase.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: RecoverPasswordUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para el caso de uso [RecoverPasswordUseCase].
 */
class RecoverPasswordUseCaseTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var recoverPasswordUseCase: RecoverPasswordUseCase

    @Before
    fun prepararEntornoTest() {
        fakeRepository = FakeAuthRepository()
        recoverPasswordUseCase = RecoverPasswordUseCase(fakeRepository)
    }

    @Test
    fun `cuando el correo esta vacio, retorna fallo sin llamar al repositorio`() = runTest {
        val result = recoverPasswordUseCase.execute("")
        assertTrue(result.isFailure)
        assertEquals(0, fakeRepository.sendResetCallCount)
    }

    @Test
    fun `cuando se proporciona el correo, delega al repositorio`() = runTest {
        fakeRepository.sendResetResult = Result.success(Unit)

        val result = recoverPasswordUseCase.execute("user@test.com")

        assertTrue(result.isSuccess)
        assertEquals(1, fakeRepository.sendResetCallCount)
        assertEquals("user@test.com", fakeRepository.lastEmail)
    }
}
