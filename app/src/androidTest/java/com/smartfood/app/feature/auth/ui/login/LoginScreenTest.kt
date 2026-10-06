package com.smartfood.app.feature.auth.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: LoginScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de UI de la pantalla stateless [LoginScreen] (HU-02).
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(state: LoginUiState = LoginUiState(), onEvent: (LoginEvent) -> Unit = {}) {
        composeTestRule.setContent {
            SmartfoodTheme {
                LoginScreen(
                    state = state,
                    onEvent = onEvent,
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {}
                )
            }
        }
    }

    @Test
    fun pantallaLogin_muestraEncabezadoYCampos() {
        mostrar()

        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()
        composeTestRule.onNodeWithText("Correo electrónico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recuérdame").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iniciar Sesión").assertIsDisplayed()
    }

    @Test
    fun pantallaLogin_cuandoUsuarioEscribeCorreo_emiteEventoEmailChanged() {
        var lastEvent: LoginEvent? = null
        mostrar(onEvent = { lastEvent = it })

        composeTestRule.onNodeWithText("nombre@correo.com").performTextInput("test@correo.com")

        assertEquals(LoginEvent.EmailChanged("test@correo.com"), lastEvent)
    }

    @Test
    fun pantallaLogin_cuandoSeHaceClicEnIniciarSesion_emiteEventoLoginClicked() {
        var lastEvent: LoginEvent? = null
        mostrar(onEvent = { lastEvent = it })

        composeTestRule.onNodeWithText("Iniciar Sesión").performClick()

        assertEquals(LoginEvent.LoginClicked, lastEvent)
    }

    @Test
    fun ca0201_camposVacios_muestraMensaje() {
        mostrar(LoginUiState(error = AuthError.EmptyFields(setOf(AuthField.EMAIL, AuthField.PASSWORD))))

        composeTestRule.onNodeWithText("Los campos no pueden estar vacíos").assertIsDisplayed()
    }

    @Test
    fun ca0203_credencialesIncorrectas_muestraMensaje() {
        mostrar(LoginUiState(error = AuthError.InvalidCredentials))

        composeTestRule.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
    }

    @Test
    fun ca0601_sinConexion_muestraDialogoDeReconexion() {
        var lastEvent: LoginEvent? = null
        mostrar(LoginUiState(error = AuthError.Network), onEvent = { lastEvent = it })

        composeTestRule.onNodeWithText("Reconectar").performClick()

        assertEquals(LoginEvent.RetryConnectionClicked, lastEvent)
    }
}
