package com.smartfood.app.feature.auth.ui.recovery

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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

    private val eventos = mutableListOf<PasswordRecoveryEvent>()
    private val estado = mutableStateOf(PasswordRecoveryUiState())

    /** Monta la pantalla una sola vez; cada paso del flujo cambia [estado]. */
    private fun mostrar(inicial: PasswordRecoveryUiState = PasswordRecoveryUiState()) {
        estado.value = inicial
        composeTestRule.setContent {
            SmartfoodTheme {
                PasswordRecoveryScreen(state = estado.value, onEvent = { eventos += it }, onNavigateToLogin = {})
            }
        }
    }

    private fun paso(nuevo: PasswordRecoveryUiState) {
        eventos.clear()
        estado.value = nuevo
        composeTestRule.waitForIdle()
    }

    private fun mostrado(texto: String) = composeTestRule.onNodeWithText(texto).assertIsDisplayed()

    /** El nodo editable declara [type] para el autocompletado del teléfono (spec 001, HU-07). */
    private fun conTipo(type: ContentType) = SemanticsMatcher.expectValue(SemanticsProperties.ContentType, type)

    @Test
    fun ca0401_0703_caRec0403_paso1PideElCorreoYRespetaLaEsperaDeEseCorreo() {
        mostrar()
        mostrado("Recupera tu acceso y vuelve a disfrutar de tus recetas favoritas.")
        // CA-07-03: el correo se ofrece como usuario al autocompletado.
        composeTestRule.onNodeWithText("Correo electrónico").assert(conTipo(EmailAccountContentType))
        composeTestRule.onNodeWithText("Enviar Código").performClick()
        assertEquals("enviar el código", listOf<PasswordRecoveryEvent>(PasswordRecoveryEvent.SendCodeClicked), eventos)

        // CA-04-01: correo vacío.
        paso(PasswordRecoveryUiState(error = AuthError.EmptyFields(setOf(AuthField.EMAIL))))
        mostrado("Correo no puede estar vacío")

        // CA-REC-04-03: la espera deshabilita el botón solo para el mismo correo.
        paso(PasswordRecoveryUiState(email = "demo@smartfood.com", resendCooldownSeconds = 42, cooldownEmail = "demo@smartfood.com"))
        composeTestRule.onNode(hasText("Enviar Código en 0:42") and hasClickAction()).assertIsNotEnabled()
        paso(PasswordRecoveryUiState(email = "otro@smartfood.com", resendCooldownSeconds = 42, cooldownEmail = "demo@smartfood.com"))
        composeTestRule.onNode(hasText("Enviar Código") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun ca0404_0406_caRec0103_0301_0302_0303_0401_paso2MuestraElCorreoLosErroresYElReenvio() {
        val paso2 = PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, email = "demo@smartfood.com")
        // CA-04-04 y CA-REC-01-03: subtítulo neutro con el correo ingresado; sin espera se ofrece reenviar.
        mostrar(paso2)
        mostrado("Verifica tu correo")
        mostrado("Si el correo tiene una cuenta, te enviamos un código de 4 dígitos a:")
        mostrado("demo@smartfood.com")
        mostrado("Reenviar código")

        // CA-04-06, CA-REC-03-01, 03-02 y 03-03: cada error del código muestra su mensaje.
        mapOf(
            AuthError.InvalidOtpCode to "Código incorrecto, inténtalo de nuevo",
            AuthError.OtpCodeExpired to "El código venció. Solicita uno nuevo",
            AuthError.TooManyAttempts to "Demasiados intentos. Solicita un código nuevo",
            AuthError.TooManyRequests(42.seconds) to "Espera un momento antes de pedir otro código"
        ).forEach { (error, mensaje) ->
            paso(paso2.copy(error = error))
            mostrado(mensaje)
        }

        // CA-REC-04-01: con espera, la cuenta regresiva reemplaza al enlace.
        paso(paso2.copy(resendCooldownSeconds = 42, cooldownEmail = "demo@smartfood.com"))
        mostrado("Reenviar código en")
        composeTestRule.onNodeWithText("0:42", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Reenviar código").assertDoesNotExist()
    }

    @Test
    fun ca0410_0703_paso3HabilitaElBotonSoloConLaContrasenaValidaYConfirmada() {
        mostrar(PasswordRecoveryUiState(step = RecoveryStep.NEW_PASSWORD))
        composeTestRule.onNode(hasText("Reestablecer Contraseña") and hasClickAction()).assertIsNotEnabled()
        // CA-07-03: las dos contraseñas se piden como nuevas al autocompletado.
        composeTestRule.onNodeWithText("Nueva Contraseña").assert(conTipo(ContentType.NewPassword))
        composeTestRule.onNodeWithText("Confirmar Contraseña").assert(conTipo(ContentType.NewPassword))

        val password = "Secreta#1"
        paso(
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
