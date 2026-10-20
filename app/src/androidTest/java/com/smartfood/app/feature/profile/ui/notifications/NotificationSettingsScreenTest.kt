package com.smartfood.app.feature.profile.ui.notifications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.profile.domain.model.NotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: NotificationSettingsScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [NotificationSettingsScreen] sin estado (spec 008, HU-PER-04, T017; Figma
 * `593:2079`): los tres avisos con su descripción y sus interruptores, el evento de cada interruptor y el aviso del
 * permiso negado con "Abrir ajustes" (CA-PER-04-01 a 04-06).
 */
class NotificationSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<NotificationSettingsEvent>()

    private fun estado(
        settings: NotificationSettings = NotificationSettings(),
        dialog: NotificationSettingsDialog? = null,
    ) = NotificationSettingsUiState(isLoading = false, settings = settings, dialog = dialog)

    private fun mostrar(state: NotificationSettingsUiState) {
        composeTestRule.setContent {
            SmartfoodTheme { NotificationSettingsScreen(state = state, onEvent = { eventos += it }) }
        }
    }

    @Test
    fun caPer0401_muestraLosTresAvisosConSuDescripcionYEncendidos() {
        mostrar(estado())

        composeTestRule.onNodeWithText("Notificaciones").assertIsDisplayed()
        listOf(
            "Productos por vencer",
            "Avisos cuando un ingrediente esté por caducar",
            "Recetas sugeridas",
            "Nuevas ideas según tu inventario",
            "Inventario vacío",
            "Recordatorio si no registras productos"
        ).forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }

        composeTestRule.onNodeWithContentDescription("Productos por vencer").assertIsOn()
        composeTestRule.onNodeWithContentDescription("Recetas sugeridas").assertIsOn()
        composeTestRule.onNodeWithContentDescription("Inventario vacío").assertIsOn()
    }

    @Test
    fun caPer0402_tocarUnInterruptorEmiteElCambioSinBotonGuardar() {
        mostrar(estado())

        composeTestRule.onNodeWithContentDescription("Recetas sugeridas").performClick()

        assertEquals(
            listOf(NotificationSettingsEvent.ToggleRequested(NotificationSetting.SUGGESTED_RECIPES, false)),
            eventos
        )
    }

    @Test
    fun caPer0402_encenderUnInterruptorApagadoEmiteElCambio() {
        mostrar(estado(settings = NotificationSettings(expiringProducts = false)))

        composeTestRule.onNodeWithContentDescription("Productos por vencer").assertIsOff().performClick()

        assertEquals(
            listOf(NotificationSettingsEvent.ToggleRequested(NotificationSetting.EXPIRING_PRODUCTS, true)),
            eventos
        )
    }

    @Test
    fun caPer0406_elAvisoDelPermisoPermiteAbrirAjustesOCancelar() {
        mostrar(estado(dialog = NotificationSettingsDialog.PermissionDenied))

        composeTestRule.onNodeWithText("Notificaciones desactivadas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.onNodeWithText("Abrir ajustes").performClick()

        assertEquals(
            listOf(NotificationSettingsEvent.DialogDismissed, NotificationSettingsEvent.OpenSettingsClicked),
            eventos
        )
    }
}
