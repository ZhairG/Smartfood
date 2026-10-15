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
 * ícono según el perfil de FormMetrics.
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

    @Test
    fun ca_ui_02_08_sinEtiquetaSeAnunciaConElPlaceholder() {
        mostrar { SmartFoodTextField(value = "Carne", onValueChange = {}, placeholder = "Ej. Carne") }

        // Con un valor escrito el placeholder no se ve, pero el campo sigue teniendo nombre (#62).
        composeTestRule.onNode(hasSetTextAction()).assert(hasContentDescription("Ej. Carne"))
    }

    @Test
    fun ca_ui_02_08_sinEtiquetaConAccessibilityLabelSeAnunciaConEse() {
        mostrar {
            SmartFoodTextField(
                value = "",
                onValueChange = {},
                placeholder = "Ej. Carne",
                accessibilityLabel = "Nombre del producto"
            )
        }

        composeTestRule.onNode(hasSetTextAction()).assert(hasContentDescription("Nombre del producto"))
    }

    @Test
    fun ca_ui_02_08_selectorAnunciaEtiquetaYValorEnUnSoloNodo() {
        mostrar { SmartFoodSelectorField(label = "Categoría", value = "Lácteos", onClick = {}) }

        composeTestRule.onNodeWithText("Categoría")
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasText("Lácteos"))
    }

    @Test
    fun ca_ui_02_01_tocarLaEtiquetaDelSelectorLoAbre() {
        var toques = 0
        mostrar { SmartFoodSelectorField(label = "Categoría", value = "", onClick = { toques++ }, placeholder = "Elegir") }

        composeTestRule.onNodeWithText("Categoría", useUnmergedTree = true).performClick()

        assertEquals(1, toques)
    }

    @Test
    fun t023_iconoConElTamanoDelPerfil() {
        mostrar {
            Column {
                SmartFoodTextField(
                    value = "",
                    onValueChange = {},
                    leadingContent = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Regular") }
                )
                CompositionLocalProvider(LocalFormMetrics provides FormMetrics.Compact) {
                    SmartFoodTextField(
                        value = "",
                        onValueChange = {},
                        leadingContent = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Compact") }
                    )
                }
            }
        }

        // Figma "Añadir Producto" 476:3664: ícono de 30 px.
        composeTestRule.onNodeWithContentDescription("Regular", useUnmergedTree = true)
            .assertWidthIsEqualTo(FormMetrics.Regular.fieldIconSize)
        composeTestRule.onNodeWithContentDescription("Compact", useUnmergedTree = true)
            .assertWidthIsEqualTo(FormMetrics.Compact.fieldIconSize)
    }

    @Test
    fun ca_07_04_sinTipoElCampoNoParticipaDelAutocompletado() {
        mostrar { SmartFoodTextField(label = "Correo", value = "", onValueChange = {}) }

        composeTestRule.onNodeWithText("Correo").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentType))
    }
}
