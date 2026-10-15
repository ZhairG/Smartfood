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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RegisterScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de UI de la pantalla stateless [RegisterScreen] (HU-01) y de los enlaces a los documentos
 * legales de la casilla (spec 005, HU-LEG-01).
 */
class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(
        state: RegisterUiState = RegisterUiState(),
        onEvent: (RegisterEvent) -> Unit = {},
        onOpenTerms: () -> Unit = {},
        onOpenPrivacy: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            SmartfoodTheme {
                RegisterScreen(
                    state = state,
                    onEvent = onEvent,
                    onNavigateToLogin = {},
                    onOpenTerms = onOpenTerms,
                    onOpenPrivacy = onOpenPrivacy
                )
            }
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
    fun pantallaRegistro_muestraEncabezadoYCampos() {
        mostrar()

        composeTestRule.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Empieza a aprovechar cada ingrediente de tu cocina.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nombre completo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Correo electrónico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
    }

    @Test
    fun ca0103_cuandoCampoContrasenaEnfocado_muestraChecklistDeRequisitos() {
        mostrar()

        campoContrasena().performClick()

        composeTestRule.onNodeWithText("Mínimo 8 caracteres").assertIsDisplayed()
        composeTestRule.onNodeWithText("Una letra mayúscula").assertIsDisplayed()
        composeTestRule.onNodeWithText("Un número y carácter especial").assertIsDisplayed()
    }

    @Test
    fun pantallaRegistro_cuandoUsuarioEscribeNombre_emiteEventoFullNameChanged() {
        var lastEvent: RegisterEvent? = null
        mostrar(onEvent = { lastEvent = it })

        composeTestRule.onNodeWithText("Ej. Pepito Perez").performTextInput("Pepito Perez")

        assertEquals(RegisterEvent.FullNameChanged("Pepito Perez"), lastEvent)
    }

    @Test
    fun ca0105_sinAceptarTerminos_botonDeshabilitado() {
        mostrar(RegisterUiState(acceptedTerms = false))

        botonCrearCuenta().assertIsNotEnabled()
    }

    @Test
    fun ca0105_conTerminosAceptados_botonHabilitado() {
        mostrar(RegisterUiState(acceptedTerms = true))

        botonCrearCuenta().assertIsEnabled()
    }

    @Test
    fun ca0106_cuentaExistente_muestraMensaje() {
        mostrar(RegisterUiState(error = AuthError.EmailAlreadyInUse))

        composeTestRule.onNodeWithText("Ya existe una cuenta registrada con este correo electrónico").assertIsDisplayed()
    }

    @Test
    fun caLeg0101_tocarTerminos_abreElDocumentoSinAlternarLaCasilla() {
        var aperturas = 0
        val eventos = mutableListOf<RegisterEvent>()
        mostrar(onEvent = { eventos += it }, onOpenTerms = { aperturas++ })

        tocarEnTextoCasilla("Términos y condiciones")

        assertEquals(1, aperturas)
        assertEquals(emptyList<RegisterEvent>(), eventos)
    }

    @Test
    fun caLeg0102_tocarPolitica_abreElDocumentoSinAlternarLaCasilla() {
        var aperturas = 0
        val eventos = mutableListOf<RegisterEvent>()
        mostrar(onEvent = { eventos += it }, onOpenPrivacy = { aperturas++ })

        tocarEnTextoCasilla("Política de privacidad")

        assertEquals(1, aperturas)
        assertEquals(emptyList<RegisterEvent>(), eventos)
    }

    @Test
    fun caLeg0104_tocarAceptoLos_alternaLaCasilla() {
        var aperturas = 0
        val eventos = mutableListOf<RegisterEvent>()
        mostrar(onEvent = { eventos += it }, onOpenTerms = { aperturas++ }, onOpenPrivacy = { aperturas++ })

        tocarEnTextoCasilla("Acepto")

        assertEquals(listOf<RegisterEvent>(RegisterEvent.AcceptedTermsChanged(true)), eventos)
        assertEquals(0, aperturas)
    }

    @Test
    fun caLeg0107_lectorDePantalla_ofreceAbrirAmbosDocumentos() {
        var terminos = 0
        var privacidad = 0
        mostrar(onOpenTerms = { terminos++ }, onOpenPrivacy = { privacidad++ })

        accionLectorDePantalla("Abrir Términos y condiciones")
        accionLectorDePantalla("Abrir Política de privacidad")

        assertEquals(1, terminos)
        assertEquals(1, privacidad)
    }

    /** El nodo editable declara [type] para el autocompletado del teléfono (spec 001, HU-07). */
    private fun conTipo(type: ContentType) = SemanticsMatcher.expectValue(SemanticsProperties.ContentType, type)

    @Test
    fun ca0703_registroOfreceUsuarioYPideContrasenaNueva() {
        mostrar()

        composeTestRule.onNodeWithText("Nombre completo").assert(conTipo(ContentType.PersonFullName))
        composeTestRule.onNodeWithText("Correo electrónico").assert(conTipo(EmailAccountContentType))
        campoContrasena().assert(conTipo(ContentType.NewPassword))
    }
}
