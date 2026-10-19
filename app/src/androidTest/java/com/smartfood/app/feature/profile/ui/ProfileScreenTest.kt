/**
 * Nombre: ProfileScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [ProfileScreen] sin estado (spec 006, CA-INV-09-03; Figma `306:369`): el
 * encabezado, las cifras, las opciones (spec 008, CA-PER-01-03) y "Cerrar Sesión". No cierra la sesión real del dispositivo:
 * solo comprueba el evento.
 */
package com.smartfood.app.feature.profile.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<ProfileEvent>()

    private fun mostrar(
        dialog: ProfileDialog? = null,
        themeMode: ThemeMode = ThemeMode.Default,
        isThemeSheetVisible: Boolean = false,
    ) {
        composeTestRule.setContent {
            SmartfoodTheme {
                ProfileScreen(
                    state = ProfileUiState(
                        isLoading = false,
                        firstName = "Pepito",
                        fullName = "Pepito Perez",
                        email = "pepito@correo.com",
                        initials = "PP",
                        inventoryCount = 34,
                        themeMode = themeMode,
                        isThemeSheetVisible = isThemeSheetVisible,
                        dialog = dialog
                    ),
                    onEvent = { eventos += it }
                )
            }
        }
    }

    @Test
    fun caInv0903_elEncabezadoMuestraNombreCorreoEIniciales() {
        mostrar()

        composeTestRule.onNodeWithText("Pepito Perez").assertIsDisplayed()
        composeTestRule.onNodeWithText("pepito@correo.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("PP").assertIsDisplayed()
    }

    @Test
    fun caInv0903_laTarjetaDeIngredientesMuestraElConteoReal() {
        mostrar()

        composeTestRule.onNodeWithText("34").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ingredientes en inventario").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recetas guardadas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ahorrados este mes").assertIsDisplayed()
    }

    @Test
    fun caPer0103_editarInformacionYLaCamaraAbrenLaPantallaYLasDemasSiguenDeshabilitadas() {
        mostrar()

        composeTestRule.onNode(hasText("Editar información personal") and hasClickAction()).performClick()
        composeTestRule.onNodeWithContentDescription("Cambiar foto de perfil").performClick()
        composeTestRule.onNode(hasText("Cambiar contraseña") and hasClickAction()).assertIsNotEnabled()

        assertEquals(listOf(ProfileEvent.EditProfileClicked, ProfileEvent.EditProfileClicked), eventos)
    }

    @Test
    fun caPer0401_laFilaDeNotificacionesAbreSuPantalla() {
        mostrar()

        composeTestRule.onNode(hasText("Notificaciones") and hasClickAction()).performClick()

        assertEquals(listOf<ProfileEvent>(ProfileEvent.NotificationsClicked), eventos)
    }

    @Test
    fun caPer0601_laFilaDeTemaMuestraElValorActualYAbreLaHoja() {
        mostrar(themeMode = ThemeMode.DARK)

        composeTestRule.onNodeWithText("Oscuro").assertIsDisplayed()
        composeTestRule.onNode(hasText("Tema de la app") and hasClickAction()).performClick()

        assertEquals(listOf<ProfileEvent>(ProfileEvent.ThemeClicked), eventos)
    }

    @Test
    fun caPer0602_laHojaEligeElTemaYConfirmaOCancela() {
        mostrar(isThemeSheetVisible = true)

        composeTestRule.onNodeWithText("Oscuro").performClick()
        composeTestRule.onNode(hasText("Guardar") and hasClickAction()).performClick()

        assertEquals(listOf<ProfileEvent>(ProfileEvent.ThemeSelected(ThemeMode.DARK)), eventos)
    }

    @Test
    fun caPer0602_cancelarLaHojaNoCambiaElTema() {
        mostrar(isThemeSheetVisible = true)

        composeTestRule.onNode(hasText("Cancelar") and hasClickAction()).performClick()

        assertEquals(listOf<ProfileEvent>(ProfileEvent.ThemeSheetDismissed), eventos)
    }

    @Test
    fun ca0503_cerrarSesionEmiteLogoutClicked() {
        mostrar()

        composeTestRule.onNode(hasText("Cerrar Sesión") and hasClickAction()).performClick()

        assertEquals(listOf<ProfileEvent>(ProfileEvent.LogoutClicked), eventos)
    }

    @Test
    fun ca0503_elAvisoDeCerrarSesionDelFigmaOfreceCerrarOCancelar() {
        mostrar(dialog = ProfileDialog.ConfirmLogout(hasPendingChanges = false))

        composeTestRule.onNodeWithText("¿Cerrar sesión?").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Tendrás que iniciar sesión nuevamente para acceder a tu inventario y tus recetas guardadas.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.onNodeWithText("Cerrar sesión").performClick()

        assertEquals(listOf(ProfileEvent.DialogDismissed, ProfileEvent.LogoutConfirmed), eventos)
    }

    @Test
    fun t051_caInv1007_conCambiosSinSubirElAvisoDiceQuePodrianPerderse() {
        mostrar(dialog = ProfileDialog.ConfirmLogout(hasPendingChanges = true))

        composeTestRule.onNodeWithText("¿Cerrar sesión?").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Tienes cambios sin subir. Si cierras sesión ahora, podrían perderse.")
            .assertIsDisplayed()
    }
}

