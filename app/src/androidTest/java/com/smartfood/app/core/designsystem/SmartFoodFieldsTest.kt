package com.smartfood.app.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.components.SmartFoodPasswordField
import com.smartfood.app.core.designsystem.components.SmartFoodSelectorField
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.theme.FormMetrics
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SmartFoodFieldsTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de los campos del design system: el campo de texto con y sin etiqueta (spec 002,
 * CA-UI-02-08; spec 006, T023) y el selector (spec 006, T024). Cubre la regresión del Issue #60: un campo con
 * etiqueta no debe cerrar la app. Desde el #62: nombre accesible sin etiqueta, selector en un solo nodo y tamaño del
 * ícono según el perfil de FormMetrics. Desde el #105, cada prueba dibuja juntos los campos de un mismo caso.
 */
class SmartFoodFieldsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(content: @Composable () -> Unit) {
        composeTestRule.setContent { SmartfoodTheme { Column { content() } } }
    }

    @Test
    fun ca_ui_02_08_ca_07_04_conEtiquetaSeDibujanYLaEtiquetaEsElCampoEditableSinAutocompletado() {
        mostrar {
            SmartFoodTextField(label = "Correo", value = "", onValueChange = {}, placeholder = "correo@ejemplo.com")
            SmartFoodPasswordField(label = "Contraseña", value = "", onValueChange = {})
        }

        // Regresión del #60: un campo con etiqueta no cierra la app.
        composeTestRule.onNodeWithText("Correo").assertIsDisplayed().assert(hasSetTextAction())
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed().assert(hasSetTextAction())
        // CA-07-04: sin tipo, el campo no participa del autocompletado.
        composeTestRule.onNodeWithText("Correo").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentType))
    }

    @Test
    fun t023_ca_ui_02_08_sinEtiquetaSeAnuncianYElIconoTieneElTamanoDelPerfil() {
        mostrar {
            SmartFoodTextField(
                value = "",
                onValueChange = {},
                placeholder = "Ej. Carne",
                leadingContent = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Regular") }
            )
            SmartFoodTextField(value = "Pollo", onValueChange = {}, placeholder = "Ej. Fruta")
            SmartFoodTextField(value = "", onValueChange = {}, placeholder = "Ej. Leche", accessibilityLabel = "Nombre del producto")
            CompositionLocalProvider(LocalFormMetrics provides FormMetrics.Compact) {
                SmartFoodTextField(
                    value = "",
                    onValueChange = {},
                    leadingContent = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Compact") }
                )
            }
        }

        composeTestRule.onNodeWithText("Ej. Carne").assertIsDisplayed()
        // Con un valor escrito el placeholder no se ve, pero el campo sigue teniendo nombre (#62).
        composeTestRule.onNode(hasSetTextAction() and hasContentDescription("Ej. Fruta")).assertExists()
        composeTestRule.onNode(hasSetTextAction() and hasContentDescription("Nombre del producto")).assertExists()
        // Figma "Añadir Producto" 476:3664: ícono de 30 px.
        composeTestRule.onNodeWithContentDescription("Regular", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(FormMetrics.Regular.fieldIconSize)
        composeTestRule.onNodeWithContentDescription("Compact", useUnmergedTree = true)
            .assertWidthIsEqualTo(FormMetrics.Compact.fieldIconSize)
    }

    @Test
    fun t024_ca_ui_02_01_ca_ui_02_08_elSelectorEsUnBotonDeUnSoloNodoQueSeAbreDesdeLaEtiqueta() {
        var toquesVacio = 0
        var toquesConValor = 0
        mostrar {
            SmartFoodSelectorField(value = "", onClick = { toquesVacio++ }, placeholder = "Seleccionar categoría")
            SmartFoodSelectorField(label = "Categoría", value = "Lácteos", onClick = { toquesConValor++ }, placeholder = "Elegir")
        }

        composeTestRule.onNodeWithText("Seleccionar categoría")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals("tocar el selector vacío", 1, toquesVacio)

        // Con valor: se ven la etiqueta y el valor, no el placeholder, y se anuncian en un solo nodo.
        composeTestRule.onNodeWithText("Lácteos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elegir").assertDoesNotExist()
        composeTestRule.onNodeWithText("Categoría")
            .assertIsDisplayed()
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasText("Lácteos"))
        // CA-UI-02-01: tocar la etiqueta abre el selector.
        composeTestRule.onNodeWithText("Categoría", useUnmergedTree = true).performClick()
        assertEquals("tocar la etiqueta", 1, toquesConValor)
    }
}
