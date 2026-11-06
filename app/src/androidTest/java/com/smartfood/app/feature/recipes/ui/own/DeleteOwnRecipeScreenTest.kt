package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
 * Nombre: DeleteOwnRecipeScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: "Eliminar receta" sin estado (spec 010, T018, CA-SAV-05-01 y 05-02).
 */
class DeleteOwnRecipeScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val acciones = mutableListOf<String>()

    private fun mostrar(estado: DeleteOwnRecipeUiState = DeleteOwnRecipeUiState()) {
        composeTestRule.setContent {
            SmartfoodTheme {
                DeleteOwnRecipeScreen(
                    name = "Pasta verde rápida",
                    state = estado,
                    onConfirm = { acciones += "confirmar" },
                    onCancel = { acciones += "cancelar" },
                )
            }
        }
    }

    @Test
    fun caSav0501_muestraLaPreguntaElTextoYSusBotones() {
        mostrar()

        composeTestRule.onNode(hasText("Eliminar receta") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("¿Eliminar “Pasta verde rápida”?").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Esta acción quitará la receta de tus creaciones. No afecta los ingredientes del inventario.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Sí, eliminar").performClick()

        assertEquals(listOf("confirmar"), acciones)
    }

    @Test
    fun caSav0502_cancelarYLaFlechaVuelvenSinEliminar() {
        mostrar()

        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(listOf("cancelar", "cancelar"), acciones)
    }

    @Test
    fun caSav0503_mientrasEliminaNoSePuedeRepetir() {
        mostrar(DeleteOwnRecipeUiState(isDeleting = true))

        composeTestRule.onNodeWithText("Sí, eliminar").assertIsNotEnabled()
    }
}
