package com.smartfood.app.feature.auth.ui.login

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.usecase.LoginUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidateEmailUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeSessionPreferences
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> LoginViewModelTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias de [LoginViewModel] (HU-02 y HU-06).<br>
 * </build>
 */
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun prepararEntornoTest() {
        authRepository = FakeAuthRepository()
        viewModel = LoginViewModel(LoginUseCase(authRepository, FakeSessionPreferences(), ValidateEmailUseCase()))
    }

    private fun escribirCredenciales(email: String = "pepito@correo.com", password: String = "Secreta#1") {
        viewModel.onEvent(LoginEvent.EmailChanged(email))
        viewModel.onEvent(LoginEvent.PasswordChanged(password))
    }

    @Test
    fun `CA-02-01 con campos vacios muestra EmptyFields y marca ambos campos`() {
        viewModel.onEvent(LoginEvent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthError.EmptyFields(setOf(AuthField.EMAIL, AuthField.PASSWORD)), state.error)
        assertTrue(state.emailHasError)
        assertTrue(state.passwordHasError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `CA-02-02 con correo invalido marca solo el correo`() {
        escribirCredenciales(email = "pepito@")
        viewModel.onEvent(LoginEvent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthError.InvalidEmail, state.error)
        assertTrue(state.emailHasError)
        assertFalse(state.passwordHasError)
    }

    @Test
    fun `CA-02-03 con credenciales incorrectas marca ambos campos`() {
        authRepository.loginResult = authFailure(AuthError.InvalidCredentials)
        escribirCredenciales()
        viewModel.onEvent(LoginEvent.LoginClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthError.InvalidCredentials, state.error)
        assertTrue(state.emailHasError)
        assertTrue(state.passwordHasError)
    }

    @Test
    fun `CA-02-04 con cuenta inexistente muestra AccountNotFound`() {
        authRepository.loginResult = authFailure(AuthError.AccountNotFound)
        escribirCredenciales()
        viewModel.onEvent(LoginEvent.LoginClicked)

        assertEquals(AuthError.AccountNotFound, viewModel.uiState.value.error)
    }

    @Test
    fun `CA-02-05 con credenciales correctas emite NavigateToHome`() = runTest {
        escribirCredenciales()
        viewModel.onEvent(LoginEvent.LoginClicked)

        assertEquals(LoginEffect.NavigateToHome, viewModel.effects.first())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `CA-02-06 editar un campo limpia el error`() {
        viewModel.onEvent(LoginEvent.LoginClicked)
        viewModel.onEvent(LoginEvent.EmailChanged("p"))

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `CA-02-07 mientras carga muestra el estado de carga e ignora un segundo toque`() = runTest {
        authRepository.loginDelayMs = 1_000
        escribirCredenciales()

        viewModel.onEvent(LoginEvent.LoginClicked)
        viewModel.onEvent(LoginEvent.LoginClicked)

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, authRepository.loginCalls)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `CA-06-01 sin conexion muestra el dialogo de desconexion`() {
        authRepository.loginResult = authFailure(AuthError.Network)
        escribirCredenciales()
        viewModel.onEvent(LoginEvent.LoginClicked)

        assertTrue(viewModel.uiState.value.isOffline)
    }

    @Test
    fun `CA-06-02 Reconectar reintenta con los mismos datos`() = runTest {
        authRepository.loginResult = authFailure(AuthError.Network)
        escribirCredenciales()
        viewModel.onEvent(LoginEvent.LoginClicked)

        authRepository.loginResult = Result.success(Unit)
        viewModel.onEvent(LoginEvent.RetryConnectionClicked)

        assertEquals(2, authRepository.loginCalls)
        assertEquals("pepito@correo.com", authRepository.lastEmail)
        assertFalse(viewModel.uiState.value.isOffline)
        assertEquals(LoginEffect.NavigateToHome, viewModel.effects.first())
    }

    @Test
    fun `Recuerdame esta activo por defecto y se puede desactivar`() {
        assertTrue(viewModel.uiState.value.rememberMe)

        viewModel.onEvent(LoginEvent.RememberMeChanged(false))

        assertFalse(viewModel.uiState.value.rememberMe)
    }
}
