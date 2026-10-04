package com.smartfood.app.ui.auth

import com.smartfood.app.domain.usecase.auth.FakeAuthRepository
import com.smartfood.app.domain.usecase.auth.LoginUseCase
import com.smartfood.app.domain.usecase.auth.RegisterUseCase
import com.smartfood.app.domain.usecase.auth.ResetPasswordUseCase
import com.smartfood.app.domain.usecase.auth.ValidateEmailUseCase
import com.smartfood.app.domain.usecase.auth.ValidatePasswordUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Nombre: AuthViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas unitarias para verificar el comportamiento reactivo de [AuthViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun prepararEntornoTest() {
        fakeRepository = FakeAuthRepository()
        val validateEmailUseCase = ValidateEmailUseCase()
        val validatePasswordUseCase = ValidatePasswordUseCase()
        val loginUseCase = LoginUseCase(fakeRepository)
        val registerUseCase = RegisterUseCase(fakeRepository, validatePasswordUseCase)
        val resetPasswordUseCase = ResetPasswordUseCase(
            authRepository = fakeRepository,
            validateEmailUseCase = validateEmailUseCase,
            validatePasswordUseCase = validatePasswordUseCase
        )

        viewModel = AuthViewModel(
            loginUseCase = loginUseCase,
            registerUseCase = registerUseCase,
            resetPasswordUseCase = resetPasswordUseCase,
            validatePasswordUseCase = validatePasswordUseCase,
            validateEmailUseCase = validateEmailUseCase
        )
    }

    @Test
    fun `cuando se envia evento FullNameChanged, el estado se actualiza`() {
        viewModel.onEvent(AuthEvent.FullNameChanged("Pepito Perez"))

        val state = viewModel.uiState.value
        assertEquals("Pepito Perez", state.fullName)
        assertFalse(state.fullNameHasError)
    }

    @Test
    fun `cuando se envia evento EmailChanged, el estado se actualiza y se limpian errores`() {
        viewModel.onEvent(AuthEvent.EmailChanged("test@correo.com"))

        val state = viewModel.uiState.value
        assertEquals("test@correo.com", state.email)
        assertFalse(state.emailHasError)
        assertNull(state.errorType)
    }

    @Test
    fun `cuando se envia evento PasswordChanged, las validaciones de contrasena se actualizan`() {
        viewModel.onEvent(AuthEvent.PasswordChanged("SecurePass#1"))

        val state = viewModel.uiState.value
        assertEquals("SecurePass#1", state.password)
        assertTrue(state.hasMinimumLength)
        assertTrue(state.hasUppercase)
        assertTrue(state.hasSpecialCharacter)
    }

    @Test
    fun `cuando se presiona LoginClicked con campos vacios, establece error EMPTY_FIELDS`() {
        viewModel.onEvent(AuthEvent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthErrorType.EMPTY_FIELDS, state.errorType)
        assertTrue(state.emailHasError)
        assertTrue(state.passwordHasError)
    }

    @Test
    fun `cuando se presiona LoginClicked con correo invalido, establece error INVALID_EMAIL`() {
        viewModel.onEvent(AuthEvent.EmailChanged("invalid-email"))
        viewModel.onEvent(AuthEvent.PasswordChanged("12345678"))
        viewModel.onEvent(AuthEvent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthErrorType.INVALID_EMAIL, state.errorType)
        assertTrue(state.emailHasError)
    }

    @Test
    fun `cuando el inicio de sesion falla por cuenta no encontrada, establece ACCOUNT_NOT_FOUND`() = runTest {
        fakeRepository.loginResult = Result.failure(RuntimeException("The user-not-found error occurred"))

        viewModel.onEvent(AuthEvent.EmailChanged("notfound@test.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("SecurePass#1"))
        viewModel.onEvent(AuthEvent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthErrorType.ACCOUNT_NOT_FOUND, state.errorType)
    }

    @Test
    fun `cuando el registro falla por cuenta existente, establece ACCOUNT_ALREADY_EXISTS`() = runTest {
        fakeRepository.registerResult = Result.failure(RuntimeException("The email-already-in-use error occurred"))

        viewModel.onEvent(AuthEvent.FullNameChanged("Pepito Perez"))
        viewModel.onEvent(AuthEvent.EmailChanged("exists@test.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("SecurePass#1"))
        viewModel.onEvent(AuthEvent.RegisterClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthErrorType.ACCOUNT_ALREADY_EXISTS, state.errorType)
    }

    @Test
    fun `cuando se solicita codigo de recuperacion con correo valido, avanza al paso de verificacion OTP`() = runTest {
        viewModel.onEvent(AuthEvent.EmailChanged("usuario@correo.com"))
        viewModel.onEvent(AuthEvent.SendRecoveryCodeClicked)

        val state = viewModel.uiState.value
        assertEquals(RecoveryStep.OTP_VERIFICATION, state.recoveryStep)
        assertFalse(state.emailHasError)
    }

    @Test
    fun `cuando se solicita codigo de recuperacion con correo vacio, establece error EMPTY_EMAIL`() {
        viewModel.onEvent(AuthEvent.EmailChanged(""))
        viewModel.onEvent(AuthEvent.SendRecoveryCodeClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthErrorType.EMPTY_EMAIL, state.errorType)
        assertTrue(state.emailHasError)
    }

    @Test
    fun `cuando se ingresa codigo OTP valido de 4 digitos, avanza al paso de nueva contrasena`() {
        viewModel.onEvent(AuthEvent.EmailChanged("usuario@correo.com"))
        viewModel.onEvent(AuthEvent.SendRecoveryCodeClicked)

        viewModel.onEvent(AuthEvent.OtpCodeChanged("4422"))
        viewModel.onEvent(AuthEvent.VerifyOtpCodeClicked)

        val state = viewModel.uiState.value
        assertEquals(RecoveryStep.NEW_PASSWORD, state.recoveryStep)
        assertFalse(state.otpCodeHasError)
    }

    @Test
    fun `cuando se restablece contrasena cumpliendo requisitos, establece isPasswordResetSuccess`() = runTest {
        viewModel.onEvent(AuthEvent.EmailChanged("usuario@correo.com"))
        viewModel.onEvent(AuthEvent.SendRecoveryCodeClicked)
        viewModel.onEvent(AuthEvent.OtpCodeChanged("4422"))
        viewModel.onEvent(AuthEvent.VerifyOtpCodeClicked)

        viewModel.onEvent(AuthEvent.PasswordChanged("PpitoPrez24_"))
        viewModel.onEvent(AuthEvent.ConfirmPasswordChanged("PpitoPrez24_"))
        viewModel.onEvent(AuthEvent.ResetPasswordClicked)

        val state = viewModel.uiState.value
        assertTrue(state.isPasswordResetSuccess)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
