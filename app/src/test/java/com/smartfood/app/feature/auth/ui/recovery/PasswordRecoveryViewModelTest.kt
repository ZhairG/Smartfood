package com.smartfood.app.feature.auth.ui.recovery

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.model.authFailure
import com.smartfood.app.feature.auth.domain.usecase.RequestRecoveryCodeUseCase
import com.smartfood.app.feature.auth.domain.usecase.ResetPasswordUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidateEmailUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import com.smartfood.app.feature.auth.domain.usecase.VerifyRecoveryCodeUseCase
import com.smartfood.app.testutil.FakePasswordRecoveryRepository
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: PasswordRecoveryViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [PasswordRecoveryViewModel] (HU-04).
 */
class PasswordRecoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakePasswordRecoveryRepository
    private lateinit var viewModel: PasswordRecoveryViewModel

    @Before
    fun prepararEntornoTest() {
        repository = FakePasswordRecoveryRepository()
        val validatePassword = ValidatePasswordUseCase()
        viewModel = PasswordRecoveryViewModel(
            requestCode = RequestRecoveryCodeUseCase(repository, ValidateEmailUseCase()),
            verifyCode = VerifyRecoveryCodeUseCase(repository),
            resetPassword = ResetPasswordUseCase(repository, validatePassword),
            validatePassword = validatePassword
        )
    }

    private fun avanzarAlPaso2() {
        viewModel.onEvent(PasswordRecoveryEvent.EmailChanged("demo@smartfood.com"))
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)
    }

    private fun avanzarAlPaso3() {
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("1234"))
        viewModel.onEvent(PasswordRecoveryEvent.VerifyCodeClicked)
    }

    @Test
    fun `CA-04-01 correo vacio muestra error y sigue en el paso 1`() {
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthError.EmptyFields(setOf(AuthField.EMAIL)), state.error)
        assertTrue(state.emailHasError)
        assertEquals(RecoveryStep.EMAIL_REQUEST, state.step)
    }

    @Test
    fun `CA-04-03 correo sin cuenta muestra AccountNotFound`() {
        repository.requestCodeResult = authFailure(AuthError.AccountNotFound)
        avanzarAlPaso2()

        assertEquals(AuthError.AccountNotFound, viewModel.uiState.value.error)
        assertEquals(RecoveryStep.EMAIL_REQUEST, viewModel.uiState.value.step)
    }

    @Test
    fun `CA-04-04 correo con cuenta avanza al paso 2`() {
        avanzarAlPaso2()

        assertEquals(RecoveryStep.OTP_VERIFICATION, viewModel.uiState.value.step)
    }

    @Test
    fun `CA-04-06 codigo incorrecto marca las casillas y sigue en el paso 2`() {
        repository.verifyCodeResult = authFailure(AuthError.InvalidOtpCode)
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("9999"))
        viewModel.onEvent(PasswordRecoveryEvent.VerifyCodeClicked)

        val state = viewModel.uiState.value
        assertTrue(state.otpCodeHasError)
        assertEquals(RecoveryStep.OTP_VERIFICATION, state.step)
    }

    @Test
    fun `CA-04-07 codigo correcto avanza al paso 3`() {
        avanzarAlPaso3()

        assertEquals(RecoveryStep.NEW_PASSWORD, viewModel.uiState.value.step)
    }

    @Test
    fun `CA-04-08 Correo incorrecto vuelve al paso 1 conservando el correo y borrando el codigo`() {
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("12"))
        viewModel.onEvent(PasswordRecoveryEvent.ChangeEmailClicked)

        val state = viewModel.uiState.value
        assertEquals(RecoveryStep.EMAIL_REQUEST, state.step)
        assertEquals("demo@smartfood.com", state.email)
        assertEquals("", state.otpCode)
    }

    @Test
    fun `CA-04-09 Reenviar codigo solicita de nuevo y borra el codigo escrito`() {
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("12"))
        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)

        val state = viewModel.uiState.value
        assertEquals(2, repository.requestCodeCalls)
        assertEquals(RecoveryStep.OTP_VERIFICATION, state.step)
        assertEquals("", state.otpCode)
    }

    @Test
    fun `CA-04-10 el boton se habilita solo con contrasena valida y confirmada`() {
        avanzarAlPaso3()
        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("Secreta#1"))
        assertFalse(viewModel.uiState.value.canResetPassword)

        viewModel.onEvent(PasswordRecoveryEvent.ConfirmPasswordChanged("Secreta#1"))
        assertTrue(viewModel.uiState.value.canResetPassword)

        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("debil"))
        assertFalse(viewModel.uiState.value.canResetPassword)
    }

    @Test
    fun `CA-04-11 restablecer con datos validos emite NavigateToSuccess`() = runTest {
        avanzarAlPaso3()
        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("Secreta#1"))
        viewModel.onEvent(PasswordRecoveryEvent.ConfirmPasswordChanged("Secreta#1"))
        viewModel.onEvent(PasswordRecoveryEvent.ResetPasswordClicked)

        assertEquals(PasswordRecoveryEffect.NavigateToSuccess, viewModel.effects.first())
        assertNull(viewModel.uiState.value.error)
    }
}
