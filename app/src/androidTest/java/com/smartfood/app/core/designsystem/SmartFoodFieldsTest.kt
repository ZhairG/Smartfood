package com.smartfood.app.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.components.SmartFoodPasswordField
import com.smartfood.app.core.designsystem.components.SmartFoodSelectorField
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
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
 * etiqueta no debe cerrar la app.
 */
class SmartFoodFieldsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(content: @Composable () -> Unit) {
        composeTestRule.setContent { SmartfoodTheme { content() } }
    }

    @Test
    fun ca_ui_02_08_conEtiquetaSeDibujaYLaEtiquetaEsElCampoEditable() {
        mostrar {
            SmartFoodTextField(label = "Correo", value = "", onValueChange = {}, placeholder = "correo@ejemplo.com")
        }

        composeTestRule.onNodeWithText("Correo").assertIsDisplayed().assert(hasSetTextAction())
    }

    @Test
    fun ca_ui_02_08_campoDeContrasenaConEtiquetaSeDibuja() {
        mostrar {
            SmartFoodPasswordField(label = "Contraseña", value = "", onValueChange = {})
        }

        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed().assert(hasSetTextAction())
    }

    @Test
    fun t023_sinEtiquetaConIconoMuestraPlaceholderEIcono() {
        mostrar {
            SmartFoodTextField(
                value = "",
                onValueChange = {},
                placeholder = "Ej. Carne",
                leadingContent = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Producto") }
            )
        }

        composeTestRule.onNodeWithText("Ej. Carne").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Producto", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun t024_selectorMuestraPlaceholderYAlTocarLlamaOnClick() {
        var toques = 0
        mostrar {
            SmartFoodSelectorField(value = "", onClick = { toques++ }, placeholder = "Seleccionar categoría")
        }

        composeTestRule.onNodeWithText("Seleccionar categoría")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(1, toques)
    }

    @Test
    fun t024_selectorConValorMuestraElValorYLaEtiqueta() {
        mostrar {
            SmartFoodSelectorField(label = "Categoría", value = "Lácteos", onClick = {}, placeholder = "Elegir")
        }

        composeTestRule.onNodeWithText("Categoría").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lácteos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elegir").assertDoesNotExist()
    }
}
