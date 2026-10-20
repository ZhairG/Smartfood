package com.smartfood.app.feature.profile.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Nombre: ChangePasswordUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias para [ChangePasswordUseCase] (CA-PER-05-01 a CA-PER-05-06).
 */
class ChangePasswordUseCaseTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var validatePasswordUseCase: ValidatePasswordUseCase
    private lateinit var useCase: ChangePasswordUseCase

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        validatePasswordUseCase = ValidatePasswordUseCase()
        useCase = ChangePasswordUseCase(fakeAuthRepository, validatePasswordUseCase)
    }

    @Test
    fun `CA-PER-05-04 cambio exitoso reautentica y actualiza la contraseña`() = runTest {
        val result = useCase("Password123!", "NewPass123!", "NewPass123!")

        assertTrue(result.isSuccess)
        assertEquals("Password123!", fakeAuthRepository.lastReauthPassword)
        assertEquals("NewPass123!", fakeAuthRepository.lastUpdatedPassword)
    }

    @Test
    fun `CA-PER-05-03 campo vacio retorna EmptyFields`() = runTest {
        val result = useCase("", "NewPass123!", "NewPass123!")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.toAuthError() is AuthError.EmptyFields)
    }

    @Test
    fun `CA-PER-05-03 contraseñas no coincidentes retorna PasswordsDontMatch`() = runTest {
        val result = useCase("Password123!", "NewPass123!", "Diferente123!")

        assertTrue(result.isFailure)
        assertEquals(AuthError.PasswordsDontMatch, result.exceptionOrNull()?.toAuthError())
    }

    @Test
    fun `CA-PER-05-03 contraseña debil retorna WeakPassword`() = runTest {
        val result = useCase("Password123!", "debil", "debil")

        assertTrue(result.isFailure)
        assertEquals(AuthError.WeakPassword, result.exceptionOrNull()?.toAuthError())
    }

    @Test
    fun `CA-PER-05-05 contraseña actual incorrecta falla en reautenticacion`() = runTest {
        fakeAuthRepository.reauthenticateResult = authFailure(AuthError.InvalidCredentials)

        val result = useCase("WrongPassword!", "NewPass123!", "NewPass123!")

        assertTrue(result.isFailure)
        assertEquals(AuthError.InvalidCredentials, result.exceptionOrNull()?.toAuthError())
    }
}
