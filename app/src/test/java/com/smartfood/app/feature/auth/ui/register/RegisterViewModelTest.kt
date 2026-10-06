package com.smartfood.app.feature.auth.ui.register

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.usecase.RegisterUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidateEmailUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.FakeUserRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> RegisterViewModelTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias de [RegisterViewModel] (HU-01).<br>
 * </build>
 */
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun prepararEntornoTest() {
        authRepository = FakeAuthRepository()
        val validatePassword = ValidatePasswordUseCase()
        val register = RegisterUseCase(authRepository, FakeUserRepository(), ValidateEmailUseCase(), validatePassword)
        viewModel = RegisterViewModel(register, validatePassword)
    }

    private fun llenarFormulario(password: String = "Secreta#1", acceptedTerms: Boolean = true) {
        viewModel.onEvent(RegisterEvent.FullNameChanged("Pepito Perez"))
        viewModel.onEvent(RegisterEvent.EmailChanged("pepito@correo.com"))
        viewModel.onEvent(RegisterEvent.PasswordChanged(password))
        viewModel.onEvent(RegisterEvent.AcceptedTermsChanged(acceptedTerms))
    }

    @Test
    fun `CA-01-01 con campos vacios marca los tres campos`() {
        viewModel.onEvent(RegisterEvent.AcceptedTermsChanged(true))
        viewModel.onEvent(RegisterEvent.RegisterClicked)

        val state = viewModel.uiState.value
        assertEquals(
            AuthError.EmptyFields(setOf(AuthField.FULL_NAME, AuthField.EMAIL, AuthField.PASSWORD)),
            state.error
        )
        assertTrue(state.fullNameHasError)
        assertTrue(state.emailHasError)
        assertTrue(state.passwordHasError)
    }

    @Test
    fun `CA-01-03 la lista de requisitos se actualiza al escribir la contrasena`() {
        viewModel.onEvent(RegisterEvent.PasswordChanged("abc"))
        assertFalse(viewModel.uiState.value.passwordValidation.isValid)

        viewModel.onEvent(RegisterEvent.PasswordChanged("Secreta#1"))
        val validation = viewModel.uiState.value.passwordValidation
        assertTrue(validation.hasMinimumLength && validation.hasUppercase && validation.hasSpecialCharacter)
    }

    @Test
    fun `CA-01-04 con contrasena debil marca la contrasena`() {
        llenarFormulario(password = "debil")
        viewModel.onEvent(RegisterEvent.RegisterClicked)

        assertEquals(AuthError.WeakPassword, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.passwordHasError)
    }

    @Test
    fun `CA-01-05 los terminos inician sin aceptar`() {
        assertFalse(viewModel.uiState.value.acceptedTerms)
    }

    @Test
    fun `CA-01-06 con correo existente marca el correo`() {
        authRepository.createAccountResult = authFailure(AuthError.EmailAlreadyInUse)
        llenarFormulario()
        viewModel.onEvent(RegisterEvent.RegisterClicked)

        assertEquals(AuthError.EmailAlreadyInUse, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.emailHasError)
    }

    @Test
    fun `CA-01-07 con datos validos emite NavigateToSuccess`() = runTest {
        llenarFormulario()
        viewModel.onEvent(RegisterEvent.RegisterClicked)

        assertEquals(RegisterEffect.NavigateToSuccess, viewModel.effects.first())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `CA-06-01 sin conexion muestra el dialogo y Reconectar reintenta`() = runTest {
        authRepository.createAccountResult = authFailure(AuthError.Network)
        llenarFormulario()
        viewModel.onEvent(RegisterEvent.RegisterClicked)
        assertTrue(viewModel.uiState.value.isOffline)

        authRepository.createAccountResult = Result.success(FakeAuthRepository.DEFAULT_UID)
        viewModel.onEvent(RegisterEvent.RetryConnectionClicked)

        assertEquals(2, authRepository.createAccountCalls)
        assertEquals(RegisterEffect.NavigateToSuccess, viewModel.effects.first())
    }
}
