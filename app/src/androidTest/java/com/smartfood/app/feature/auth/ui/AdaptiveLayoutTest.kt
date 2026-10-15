package com.smartfood.app.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.ActionSuccessScreen
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.ui.login.LoginScreen
import com.smartfood.app.feature.auth.ui.login.LoginUiState
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryScreen
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryUiState
import com.smartfood.app.feature.auth.ui.recovery.RecoveryStep
import com.smartfood.app.feature.auth.ui.register.RegisterScreen
import com.smartfood.app.feature.auth.ui.register.RegisterUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.ceil

/**
 * Nombre: AdaptiveLayoutTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Verifica que cada pantalla de formulario cabe sin scroll (spec 002, HU-UI-01): con el teclado
 * cerrado, ningún contenedor desplazable tiene rango de scroll y el botón principal es visible.
 * Se ejecuta en 360×720 dp (perfil compacto), 384×857 dp (perfil medio, teléfono V2436) y 412×917 dp (tamaño de los
 * frames de Figma).
 */
@RunWith(Parameterized::class)
class AdaptiveLayoutTest(widthDp: Int, heightDp: Int) {

    companion object {
        // Se usan Int y no DpSize: DpSize es una value class y su constructor no es compatible con Parameterized.
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}dp")
        fun tamanos(): List<Array<Any>> = listOf(
            arrayOf(360, 720),
            arrayOf(384, 857),
            arrayOf(412, 917)  // tamaño de los frames de Figma (CA-UI-07-03)
        )
    }

    private val screenSize = DpSize(widthDp.dp, heightDp.dp)

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(screenSize)) {
                SmartfoodTheme { content() }
            }
        }
        // El scaffold baja de perfil solo si el desborde se mantiene; el reloj de la prueba es virtual y waitForIdle
        // no avanza esos delay, así que se deja pasar el tiempo para que cada pantalla use su perfil definitivo.
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.waitForIdle()
    }

    /** Ningún contenedor desplazable puede tener rango de scroll y [buttonText] debe verse completo. */
    private fun assertCabeSinScroll(buttonText: String) {
        val scrollables = composeTestRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNodes()
        assertTrue("Debe existir el contenedor del formulario", scrollables.isNotEmpty())
        scrollables.forEach { node ->
            val range = node.config[SemanticsProperties.VerticalScrollAxisRange]
            assertEquals("No debe haber scroll en $screenSize", 0f, range.maxValue(), 0.5f)
        }
        composeTestRule.onNode(hasText(buttonText) and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun ca_ui_01_01_login_cabeSinScroll() {
        mostrar { LoginScreen(LoginUiState(), {}, {}, {}) }
        assertCabeSinScroll("Iniciar Sesión")
    }

    @Test
    fun ca_ui_01_01_registro_cabeSinScroll() {
        mostrar { RegisterScreen(RegisterUiState(acceptedTerms = true), {}, {}, {}, {}) }
        assertCabeSinScroll("Crear Cuenta")
    }

    @Test
    fun ca_ui_01_01_registroConMensajeDeError_cabeSinScroll() {
        mostrar { RegisterScreen(RegisterUiState(acceptedTerms = true, error = AuthError.EmailAlreadyInUse), {}, {}, {}, {}) }
        assertCabeSinScroll("Crear Cuenta")
    }

    @Test
    fun ca_ui_01_01_recuperacionPaso1_cabeSinScroll() {
        mostrar { PasswordRecoveryScreen(PasswordRecoveryUiState(step = RecoveryStep.EMAIL_REQUEST), {}, {}) }
        assertCabeSinScroll("Enviar Código")
    }

    @Test
    fun ca_ui_01_01_recuperacionPaso2_cabeSinScroll() {
        mostrar {
            PasswordRecoveryScreen(
                PasswordRecoveryUiState(step = RecoveryStep.OTP_VERIFICATION, email = "demo@smartfood.com"),
                {},
                {}
            )
        }
        assertCabeSinScroll("Verificar Código")
    }

    /**
     * CA-REC-04-04: con la cuenta regresiva más larga ("37:12", límite de 5 por hora) el paso 2 sigue cabiendo y la
     * fila cabe en una línea y la píldora no se recorta.
     */
    @Test
    fun ca_rec_04_04_recuperacionPaso2ConCuentaRegresiva_cabeSinScroll() {
        mostrar {
            PasswordRecoveryScreen(
                PasswordRecoveryUiState(
                    step = RecoveryStep.OTP_VERIFICATION,
                    email = "demo@smartfood.com",
                    resendCooldownSeconds = 2_232,
                    cooldownEmail = "demo@smartfood.com"
                ),
                {},
                {}
            )
        }
        assertCabeSinScroll("Verificar Código")

        val texto = textLayout("Reenviar código en")
        assertEquals("El texto debe quedar en una línea en $screenSize", 1, texto.lineCount)
        val pildora = textLayout("37:12")
        // hasVisualOverflow no sirve aquí: la semántica rearma el texto con el ancho máximo disponible y no con el
        // que recibió. Se compara el ancho que el texto necesita con el que ocupa.
        assertEquals("La píldora debe quedar en una línea en $screenSize", 1, pildora.lineCount)
        assertTrue(
            "La píldora no debe recortarse en $screenSize",
            ceil(pildora.multiParagraph.maxIntrinsicWidth) <= pildora.size.width
        )
    }

    /** Resultado del layout del texto visible [text], para comprobar líneas y recortes. */
    private fun textLayout(text: String): TextLayoutResult {
        val node = composeTestRule.onNodeWithText(text, useUnmergedTree = true)
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.first()
    }

    @Test
    fun ca_ui_01_01_recuperacionPaso3_cabeSinScroll() {
        mostrar { PasswordRecoveryScreen(PasswordRecoveryUiState(step = RecoveryStep.NEW_PASSWORD), {}, {}) }
        assertCabeSinScroll("Reestablecer Contraseña")
    }

    @Test
    fun ca_ui_01_02_pantallaDeExito_muestraElBotonCompleto() {
        mostrar {
            ActionSuccessScreen(
                title = "¡Cuenta creada con éxito!",
                body = "Ya puedes acceder a todas nuestras funciones. Te enviamos un correo de confirmación.",
                buttonText = "Continuar",
                onButtonClick = {}
            )
        }
        composeTestRule.onNode(hasText("Continuar") and hasClickAction()).assertIsDisplayed()
    }
}
