package com.smartfood.app.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SmartFoodTopBarTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de la barra superior de las pantallas internas (spec 006, CA-INV-09-04, RNF-INV-04).
 */
class SmartFoodTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun caInv0904_tituloEsEncabezadoYLaFlechaVuelve() {
        var atras = 0
        composeTestRule.setContent {
            SmartfoodTheme { SmartFoodTopBar(title = "Foto del producto", onBack = { atras++ }) }
        }

        composeTestRule.onNodeWithText("Foto del producto")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        // RNF-INV-04: el botón de solo ícono tiene descripción.
        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(1, atras)
    }

    @Test
    fun laAccionOpcionalSeMuestraYResponde() {
        var guardar = 0
        composeTestRule.setContent {
            SmartfoodTheme {
                SmartFoodTopBar(
                    title = "Editar Producto",
                    onBack = {},
                    action = { TextButton(onClick = { guardar++ }) { Text("Guardar") } }
                )
            }
        }

        composeTestRule.onNodeWithText("Guardar").performClick()

        assertEquals(1, guardar)
    }
}
