package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.Store
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: OwnRecipeDetailScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Detalle de solo lectura de una receta propia (spec 010, T015, CA-SAV-04-05).
 */
class OwnRecipeDetailScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val arroz = Ingredient(
        id = "arroz",
        name = "Arroz",
        section = "Granos",
        unit = IngredientUnit.GRAM,
        pack = 500.0,
        prices = mapOf(Store.D1 to 3_000L),
    )

    private val estado = OwnRecipeDetailUiState(
        isLoading = false,
        recipe = OwnRecipe(
            id = "r1",
            name = "Arroz casero",
            category = RecipeCategory.ARROCES,
            ingredients = listOf(RecipeIngredient("arroz", 200.0)),
            cookMinutes = 30,
            preparation = "Cocinar y servir.",
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        ),
        ingredients = mapOf("arroz" to arroz),
        isFavorite = true,
    )

    @Test
    fun caSav0405_muestraLosDatosConElNombreDelIngrediente() {
        composeTestRule.setContent { SmartfoodTheme { OwnRecipeDetailScreen(estado, {}, {}, {}) } }

        composeTestRule.onNodeWithText("Arroces · 2 porciones · 30 min").assertIsDisplayed()
        composeTestRule.onNodeWithText("Arroz").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("200 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cocinar y servir.").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cocinar").assertDoesNotExist()
    }

    @Test
    fun caSav0405_tieneCorazonYEditar() {
        var favorito = false
        var editar = false
        composeTestRule.setContent {
            SmartfoodTheme { OwnRecipeDetailScreen(estado, { favorito = true }, { editar = true }, {}) }
        }

        composeTestRule.onNodeWithContentDescription("Quitar de favoritas").performClick()
        composeTestRule.onNodeWithText("Editar").performScrollTo().performClick()

        assertTrue(favorito)
        assertTrue(editar)
    }
}
