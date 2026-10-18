package com.smartfood.app.feature.auth.ui.register

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.ui.components.EmailAccountContentType
import com.smartfood.app.testutil.PantallaEnFlujo
import com.smartfood.app.testutil.conTipo
import com.smartfood.app.testutil.mostrado
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RegisterScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de UI de la pantalla stateless [RegisterScreen] (HU-01) y de los enlaces a los documentos
 * legales de la casilla (spec 005, HU-LEG-01). Una prueba recorre el formulario cambiando el `UiState` y otra la
 * casilla con sus enlaces (#105).
 */
class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var terminos = 0
    private var privacidad = 0

    private val flujo = PantallaEnFlujo<RegisterUiState, RegisterEvent>(
        composeTestRule,
        RegisterUiState()
    ) { state, onEvent ->
        SmartfoodTheme {
            RegisterScreen(
                state = state,
                onEvent = onEvent,
                onNavigateToLogin = {},
                onOpenTerms = { terminos++ },
                onOpenPrivacy = { privacidad++ }
            )
        }
    }

    /** Texto de la casilla, fuera del árbol fusionado para poder medir dónde quedó cada enlace. */
    private fun textoCasilla() = composeTestRule.onNodeWithText("Acepto los", substring = true, useUnmergedTree = true)

    /** Toca el centro de [fragmento] dentro del texto de la casilla. */
    private fun tocarEnTextoCasilla(fragmento: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        textoCasilla().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.first()
        val inicio = layout.layoutInput.text.text.indexOf(fragmento)
        check(inicio >= 0) { "No se encontró \"$fragmento\" en la casilla" }
        val caja = layout.getBoundingBox(inicio + fragmento.length / 2)
        textoCasilla().performTouchInput { click(Offset(caja.center.x, caja.center.y)) }
    }

    /** Ejecuta la acción del lector de pantalla con la etiqueta [etiqueta] sobre la casilla. */
    private fun accionLectorDePantalla(etiqueta: String) {
        val acciones = composeTestRule.onNodeWithText("Acepto los", substring = true)
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeTestRule.runOnIdle { acciones.first { it.label == etiqueta }.action() }
    }

    private fun botonCrearCuenta() = composeTestRule.onNode(hasText("Crear Cuenta") and hasClickAction())

    /** La etiqueta "Contraseña" es un texto aparte que no enfoca el campo; se toca el campo editable de tipo contraseña. */
    private fun campoContrasena() =
        composeTestRule.onNode(hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))

    @Test
    fun ca0103_0105_0106_0703_elFormularioPideLosDatosYHabilitaCrearCuentaConLosTerminos() {
        flujo.mostrar()
        composeTestRule.mostrado(
            "Crea tu cuenta", "Empieza a aprovechar cada ingrediente de tu cocina.", "Nombre completo",
            "Correo electrónico", "Contraseña"
        )
        // CA-07-03: el registro ofrece el nombre y el usuario, y pide una contraseña nueva.
        composeTestRule.onNodeWithText("Nombre completo").assert(conTipo(ContentType.PersonFullName))
        composeTestRule.onNodeWithText("Correo electrónico").assert(conTipo(EmailAccountContentType))
        campoContrasena().assert(conTipo(ContentType.NewPassword))
        composeTestRule.onNodeWithText("Ej. Pepito Perez").performTextInput("Pepito Perez")
        assertEquals("escribir el nombre", listOf<RegisterEvent>(RegisterEvent.FullNameChanged("Pepito Perez")), flujo.eventos)

        // CA-01-05: el botón solo se habilita con los términos aceptados.
        botonCrearCuenta().assertIsNotEnabled()
        flujo.paso(RegisterUiState(acceptedTerms = true))
        botonCrearCuenta().assertIsEnabled()

        // CA-01-06: cuenta existente.
        flujo.paso(RegisterUiState(acceptedTerms = true, error = AuthError.EmailAlreadyInUse))
        composeTestRule.onNodeWithText("Ya existe una cuenta registrada con este correo electrónico").assertIsDisplayed()

        // CA-01-03: al enfocar la contraseña aparece el checklist de requisitos.
        campoContrasena().performClick()
        composeTestRule.mostrado("Mínimo 8 caracteres", "Una letra mayúscula", "Un número y carácter especial")
    }

    @Test
    fun caLeg0101_0102_0104_0107_losEnlacesAbrenSuDocumentoYElRestoAlternaLaCasilla() {
        flujo.mostrar()

        // CA-LEG-01-01 y 01-02: cada enlace abre su documento sin alternar la casilla.
        tocarEnTextoCasilla("Términos y condiciones")
        tocarEnTextoCasilla("Política de privacidad")
        assertEquals("términos y privacidad", 1 to 1, terminos to privacidad)
        assertEquals("los enlaces no alternan la casilla", emptyList<RegisterEvent>(), flujo.eventos)

        // CA-LEG-01-04: el resto del texto alterna la casilla.
        tocarEnTextoCasilla("Acepto")
        assertEquals("tocar «Acepto»", listOf<RegisterEvent>(RegisterEvent.AcceptedTermsChanged(true)), flujo.eventos)
        assertEquals("«Acepto» no abre documentos", 1 to 1, terminos to privacidad)

        // CA-LEG-01-07: el lector de pantalla ofrece abrir ambos documentos.
        accionLectorDePantalla("Abrir Términos y condiciones")
        accionLectorDePantalla("Abrir Política de privacidad")
        assertEquals("acciones del lector de pantalla", 2 to 2, terminos to privacidad)
    }
}
