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
    fun `CA-REC-04-01 al enviar el codigo empieza la cuenta regresiva y mientras corre Reenviar no pide otro`() {
        avanzarAlPaso2()
        assertEquals(60, viewModel.uiState.value.resendCooldownSeconds)
        assertTrue(viewModel.uiState.value.isEmailInCooldown)
        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)
        assertEquals("reenviar durante la espera", 1, repository.requestCodeCalls)

        avanzarSegundos(1)
        assertEquals(59, viewModel.uiState.value.resendCooldownSeconds)

        avanzarSegundos(59)
        assertEquals(0, viewModel.uiState.value.resendCooldownSeconds)
        assertFalse(viewModel.uiState.value.isEmailInCooldown)
        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)
        assertEquals("reenviar al terminar la espera", 2, repository.requestCodeCalls)
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
    fun `CA-REC-04-03 al volver al paso 1 el mismo correo espera y otro correo vuelve a enviar`() {
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.ChangeEmailClicked)

        assertTrue(viewModel.uiState.value.isEmailInCooldown)
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)
        assertEquals("mismo correo", 1, repository.requestCodeCalls)
        assertEquals(RecoveryStep.EMAIL_REQUEST, viewModel.uiState.value.step)

        viewModel.onEvent(PasswordRecoveryEvent.EmailChanged("otro@smartfood.com"))
        assertFalse(viewModel.uiState.value.isEmailInCooldown)
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)
        assertEquals("otro correo", 2, repository.requestCodeCalls)
    }

    @Test
    fun `CA-04-06 CA-REC-03-06 un codigo incorrecto, vencido o agotado marca las casillas y sigue en el paso 2`() {
        avanzarAlPaso2()
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("9999"))

        listOf(AuthError.InvalidOtpCode, AuthError.OtpCodeExpired, AuthError.TooManyAttempts).forEach { error ->
            repository.verifyCodeResult = authFailure(error)
            viewModel.onEvent(PasswordRecoveryEvent.VerifyCodeClicked)

            val state = viewModel.uiState.value
            assertEquals(error, state.error)
            assertTrue("$error", state.otpCodeHasError)
            assertEquals("$error", RecoveryStep.OTP_VERIFICATION, state.step)
        }
    }

    @Test
    fun `CA-04-08 CA-04-09 Reenviar codigo y Correo incorrecto borran el codigo escrito`() {
        repository.requestCodeResult = Result.success(Duration.ZERO)
        avanzarAlPaso2()

        // CA-04-09: Reenviar código solicita de nuevo y sigue en el paso 2.
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("12"))
        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)
        var state = viewModel.uiState.value
        assertEquals(2, repository.requestCodeCalls)
        assertEquals(RecoveryStep.OTP_VERIFICATION, state.step)
        assertEquals("", state.otpCode)

        // CA-04-08: Correo incorrecto vuelve al paso 1 conservando el correo.
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("12"))
        viewModel.onEvent(PasswordRecoveryEvent.ChangeEmailClicked)
        state = viewModel.uiState.value
        assertEquals(RecoveryStep.EMAIL_REQUEST, state.step)
        assertEquals("demo@smartfood.com", state.email)
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
    fun `CA-REC-02-03 CA-REC-02-04 en el paso 3 un codigo invalido vuelve al paso 2 y una contrasena rechazada no`() {
        listOf(AuthError.OtpCodeExpired, AuthError.TooManyAttempts, AuthError.InvalidOtpCode, AuthError.WeakPassword)
            .forEach { error ->
                prepararEntornoTest()
                repository.resetPasswordResult = authFailure(error)
                restablecerConContrasenaValida()

                val state = viewModel.uiState.value
                assertEquals(error, state.error)
                if (error == AuthError.WeakPassword) {
                    // CA-REC-02-04: la contraseña rechazada por el servidor sigue en el paso 3 con el código.
                    assertEquals(RecoveryStep.NEW_PASSWORD, state.step)
                    assertEquals("1234", state.otpCode)
                } else {
                    assertEquals("$error", RecoveryStep.OTP_VERIFICATION, state.step)
                    assertEquals("$error", "", state.otpCode)
                    assertTrue("$error", state.otpCodeHasError)
                }
            }
    }

    private fun restablecerConContrasenaValida() {
        avanzarAlPaso3()
        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("Secreta#1"))
        viewModel.onEvent(PasswordRecoveryEvent.ConfirmPasswordChanged("Secreta#1"))
        viewModel.onEvent(PasswordRecoveryEvent.ResetPasswordClicked)
    }

    @Test
    fun `CA-04-04 CA-04-07 CA-04-11 con datos validos avanza por los tres pasos y emite NavigateToSuccess`() = runTest {
        avanzarAlPaso2()
        assertEquals(RecoveryStep.OTP_VERIFICATION, viewModel.uiState.value.step)
        viewModel.onEvent(PasswordRecoveryEvent.OtpCodeChanged("1234"))
        viewModel.onEvent(PasswordRecoveryEvent.VerifyCodeClicked)
        assertEquals(RecoveryStep.NEW_PASSWORD, viewModel.uiState.value.step)

        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("Secreta#1"))
        viewModel.onEvent(PasswordRecoveryEvent.ConfirmPasswordChanged("Secreta#1"))
        viewModel.onEvent(PasswordRecoveryEvent.ResetPasswordClicked)

        assertEquals(PasswordRecoveryEffect.NavigateToSuccess, viewModel.effects.first())
        assertNull(viewModel.uiState.value.error)
    }
}
