package com.smartfood.app.feature.auth.domain.usecase

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.User
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeAuthRepository.Companion.DEFAULT_UID
import com.smartfood.app.testutil.FakeUserRepository
import com.smartfood.app.testutil.authError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> RegisterUseCaseTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias de [RegisterUseCase] (HU-01), incluido el rollback de CA-01-09.<br>
 * </build>
 */
class RegisterUseCaseTest {

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var userRepository: FakeUserRepository
    private lateinit var register: RegisterUseCase

    @Before
    fun prepararEntornoTest() {
        authRepository = FakeAuthRepository()
        userRepository = FakeUserRepository()
        register = RegisterUseCase(
            authRepository = authRepository,
            userRepository = userRepository,
            validateEmailUseCase = ValidateEmailUseCase(),
            validatePasswordUseCase = ValidatePasswordUseCase()
        )
    }

    private suspend fun registrarValido() = register(
        fullName = " Pepito Perez ",
        email = "pepito@correo.com",
        password = "Secreta#1",
        acceptedTerms = true
    )

    @Test
    fun `CA-01-01 con campos vacios falla con EmptyFields y no crea la cuenta`() = runTest {
        val result = register("", "", "", acceptedTerms = true)

        assertEquals(
            AuthError.EmptyFields(setOf(AuthField.FULL_NAME, AuthField.EMAIL, AuthField.PASSWORD)),
            result.authError
        )
        assertEquals(0, authRepository.createAccountCalls)
    }

    @Test
    fun `CA-01-02 con correo mal formado falla con InvalidEmail`() = runTest {
        val result = register("Pepito", "pepito@", "Secreta#1", acceptedTerms = true)

        assertEquals(AuthError.InvalidEmail, result.authError)
    }

    @Test
    fun `CA-01-04 con contrasena debil falla con WeakPassword`() = runTest {
        val result = register("Pepito", "pepito@correo.com", "debil", acceptedTerms = true)

        assertEquals(AuthError.WeakPassword, result.authError)
        assertEquals(0, authRepository.createAccountCalls)
    }

    @Test
    fun `CA-01-05 sin aceptar terminos falla con TermsNotAccepted`() = runTest {
        val result = register("Pepito", "pepito@correo.com", "Secreta#1", acceptedTerms = false)

        assertEquals(AuthError.TermsNotAccepted, result.authError)
        assertEquals(0, authRepository.createAccountCalls)
    }

    @Test
    fun `CA-01-06 propaga EmailAlreadyInUse del repositorio`() = runTest {
        authRepository.createAccountResult = authFailure(AuthError.EmailAlreadyInUse)

        val result = registrarValido()

        assertEquals(AuthError.EmailAlreadyInUse, result.authError)
        assertTrue(userRepository.profiles.isEmpty())
    }

    @Test
    fun `CA-01-07 crea la cuenta, guarda el perfil, envia verificacion y cierra la sesion`() = runTest {
        val result = registrarValido()

        assertTrue(result.isSuccess)
        assertEquals(User(DEFAULT_UID, "Pepito Perez", "pepito@correo.com"), userRepository.profiles[DEFAULT_UID])
        assertEquals(1, authRepository.sendVerificationCalls)
        assertNull(authRepository.currentUserId())
    }

    @Test
    fun `CA-01-07 si falla el envio de verificacion el registro igual es exitoso`() = runTest {
        authRepository.sendVerificationResult = authFailure(AuthError.Network)

        val result = registrarValido()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `CA-01-09 si falla el guardado del perfil elimina la cuenta y devuelve el error`() = runTest {
        userRepository.createProfileResult = authFailure(AuthError.Network)

        val result = registrarValido()

        assertEquals(AuthError.Network, result.authError)
        assertEquals(1, authRepository.deleteAccountCalls)
        assertEquals(0, authRepository.sendVerificationCalls)
        assertNull(authRepository.currentUserId())
    }
}
