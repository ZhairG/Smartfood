package com.smartfood.app.feature.auth.ui.recovery

import com.smartfood.app.feature.auth.domain.usecase.RequestRecoveryCodeUseCase
import com.smartfood.app.feature.auth.domain.usecase.ResetPasswordUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidateEmailUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import com.smartfood.app.feature.auth.domain.usecase.VerifyRecoveryCodeUseCase
import com.smartfood.app.testutil.FakePasswordRecoveryRepository
import com.smartfood.app.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: PasswordRecoveryLinkViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas de [PasswordRecoveryViewModel] en el modo gratuito con enlace (spec 012, HU-SPK-02).
 */
class PasswordRecoveryLinkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repository: FakePasswordRecoveryRepository): PasswordRecoveryViewModel {
        val validatePassword = ValidatePasswordUseCase()
        return PasswordRecoveryViewModel(
            requestCode = RequestRecoveryCodeUseCase(repository, ValidateEmailUseCase()),
            verifyCode = VerifyRecoveryCodeUseCase(repository),
            resetPassword = ResetPasswordUseCase(repository, validatePassword),
            validatePassword = validatePassword
        )
    }

    @Test
    fun `CA-SPK-02-01 CA-SPK-02-02 con enlace el envio pasa a Revisa tu correo`() {
        val repository = FakePasswordRecoveryRepository(usesCode = false)
        val viewModel = viewModel(repository)
        assertTrue(viewModel.uiState.value.isLinkMode)

        viewModel.onEvent(PasswordRecoveryEvent.EmailChanged("demo@smartfood.com"))
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)

        assertEquals(1, repository.requestCodeCalls)
        assertEquals(RecoveryStep.LINK_SENT, viewModel.uiState.value.step)
    }

    @Test
    fun `CA-SPK-02-03 el reenvio del enlace respeta la espera de 60 segundos`() {
        val repository = FakePasswordRecoveryRepository(usesCode = false)
        val viewModel = viewModel(repository)
        viewModel.onEvent(PasswordRecoveryEvent.EmailChanged("demo@smartfood.com"))
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)

        assertEquals(60, viewModel.uiState.value.resendCooldownSeconds)
        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)
        assertEquals("durante la espera no se reenvía", 1, repository.requestCodeCalls)
    }

    @Test
    fun `CA-SPK-02-05 con codigo el flujo sigue en los 3 pasos`() {
        val repository = FakePasswordRecoveryRepository(usesCode = true)
        val viewModel = viewModel(repository)
        assertFalse(viewModel.uiState.value.isLinkMode)

        viewModel.onEvent(PasswordRecoveryEvent.EmailChanged("demo@smartfood.com"))
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)

        assertEquals(RecoveryStep.OTP_VERIFICATION, viewModel.uiState.value.step)
    }
}
