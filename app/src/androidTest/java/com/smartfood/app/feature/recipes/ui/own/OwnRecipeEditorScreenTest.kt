package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Rule
import org.junit.Test

/** Pruebas instrumentadas del editor propio (spec 010, T015, CA-SAV-03-01 y CA-SAV-04-01). */
class OwnRecipeEditorScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun caSav0301_muestraCamposPrincipalesDeNuevaReceta() {
        composeTestRule.setContent { SmartfoodTheme { OwnRecipeEditorScreen(OwnRecipeEditorUiState(), {}) } }

        composeTestRule.onNodeWithText("Nueva receta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nombre de la receta *").assertIsDisplayed()
        composeTestRule.onAllNodes(hasClickAction())[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Tiempo de cocción *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preparación *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardar receta").assertIsDisplayed()
    }
}
