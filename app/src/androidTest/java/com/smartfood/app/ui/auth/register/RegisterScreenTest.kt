package com.smartfood.app.ui.auth.register

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.smartfood.app.ui.auth.AuthErrorType
import com.smartfood.app.ui.auth.AuthEvent
import com.smartfood.app.ui.auth.AuthUiState
import com.smartfood.app.ui.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RegisterScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas de UI con Compose Test Rule para la pantalla de Registro [RegisterScreen].
 */
class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pantallaRegistro_muestraEncabezadoYCampos() {
        composeTestRule.setContent {
            SmartfoodTheme {
                RegisterScreen(
                    state = AuthUiState(),
                    onEvent = {},
                    onNavigateToLogin = {},
                    onNavigateToSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Empieza a aprovechar cada ingrediente de tu cocina.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nombre completo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Correo electrónico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crear Cuenta").assertIsDisplayed()
    }

    @Test
    fun pantallaRegistro_cuandoCampoContrasenaEnfocado_muestraChecklistDeRequisitos() {
        composeTestRule.setContent {
            SmartfoodTheme {
                RegisterScreen(
                    state = AuthUiState(),
                    onEvent = {},
                    onNavigateToLogin = {},
                    onNavigateToSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Contraseña").performClick()

        composeTestRule.onNodeWithText("Mínimo 8 caracteres").assertIsDisplayed()
        composeTestRule.onNodeWithText("Una letra mayúscula").assertIsDisplayed()
        composeTestRule.onNodeWithText("Un número y carácter especial").assertIsDisplayed()
    }

    @Test
    fun pantallaRegistro_cuandoUsuarioEscribeNombre_emiteEventoFullNameChanged() {
        var lastEvent: AuthEvent? = null

        composeTestRule.setContent {
            SmartfoodTheme {
                RegisterScreen(
                    state = AuthUiState(),
                    onEvent = { lastEvent = it },
                    onNavigateToLogin = {},
                    onNavigateToSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Ej. Pepito Perez").performTextInput("Pepito Perez")

        assertEquals(AuthEvent.FullNameChanged("Pepito Perez"), lastEvent)
    }

    @Test
    fun pantallaRegistro_cuandoErrorCuentaExistente_muestraMensajeDeError() {
        composeTestRule.setContent {
            SmartfoodTheme {
                RegisterScreen(
                    state = AuthUiState(
                        errorType = AuthErrorType.ACCOUNT_ALREADY_EXISTS,
                        emailHasError = true,
                    ),
                    onEvent = {},
                    onNavigateToLogin = {},
                    onNavigateToSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Ya existe una cuenta registrada con este correo electrónico").assertIsDisplayed()
    }
}
