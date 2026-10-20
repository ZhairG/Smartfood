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
    fun test_01_campos_y_boton_se_muestran() {
        setContent(ChangePasswordUiState())

        composeTestRule.onNodeWithText("Cambiar contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña actual").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nueva Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmar Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Actualizar contraseña").assertIsNotEnabled()
    }

    @Test
    fun test_03_boton_se_habilita_cuando_los_campos_son_validos() {
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
    fun test_04_dialogo_de_exito_muestra_mensaje() {
        setContent(
            ChangePasswordUiState(isSavedSuccess = true)
        )

        composeTestRule.onNodeWithText("Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña se actualizó").assertIsDisplayed()
    }

    @Test
    fun test_05_contrasenas_no_coinciden_muestra_mensaje_error() {
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
    fun test_06_contrasena_actual_incorrecta_muestra_mensaje_error() {
        setContent(
            ChangePasswordUiState(
                currentPasswordError = AuthError.InvalidCredentials
            )
        )

        composeTestRule.onNodeWithText("La contraseña actual no es correcta").assertIsDisplayed()
    }
}
