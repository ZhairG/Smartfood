package com.smartfood.app.ui.auth.login

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
 * Nombre: LoginScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Pruebas de UI con Compose Test Rule para la pantalla [LoginScreen].
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pantallaLogin_muestraEncabezadoYCampos() {
        composeTestRule.setContent {
            SmartfoodTheme {
                LoginScreen(
                    state = AuthUiState(),
                    onEvent = {},
                    onNavigateToRegister = {},
                    onNavigateToRecovery = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inicia sesión para continuar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Correo electrónico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iniciar Sesión").assertIsDisplayed()
    }

    @Test
    fun pantallaLogin_cuandoUsuarioEscribeCorreo_emiteEvento() {
        var lastEvent: AuthEvent? = null

        composeTestRule.setContent {
            SmartfoodTheme {
                LoginScreen(
                    state = AuthUiState(),
                    onEvent = { lastEvent = it },
                    onNavigateToRegister = {},
                    onNavigateToRecovery = {}
                )
            }
        }

        composeTestRule.onNodeWithText("tu@email.com").performTextInput("test@correo.com")

        assertEquals(AuthEvent.EmailChanged("test@correo.com"), lastEvent)
    }

    @Test
    fun pantallaLogin_cuandoSeHaceClicEnIniciarSesion_emiteEventoLoginClicked() {
        var lastEvent: AuthEvent? = null

        composeTestRule.setContent {
            SmartfoodTheme {
                LoginScreen(
                    state = AuthUiState(),
                    onEvent = { lastEvent = it },
                    onNavigateToRegister = {},
                    onNavigateToRecovery = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Iniciar Sesión").performClick()

        assertEquals(AuthEvent.LoginClicked, lastEvent)
    }

    @Test
    fun pantallaLogin_cuandoHayEstadoDeError_muestraMensajeDeError() {
        composeTestRule.setContent {
            SmartfoodTheme {
                LoginScreen(
                    state = AuthUiState(
                        errorType = AuthErrorType.CREDENTIALS_ERROR,
                        emailHasError = true,
                        passwordHasError = true
                    ),
                    onEvent = {},
                    onNavigateToRegister = {},
                    onNavigateToRecovery = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
    }
}
