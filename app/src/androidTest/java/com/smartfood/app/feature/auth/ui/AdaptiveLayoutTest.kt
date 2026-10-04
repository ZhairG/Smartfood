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

/**
 * Nombre: AdaptiveLayoutTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Verifica que cada pantalla de formulario cabe sin scroll (spec 002, HU-UI-01): con el teclado
 * cerrado, ningún contenedor desplazable tiene rango de scroll y el botón principal es visible.
 * Se ejecuta en 360×720 dp (perfil compacto) y 384×857 dp (perfil medio, teléfono V2436).
 */
@RunWith(Parameterized::class)
class AdaptiveLayoutTest(widthDp: Int, heightDp: Int) {

    companion object {
        // Se usan Int y no DpSize: DpSize es una value class y su constructor no es compatible con Parameterized.
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}dp")
        fun tamanos(): List<Array<Any>> = listOf(
            arrayOf(360, 720),
            arrayOf(384, 857)
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
        mostrar { RegisterScreen(RegisterUiState(acceptedTerms = true), {}, {}) }
        assertCabeSinScroll("Crear Cuenta")
    }

    @Test
    fun ca_ui_01_01_registroConMensajeDeError_cabeSinScroll() {
        mostrar { RegisterScreen(RegisterUiState(acceptedTerms = true, error = AuthError.EmailAlreadyInUse), {}, {}) }
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
