package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Pruebas instrumentadas de confirmación de guardado (spec 010, T015, CA-SAV-03-04 y CA-SAV-04-02). */
class OwnRecipeSavedScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun caSav0304_muestraAccionesDeRecetaCreada() {
        var viewed = false
        var another = false
        composeTestRule.setContent {
            SmartfoodTheme { OwnRecipeSavedScreen("Arroz", false, { viewed = true }, { another = true }, {}) }
        }

        composeTestRule.onNodeWithText("¡Receta guardada!").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"Arroz\" ya está en Creadas por mí.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver receta").performClick()
        composeTestRule.onNodeWithText("Crear otra").performClick()
        assertEquals(true, viewed)
        assertEquals(true, another)
    }
}
