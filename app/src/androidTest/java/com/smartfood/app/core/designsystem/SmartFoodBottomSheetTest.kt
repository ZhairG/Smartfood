package com.smartfood.app.core.designsystem

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.components.SmartFoodBottomSheet
import com.smartfood.app.core.designsystem.theme.SmartFoodMotion
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SmartFoodBottomSheetTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: La hoja propia entra en 300 ms y su acción de cierre llega al terminar la salida (spec 007,
 * CA-ANI-04-01).
 */
class SmartFoodBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ca_ani_04_01_laHojaEntraY_laAccionDeCierreEsperaLaSalida() {
        var cierres = 0
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            SmartfoodTheme {
                SmartFoodBottomSheet(onDismissRequest = {}) {
                    Text("Seleccione una categoría")
                    Button(onClick = { close { cierres++ } }) { Text("Guardar") }
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(SmartFoodMotion.MEDIUM + 50L)
        composeTestRule.onNodeWithText("Seleccione una categoría").assertIsDisplayed()

        composeTestRule.onNodeWithText("Guardar").performClick()
        composeTestRule.mainClock.advanceTimeBy(SmartFoodMotion.MEDIUM / 2L)
        assertEquals("La acción no llega a mitad de la salida", 0, cierres)

        composeTestRule.mainClock.advanceTimeBy(SmartFoodMotion.MEDIUM + 50L)
        assertEquals("La acción llega una sola vez al terminar la salida", 1, cierres)
    }
}
