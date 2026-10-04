package com.smartfood.app.feature.auth.ui.register

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.domain.model.AuthError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RegisterScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de UI de la pantalla stateless [RegisterScreen] (HU-01).
 */
class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(state: RegisterUiState = RegisterUiState(), onEvent: (RegisterEvent) -> Unit = {}) {
        composeTestRule.setContent {
            SmartfoodTheme {
                RegisterScreen(state = state, onEvent = onEvent, onNavigateToLogin = {})
            }
        }
    }

    private fun botonCrearCuenta() = composeTestRule.onNode(hasText("Crear Cuenta") and hasClickAction())

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

        composeTestRule.onNodeWithText("Contraseña").performClick()

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
}
