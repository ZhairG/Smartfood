package com.smartfood.app.feature.auth.ui.recovery

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.test.assert
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
import com.smartfood.app.feature.auth.ui.components.EmailAccountContentType
import com.smartfood.app.testutil.PantallaEnFlujo
import com.smartfood.app.testutil.conTipo
import com.smartfood.app.testutil.mostrado
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: PasswordRecoveryScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de UI de la pantalla stateless [PasswordRecoveryScreen] (HU-04 y spec 003). Hay una prueba por
 * paso del flujo: cada una monta la pantalla una sola vez y recorre sus estados cambiando el `UiState` (#105).
 */
class PasswordRecoveryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    private val flujo = PantallaEnFlujo<PasswordRecoveryUiState, PasswordRecoveryEvent>(
        composeTestRule,
        PasswordRecoveryUiState()
    ) { state, onEvent ->
        SmartfoodTheme { PasswordRecoveryScreen(state = state, onEvent = onEvent, onNavigateToLogin = {}) }
    }

    @Test
    fun ca0401_0703_caRec0403_paso1PideElCorreoYRespetaLaEsperaDeEseCorreo() {
        flujo.mostrar()
        composeTestRule.mostrado("Recupera tu acceso y vuelve a disfrutar de tus recetas favoritas.")
        // CA-07-03: el correo se ofrece como usuario al autocompletado.
        composeTestRule.onNodeWithText("Correo electrónico").assert(conTipo(EmailAccountContentType))
        composeTestRule.onNodeWithText("Enviar Código").performClick()
        assertEquals("enviar el código", listOf<PasswordRecoveryEvent>(PasswordRecoveryEvent.SendCodeClicked), flujo.eventos)

        // CA-04-01: correo vacío.
        flujo.paso(PasswordRecoveryUiState(error = AuthError.EmptyFields(setOf(AuthField.EMAIL))))
        composeTestRule.mostrado("Correo no puede estar vacío")

        // CA-REC-04-03: la espera deshabilita el botón solo para el mismo correo.
        flujo.paso(PasswordRecoveryUiState(email = "demo@smartfood.com", resendCooldownSeconds = 42, cooldownEmail = "demo@smartfood.com"))
        composeTestRule.onNode(hasText("Enviar Código en 0:42") and hasClickAction()).assertIsNotEnabled()
        flujo.paso(PasswordRecoveryUiState(email = "otro@smartfood.com", resendCooldownSeconds = 42, cooldownEmail = "demo@smartfood.com"))
        composeTestRule.onNode(hasText("Enviar Código") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun ca0404_0406_caRec0103_0301_0302_0303_0401_paso2MuestraElCorreoLosErroresYElReenvio() {
        val paso2 = PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, email = "demo@smartfood.com")
        // CA-04-04 y CA-REC-01-03: subtítulo neutro con el correo ingresado; sin espera se ofrece reenviar.
        flujo.mostrar(paso2)
        composeTestRule.mostrado("Verifica tu correo")
        composeTestRule.mostrado("Si el correo tiene una cuenta, te enviamos un código de 4 dígitos a:")
        composeTestRule.mostrado("demo@smartfood.com")
        composeTestRule.mostrado("Reenviar código")

        // CA-04-06, CA-REC-03-01, 03-02 y 03-03: cada error del código muestra su mensaje.
        mapOf(
            AuthError.InvalidOtpCode to "Código incorrecto, inténtalo de nuevo",
            AuthError.OtpCodeExpired to "El código venció. Solicita uno nuevo",
            AuthError.TooManyAttempts to "Demasiados intentos. Solicita un código nuevo",
            AuthError.TooManyRequests(42.seconds) to "Espera un momento antes de pedir otro código"
        ).forEach { (error, mensaje) ->
            flujo.paso(paso2.copy(error = error))
            composeTestRule.mostrado(mensaje)
        }

        // CA-REC-04-01: con espera, la cuenta regresiva reemplaza al enlace.
        flujo.paso(paso2.copy(resendCooldownSeconds = 42, cooldownEmail = "demo@smartfood.com"))
        composeTestRule.mostrado("Reenviar código en")
        composeTestRule.onNodeWithText("0:42", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Reenviar código").assertDoesNotExist()
    }

    @Test
    fun ca0410_0703_paso3HabilitaElBotonSoloConLaContrasenaValidaYConfirmada() {
        flujo.mostrar(PasswordRecoveryUiState(step = RecoveryStep.NEW_PASSWORD))
        composeTestRule.onNode(hasText("Reestablecer Contraseña") and hasClickAction()).assertIsNotEnabled()
        // CA-07-03: las dos contraseñas se piden como nuevas al autocompletado.
        composeTestRule.onNodeWithText("Nueva Contraseña").assert(conTipo(ContentType.NewPassword))
        composeTestRule.onNodeWithText("Confirmar Contraseña").assert(conTipo(ContentType.NewPassword))

        val password = "Secreta#1"
        flujo.paso(
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
