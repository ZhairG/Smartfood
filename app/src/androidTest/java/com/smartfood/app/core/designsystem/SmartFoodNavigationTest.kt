package com.smartfood.app.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodBottomBar
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodTab
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SmartFoodNavigationTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de la barra inferior y del botón principal con ícono (spec 006, T026; CA-INV-09-02 y
 * CA-INV-02-02).
 */
class SmartFoodNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(content: @Composable () -> Unit) {
        composeTestRule.setContent { SmartfoodTheme { content() } }
    }

    @Test
    fun caInv0902_laPestanaActualEstaSeleccionadaYTocarOtraLlamaASuAccion() {
        var perfil = 0
        mostrar {
            SmartFoodBottomBar(
                tabs = listOf(
                    SmartFoodTab("Inventario", R.drawable.ic_figma_tab_inventory, selected = true, onClick = {}),
                    SmartFoodTab("Perfil", R.drawable.ic_figma_tab_profile, selected = false, onClick = { perfil++ }),
                )
            )
        }

        composeTestRule.onNodeWithText("Inventario")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        composeTestRule.onNodeWithText("Perfil").assertIsNotSelected().performClick()

        assertEquals(1, perfil)
    }

    @Test
    fun caInv0202_botonConIconoHabilitadoRespondeYDeshabilitadoNo() {
        var toques = 0
        mostrar {
            Column {
                SmartFoodPrimaryButton(text = "Añadir al Inventario", onClick = { toques++ }, leadingIcon = Icons.Filled.Add)
                SmartFoodPrimaryButton(text = "Deshabilitado", onClick = { toques++ }, enabled = false, leadingIcon = Icons.Filled.Add)
            }
        }

        composeTestRule.onNodeWithText("Añadir al Inventario").assertIsDisplayed().assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("Deshabilitado").assertIsNotEnabled()

        assertEquals(1, toques)
    }

    @Test
    fun caInv0902_unaPestanaDeshabilitadaNoResponde() {
        var recetas = 0
        mostrar {
            SmartFoodBottomBar(
                tabs = listOf(
                    SmartFoodTab("Inventario", R.drawable.ic_figma_tab_inventory, selected = true, onClick = {}),
                    SmartFoodTab(
                        "Recetas",
                        R.drawable.ic_figma_tab_recipes,
                        selected = false,
                        onClick = { recetas++ },
                        enabled = false
                    ),
                )
            )
        }

        composeTestRule.onNodeWithText("Recetas").assertIsNotEnabled().performClick()

        assertEquals(0, recetas)
    }
}
