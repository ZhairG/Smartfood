package com.smartfood.app.ui.auth

import com.smartfood.app.domain.usecase.auth.FakeAuthRepository
import com.smartfood.app.domain.usecase.auth.LoginUseCase
import com.smartfood.app.domain.usecase.auth.RecoverPasswordUseCase
import com.smartfood.app.domain.usecase.auth.RegisterUseCase
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
        val recoverPasswordUseCase = RecoverPasswordUseCase(fakeRepository)

        viewModel = AuthViewModel(
            loginUseCase = loginUseCase,
            registerUseCase = registerUseCase,
            recoverPasswordUseCase = recoverPasswordUseCase,
            validatePasswordUseCase = validatePasswordUseCase,
            validateEmailUseCase = validateEmailUseCase
        )
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
    fun `cuando se presiona LoginClicked con credenciales validas, establece estado de exito`() = runTest {
        fakeRepository.loginResult = Result.success(Unit)

        viewModel.onEvent(AuthEvent.EmailChanged("user@test.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("SecurePass#1"))
        viewModel.onEvent(AuthEvent.LoginClicked)

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.errorType)
    }

    @Test
    fun `cuando el inicio de sesion falla por credenciales, establece CREDENTIALS_ERROR`() = runTest {
        fakeRepository.loginResult = Result.failure(RuntimeException("Invalid credentials"))

        viewModel.onEvent(AuthEvent.EmailChanged("user@test.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("SecurePass#1"))
        viewModel.onEvent(AuthEvent.LoginClicked)

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertEquals(AuthErrorType.CREDENTIALS_ERROR, state.errorType)
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
