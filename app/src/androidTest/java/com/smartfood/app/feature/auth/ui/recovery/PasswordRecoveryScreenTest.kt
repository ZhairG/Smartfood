package com.smartfood.app.feature.auth.ui.recovery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Nombre: PasswordRecoveryScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de UI de la pantalla stateless [PasswordRecoveryScreen] (HU-04 y spec 003).
 */
class PasswordRecoveryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(
        state: PasswordRecoveryUiState = PasswordRecoveryUiState(),
        onEvent: (PasswordRecoveryEvent) -> Unit = {}
    ) {
        composeTestRule.setContent {
            SmartfoodTheme {
                PasswordRecoveryScreen(state = state, onEvent = onEvent, onNavigateToLogin = {})
            }
        }
    }

    @Test
    fun paso1_muestraTituloYEnviarCodigo() {
        var lastEvent: PasswordRecoveryEvent? = null
        mostrar(onEvent = { lastEvent = it })

        composeTestRule.onNodeWithText("Recupera tu acceso y vuelve a disfrutar de tus recetas favoritas.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enviar Código").performClick()

        assertEquals(PasswordRecoveryEvent.SendCodeClicked, lastEvent)
    }

    @Test
    fun ca0401_correoVacio_muestraMensaje() {
        mostrar(PasswordRecoveryUiState(error = AuthError.EmptyFields(setOf(AuthField.EMAIL))))

        composeTestRule.onNodeWithText("Correo no puede estar vacío").assertIsDisplayed()
    }

    @Test
    fun ca0404_caRec0103_paso2_muestraSubtituloNeutroYElCorreoIngresado() {
        mostrar(PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, email = "demo@smartfood.com"))

        composeTestRule.onNodeWithText("Verifica tu correo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Si el correo tiene una cuenta, te enviamos un código de 4 dígitos a:")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("demo@smartfood.com").assertIsDisplayed()
    }

    @Test
    fun caRec0301_codigoVencido_muestraMensaje() {
        mostrar(PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, error = AuthError.OtpCodeExpired))

        composeTestRule.onNodeWithText("El código venció. Solicita uno nuevo").assertIsDisplayed()
    }

    @Test
    fun caRec0302_intentosAgotados_muestraMensaje() {
        mostrar(PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, error = AuthError.TooManyAttempts))

        composeTestRule.onNodeWithText("Demasiados intentos. Solicita un código nuevo").assertIsDisplayed()
    }

    @Test
    fun caRec0303_demasiadasSolicitudes_muestraMensaje() {
        mostrar(
            PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, error = AuthError.TooManyRequests(42.seconds))
        )

        composeTestRule.onNodeWithText("Espera un momento antes de pedir otro código").assertIsDisplayed()
    }

    @Test
    fun ca0406_codigoIncorrecto_muestraMensaje() {
        mostrar(PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, error = AuthError.InvalidOtpCode))

        composeTestRule.onNodeWithText("Código incorrecto, inténtalo de nuevo").assertIsDisplayed()
    }

    @Test
    fun caRec0401_paso2_conEsperaMuestraCuentaRegresivaEnLugarDelEnlace() {
        mostrar(
            PasswordRecoveryUiState(
                step = RecoveryStep.OTP_VERIFICATION,
                email = "demo@smartfood.com",
                resendCooldownSeconds = 42,
                cooldownEmail = "demo@smartfood.com"
            )
        )

        composeTestRule.onNodeWithText("Reenviar código en").assertIsDisplayed()
        composeTestRule.onNodeWithText("0:42", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Reenviar código").assertDoesNotExist()
    }

    @Test
    fun caRec0401_paso2_sinEsperaMuestraElEnlaceReenviar() {
        mostrar(PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, email = "demo@smartfood.com"))

        composeTestRule.onNodeWithText("Reenviar código").assertIsDisplayed()
    }

    @Test
    fun caRec0403_paso1_conEsperaParaElMismoCorreoDeshabilitaElBoton() {
        mostrar(
            PasswordRecoveryUiState(
                email = "demo@smartfood.com",
                resendCooldownSeconds = 42,
                cooldownEmail = "demo@smartfood.com"
            )
        )

        composeTestRule.onNode(hasText("Enviar Código en 0:42") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun caRec0403_paso1_conOtroCorreoElBotonQuedaHabilitado() {
        mostrar(
            PasswordRecoveryUiState(
                email = "otro@smartfood.com",
                resendCooldownSeconds = 42,
                cooldownEmail = "demo@smartfood.com"
            )
        )

        composeTestRule.onNode(hasText("Enviar Código") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun ca0410_paso3_botonDeshabilitadoHastaQueLaContrasenaSeaValida() {
        mostrar(PasswordRecoveryUiState(step = RecoveryStep.NEW_PASSWORD))

        composeTestRule.onNode(hasText("Reestablecer Contraseña") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun ca0410_paso3_botonHabilitadoConContrasenaValidaYConfirmada() {
        val password = "Secreta#1"
        mostrar(
            PasswordRecoveryUiState(
                step = RecoveryStep.NEW_PASSWORD,
                newPassword = password,
                confirmPassword = password,
                passwordValidation = ValidatePasswordUseCase().execute(password)
            )
        )

        composeTestRule.onNode(hasText("Reestablecer Contraseña") and hasClickAction()).assertIsEnabled()
    }
}
