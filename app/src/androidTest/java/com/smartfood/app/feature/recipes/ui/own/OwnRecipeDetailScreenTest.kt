package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import org.junit.Rule
import org.junit.Test

/** Pruebas instrumentadas del detalle propio (spec 010, T015, CA-SAV-04-05). */
class OwnRecipeDetailScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun caSav0405_muestraDatosYAccionEditar() {
        val recipe = OwnRecipe("arroz", "Arroz casero", RecipeCategory.ARROCES, emptyList(), 30, "Cocinar", 1L, 2L)
        composeTestRule.setContent { SmartfoodTheme { OwnRecipeDetailScreen(recipe, {}, {}) } }

        composeTestRule.onNodeWithText("Arroz casero").assertIsDisplayed()
        composeTestRule.onNodeWithText("ARROCES · 2 porciones · 30 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cocinar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Editar").assertIsDisplayed()
    }
}
