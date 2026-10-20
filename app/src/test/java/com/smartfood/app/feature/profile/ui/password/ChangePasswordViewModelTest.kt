package com.smartfood.app.feature.profile.ui.password

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import com.smartfood.app.feature.profile.domain.usecase.ChangePasswordUseCase
import com.smartfood.app.testutil.FakeAuthRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: ChangePasswordViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ChangePasswordViewModel] (spec 008, HU-PER-05, T022).
 */
class ChangePasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var validatePasswordUseCase: ValidatePasswordUseCase
    private lateinit var changePasswordUseCase: ChangePasswordUseCase
    private lateinit var viewModel: ChangePasswordViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        validatePasswordUseCase = ValidatePasswordUseCase()
        changePasswordUseCase = ChangePasswordUseCase(authRepository, validatePasswordUseCase)
        viewModel = ChangePasswordViewModel(changePasswordUseCase, validatePasswordUseCase)
    }

    @Test
    fun `CA-PER-05-03 canSubmit es true solo si los tres campos son validos`() {
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEvent(ChangePasswordEvent.CurrentPasswordChanged("Pass123!"))
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEvent(ChangePasswordEvent.NewPasswordChanged("NewPass123!"))
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEvent(ChangePasswordEvent.ConfirmPasswordChanged("NewPass123!"))
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `CA-PER-05-04 submit exitoso muestra guardado y emite efecto NavigateBack`() = runTest {
        viewModel.onEvent(ChangePasswordEvent.CurrentPasswordChanged("Pass123!"))
        viewModel.onEvent(ChangePasswordEvent.NewPasswordChanged("NewPass123!"))
        viewModel.onEvent(ChangePasswordEvent.ConfirmPasswordChanged("NewPass123!"))

        viewModel.onEvent(ChangePasswordEvent.SubmitClicked)
        runCurrent()

        assertTrue(viewModel.uiState.value.isSavedSuccess)

        advanceTimeBy(2005L)
        val effect = viewModel.effects.first()
        assertEquals(ChangePasswordEffect.NavigateBack, effect)
    }

    @Test
    fun `CA-PER-05-05 contraseña actual incorrecta marca error en contraseña actual`() = runTest {
        authRepository.reauthenticateResult = authFailure(AuthError.InvalidCredentials)

        viewModel.onEvent(ChangePasswordEvent.CurrentPasswordChanged("WrongPass!"))
        viewModel.onEvent(ChangePasswordEvent.NewPasswordChanged("NewPass123!"))
        viewModel.onEvent(ChangePasswordEvent.ConfirmPasswordChanged("NewPass123!"))

        viewModel.onEvent(ChangePasswordEvent.SubmitClicked)
        runCurrent()

        assertEquals(AuthError.InvalidCredentials, viewModel.uiState.value.currentPasswordError)
        assertNull(viewModel.uiState.value.generalError)
        assertFalse(viewModel.uiState.value.isSavedSuccess)
    }
}
