package com.smartfood.app.feature.profile.ui.password

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.usecase.PasswordValidationResult
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: ChangePasswordScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de la UI stateless [ChangePasswordScreen] (spec 008, HU-PER-05, T022; Figma 603:2358).
 */
class ChangePasswordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<ChangePasswordEvent>()

    private fun setContent(state: ChangePasswordUiState) {
        composeTestRule.setContent {
            SmartfoodTheme {
                ChangePasswordScreen(
                    state = state,
                    onEvent = { events.add(it) },
                    onBack = {}
                )
            }
        }
    }

    @Test
    fun caPer0501_muestraLosTresCamposYElBoton() {
        setContent(ChangePasswordUiState())

        composeTestRule.onNodeWithText("Cambiar contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña actual").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nueva Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmar Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Actualizar contraseña").assertIsNotEnabled()
    }

    @Test
    fun caPer0502_focoEnNuevaContrasenaMuestraChecklist() {
        setContent(ChangePasswordUiState(newPassword = "pass"))

        composeTestRule.onNodeWithText("Nueva Contraseña").performClick()
        composeTestRule.onNodeWithText("Mínimo 8 caracteres").assertIsDisplayed()
    }

    @Test
    fun caPer0503_botonHabilitadoConCamposValidos() {
        setContent(
            ChangePasswordUiState(
                currentPassword = "Pass123!",
                newPassword = "NewPass123!",
                confirmPassword = "NewPass123!",
                passwordValidation = PasswordValidationResult(
                    hasMinimumLength = true,
                    hasUppercase = true,
                    hasSpecialCharacter = true
                )
            )
        )

        composeTestRule.onNodeWithText("Actualizar contraseña").assertIsEnabled()
        composeTestRule.onNodeWithText("Actualizar contraseña").performClick()

        assertTrue(events.contains(ChangePasswordEvent.SubmitClicked))
    }

    @Test
    fun caPer0504_dialogoDeExitoMuestraMensaje() {
        setContent(
            ChangePasswordUiState(isSavedSuccess = true)
        )

        composeTestRule.onNodeWithText("Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña se actualizó").assertIsDisplayed()
    }

    @Test
    fun caPer0503_contrasenasNoCoincidenMuestraError() {
        setContent(
            ChangePasswordUiState(
                currentPassword = "Pass123!",
                newPassword = "NewPass123!",
                confirmPassword = "Diferente123!",
                passwordValidation = PasswordValidationResult(
                    hasMinimumLength = true,
                    hasUppercase = true,
                    hasSpecialCharacter = true
                )
            )
        )

        composeTestRule.onNodeWithText("Las contraseñas no coinciden").assertIsDisplayed()
    }

    @Test
    fun caPer0505_contrasenaActualIncorrectaMuestraError() {
        setContent(
            ChangePasswordUiState(
                currentPasswordError = AuthError.InvalidCredentials
            )
        )

        composeTestRule.onNodeWithText("La contraseña actual no es correcta").assertIsDisplayed()
    }
}
