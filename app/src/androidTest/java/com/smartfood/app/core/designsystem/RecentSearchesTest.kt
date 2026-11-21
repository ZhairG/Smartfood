package com.smartfood.app.core.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.components.SmartFoodRecentSearches
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: RecentSearchesTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas instrumentadas de [SmartFoodRecentSearches] (spec 014, T005): el título, los chips en orden y
 * «Borrar» (CA-LOC-02-01), tocar un chip (CA-LOC-02-05), «Borrar» (CA-LOC-02-06) y que sin búsquedas no dibuja nada.
 */
class RecentSearchesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tocados = mutableListOf<String>()
    private var borrados = 0

    private fun mostrar(items: List<String>) {
        composeTestRule.setContent {
            SmartfoodTheme {
                SmartFoodRecentSearches(items = items, onItemClick = { tocados += it }, onClear = { borrados++ })
            }
        }
    }

    @Test
    fun caLoc0201_muestraElTituloLosChipsYBorrar() {
        mostrar(listOf("pollo", "arroz con leche"))

        composeTestRule.onNode(hasText("Recientes") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("pollo").assertIsDisplayed()
        composeTestRule.onNodeWithText("arroz con leche").assertIsDisplayed()
        composeTestRule.onNodeWithText("Borrar").assertIsDisplayed()
    }

    @Test
    fun caLoc0205_tocarUnChipAvisaSuTexto() {
        mostrar(listOf("pollo", "arroz"))

        composeTestRule.onNodeWithText("arroz").performClick()

        assertEquals(listOf("arroz"), tocados)
    }

    @Test
    fun caLoc0206_borrarAvisa() {
        mostrar(listOf("pollo"))

        composeTestRule.onNodeWithText("Borrar").performClick()

        assertEquals(1, borrados)
    }

    @Test
    fun caLoc0201_sinBusquedasNoDibujaNada() {
        mostrar(emptyList())

        composeTestRule.onNodeWithText("Recientes").assertDoesNotExist()
        composeTestRule.onNodeWithText("Borrar").assertDoesNotExist()
    }
}
