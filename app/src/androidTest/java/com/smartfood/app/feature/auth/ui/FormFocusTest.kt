package com.smartfood.app.feature.auth.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryScreen
import com.smartfood.app.feature.auth.ui.recovery.PasswordRecoveryUiState
import com.smartfood.app.feature.auth.ui.recovery.RecoveryStep
import com.smartfood.app.feature.auth.ui.register.RegisterScreen
import com.smartfood.app.feature.auth.ui.register.RegisterUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> FormFocusTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas de foco y del popup de requisitos de la contraseña (spec 002, HU-UI-02 y HU-UI-03).<br>
 * </build>
 */
class FormFocusTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val campoContrasena = hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)

    private fun mostrarRegistro() {
        composeTestRule.setContent {
            SmartfoodTheme { RegisterScreen(state = RegisterUiState(), onEvent = {}, onNavigateToLogin = {}) }
        }
    }

    /** Borde inferior del nodo en coordenadas de pantalla (el popup vive en otra ventana). */
    private fun SemanticsNode.bottomOnScreen(): Float = positionOnScreen.y + size.height

    @Test
    fun ca_ui_02_01_tocarLaEtiquetaEnfocaElCampo() {
        mostrarRegistro()

        composeTestRule.onNodeWithText("Contraseña").performClick()

        composeTestRule.onNode(campoContrasena).assertIsFocused()
    }

    @Test
    fun ca_ui_02_02_siguientePasaAlSiguienteCampo() {
        mostrarRegistro()

        composeTestRule.onNodeWithText("Nombre completo").performClick()
        composeTestRule.onNodeWithText("Nombre completo").performImeAction()

        composeTestRule.onNode(hasText("Correo electrónico") and hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun ca_ui_03_01_elChecklistApareceEncimaDelCampoSinTaparlo() {
        mostrarRegistro()

        composeTestRule.onNode(campoContrasena).performClick()
        composeTestRule.waitForIdle()

        val popup = composeTestRule.onNodeWithText("Mínimo 8 caracteres").fetchSemanticsNode()
        val campo = composeTestRule.onNode(campoContrasena).fetchSemanticsNode()
        assertTrue(
            "El checklist debe quedar encima del campo (popup=${popup.bottomOnScreen()}, campo=${campo.positionOnScreen.y})",
            popup.bottomOnScreen() <= campo.positionOnScreen.y
        )
    }

    @Test
    fun ca_ui_03_05_elChecklistDesapareceAlCambiarDeCampo() {
        mostrarRegistro()

        composeTestRule.onNode(campoContrasena).performClick()
        composeTestRule.onAllNodesWithText("Mínimo 8 caracteres").assertCountEquals(1)

        composeTestRule.onNode(hasText("Correo electrónico") and hasSetTextAction()).performClick()

        composeTestRule.onAllNodesWithText("Mínimo 8 caracteres").assertCountEquals(0)
    }

    @Test
    fun ca_ui_03_02_enElPaso3ElChecklistIncluyeCoincidenciaYNoEstaFijo() {
        composeTestRule.setContent {
            SmartfoodTheme {
                PasswordRecoveryScreen(
                    state = PasswordRecoveryUiState(step = RecoveryStep.NEW_PASSWORD),
                    onEvent = {},
                    onNavigateToLogin = {}
                )
            }
        }

        // CA-UI-03-06: sin foco no hay lista de requisitos fija en el formulario.
        composeTestRule.onAllNodesWithText("¿Coinciden?").assertCountEquals(0)

        composeTestRule.onNode(hasText("Confirmar Contraseña") and hasSetTextAction()).performClick()

        composeTestRule.onAllNodesWithText("¿Coinciden?").assertCountEquals(1)
    }

    @Test
    fun ca_ui_03_04_enElPaso3ElChecklistQuedaEncimaDeAmbosCampos() {
        // A pantalla completa del dispositivo (no con un tamaño forzado), con el teclado real abriéndose.
        composeTestRule.setContent {
            SmartfoodTheme {
                PasswordRecoveryScreen(
                    state = PasswordRecoveryUiState(step = RecoveryStep.NEW_PASSWORD),
                    onEvent = {},
                    onNavigateToLogin = {}
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000)  // deja que el scaffold confirme el perfil

        listOf("Nueva Contraseña", "Confirmar Contraseña").forEach { etiqueta ->
            val campo = hasText(etiqueta) and hasSetTextAction()
            composeTestRule.onNode(campo).performClick()
            composeTestRule.mainClock.advanceTimeBy(1_000)
            composeTestRule.waitForIdle()

            val popup = composeTestRule.onNodeWithText("Mínimo 8 caracteres").fetchSemanticsNode()
            val nodoCampo = composeTestRule.onNode(campo).fetchSemanticsNode()
            assertTrue(
                "En \"$etiqueta\" el checklist debe quedar encima (popup=${popup.bottomOnScreen()}, " +
                    "campo=${nodoCampo.positionOnScreen.y})",
                popup.bottomOnScreen() <= nodoCampo.positionOnScreen.y
            )
        }
    }
}
