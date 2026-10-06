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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: PasswordRecoveryViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [PasswordRecoveryViewModel] (HU-04 y spec 003).
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

    /** Avanza el reloj virtual del dispatcher de prueba y ejecuta lo que quedó pendiente. */
    private fun avanzarSegundos(segundos: Int) {
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(segundos * 1_000L)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `CA-REC-03-03 pedir codigos muy seguido muestra el error y sigue en el paso 1`() {
        repository.requestCodeResult = authFailure(AuthError.TooManyRequests(42.seconds))
        avanzarAlPaso2()

        val state = viewModel.uiState.value
        assertEquals(AuthError.TooManyRequests(42.seconds), state.error)
        assertEquals(RecoveryStep.EMAIL_REQUEST, state.step)
        assertFalse(state.emailHasError)
    }

    @Test
    fun `CA-REC-03-04 reenviar al superar el limite muestra el error y conserva el paso 2`() {
        repository.requestCodeResult = Result.success(Duration.ZERO)
        avanzarAlPaso2()
        repository.requestCodeResult = authFailure(AuthError.TooManyRequests(42.seconds))
        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)

        val state = viewModel.uiState.value
        assertEquals(AuthError.TooManyRequests(42.seconds), state.error)
        assertEquals(RecoveryStep.OTP_VERIFICATION, state.step)
        assertFalse(state.otpCodeHasError)
    }

    @Test
    fun `CA-REC-04-01 al enviar el codigo empieza la cuenta regresiva y baja cada segundo`() {
        avanzarAlPaso2()
        assertEquals(60, viewModel.uiState.value.resendCooldownSeconds)
        assertTrue(viewModel.uiState.value.isEmailInCooldown)

        avanzarSegundos(1)
        assertEquals(59, viewModel.uiState.value.resendCooldownSeconds)

        avanzarSegundos(59)
        assertEquals(0, viewModel.uiState.value.resendCooldownSeconds)
        assertFalse(viewModel.uiState.value.isEmailInCooldown)
    }

    @Test
    fun `CA-REC-04-01 durante la cuenta regresiva Reenviar no pide otro codigo`() {
        avanzarAlPaso2()

        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)
        assertEquals(1, repository.requestCodeCalls)

        avanzarSegundos(60)
        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)
        assertEquals(2, repository.requestCodeCalls)
    }

    @Test
    fun `CA-REC-04-02 el rechazo por limites muestra la espera real y al terminar borra el aviso`() {
        repository.requestCodeResult = authFailure(AuthError.TooManyRequests(2_232.seconds))
        avanzarAlPaso2()
        assertEquals(2_232, viewModel.uiState.value.resendCooldownSeconds)

        avanzarSegundos(2_232)
        val state = viewModel.uiState.value
        assertEquals(0, state.resendCooldownSeconds)
        assertNull(state.error)
    }

    @Test
    fun `CA-REC-04-03 al volver al paso 1 con el mismo correo no se puede enviar hasta que termine la espera`() {
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.ChangeEmailClicked)

        assertTrue(viewModel.uiState.value.isEmailInCooldown)
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)
        assertEquals(1, repository.requestCodeCalls)
        assertEquals(RecoveryStep.EMAIL_REQUEST, viewModel.uiState.value.step)
    }

    @Test
    fun `CA-REC-04-03 con otro correo el boton vuelve a enviar`() {
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.ChangeEmailClicked)
        viewModel.onEvent(PasswordRecoveryEvent.EmailChanged("otro@smartfood.com"))

        assertFalse(viewModel.uiState.value.isEmailInCooldown)
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)
        assertEquals(2, repository.requestCodeCalls)
    }

    @Test
    fun `CA-REC-03-06 codigo vencido o agotado marca las casillas en el paso 2`() {
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("1234"))

        listOf(AuthError.OtpCodeExpired, AuthError.TooManyAttempts).forEach { error ->
            repository.verifyCodeResult = authFailure(error)
            viewModel.onEvent(PasswordRecoveryEvent.VerifyCodeClicked)

            val state = viewModel.uiState.value
            assertEquals(error, state.error)
            assertTrue(state.otpCodeHasError)
            assertEquals(RecoveryStep.OTP_VERIFICATION, state.step)
        }
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
        repository.requestCodeResult = Result.success(Duration.ZERO)
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
    fun `CA-REC-02-03 si el codigo vencio en el paso 3 vuelve al paso 2 con el codigo borrado`() {
        listOf(AuthError.OtpCodeExpired, AuthError.TooManyAttempts, AuthError.InvalidOtpCode).forEach { error ->
            prepararEntornoTest()
            repository.resetPasswordResult = authFailure(error)
            restablecerConContrasenaValida()

            val state = viewModel.uiState.value
            assertEquals(error, state.error)
            assertEquals(RecoveryStep.OTP_VERIFICATION, state.step)
            assertEquals("", state.otpCode)
            assertTrue(state.otpCodeHasError)
        }
    }

    @Test
    fun `CA-REC-02-04 contrasena rechazada por el servidor sigue en el paso 3`() {
        repository.resetPasswordResult = authFailure(AuthError.WeakPassword)
        restablecerConContrasenaValida()

        val state = viewModel.uiState.value
        assertEquals(AuthError.WeakPassword, state.error)
        assertEquals(RecoveryStep.NEW_PASSWORD, state.step)
        assertEquals("1234", state.otpCode)
    }

    private fun restablecerConContrasenaValida() {
        avanzarAlPaso3()
        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("Secreta#1"))
        viewModel.onEvent(PasswordRecoveryEvent.ConfirmPasswordChanged("Secreta#1"))
        viewModel.onEvent(PasswordRecoveryEvent.ResetPasswordClicked)
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
