package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: OwnRecipeSavedScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: "¡Receta guardada!" al crear y al editar (spec 010, T015, CA-SAV-03-04, 03-07 y 04-02).
 */
class OwnRecipeSavedScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val acciones = mutableListOf<String>()

    private fun mostrar(edited: Boolean) {
        composeTestRule.setContent {
            SmartfoodTheme {
                OwnRecipeSavedScreen(
                    name = "Arroz",
                    edited = edited,
                    onView = { acciones += "ver" },
                    onCreateAnother = { acciones += "otra" },
                    onBack = { acciones += "volver" },
                )
            }
        }
    }

    @Test
    fun caSav0304_alCrearMuestraElMensajeYSusAcciones() {
        mostrar(edited = false)

        composeTestRule.onNode(hasText("Nueva receta") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("¡Receta guardada!").assertIsDisplayed()
        composeTestRule.onNodeWithText("“Arroz” ya está en Creadas por mí.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver receta").performClick()
        composeTestRule.onNodeWithText("Crear otra").performClick()
        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(listOf("ver", "otra", "volver"), acciones)
    }

    @Test
    fun caSav0402_alEditarDiceQueFueActualizada() {
        mostrar(edited = true)

        composeTestRule.onNode(hasText("Editar receta") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("“Arroz” ha sido actualizada.").assertIsDisplayed()
    }
}
