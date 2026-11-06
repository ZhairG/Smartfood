package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeDraft
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeField
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredient
import com.smartfood.app.feature.recipes.domain.model.Store
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: OwnRecipeEditorScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: "Nueva receta" y "Editar receta" sin estado (spec 010, T015, CA-SAV-03-01, 03-03, 03-06, 04-01 y 04-04).
 */
class OwnRecipeEditorScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<OwnRecipeEditorEvent>()

    private val tomate = Ingredient(
        id = "tomate",
        name = "Tomate",
        section = "Verduras",
        unit = IngredientUnit.UNIT,
        pack = 1.0,
        prices = mapOf(Store.D1 to 1_000L),
    )

    private fun mostrar(estado: OwnRecipeEditorUiState) {
        composeTestRule.setContent {
            SmartfoodTheme { OwnRecipeEditorScreen(state = estado, onEvent = { eventos += it }) }
        }
    }

    @Test
    fun caSav0301_nuevaRecetaMuestraSusCamposYSusAcciones() {
        mostrar(OwnRecipeEditorUiState())

        composeTestRule.onNode(hasText("Nueva receta") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nombre de la receta *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Categoría *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ingredientes").assertIsDisplayed()
        composeTestRule.onNodeWithText("＋ Añadir ingrediente").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tiempo de cocción *").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Preparación *").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Los campos con * son obligatorios. Revisa antes de guardar.")
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardar receta").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun caSav0303_laFilaMuestraNombreYCantidadYSeTocaParaCambiarla() {
        mostrar(
            OwnRecipeEditorUiState(
                catalog = listOf(tomate),
                draft = OwnRecipeDraft(ingredients = listOf(RecipeIngredient("tomate", 1.0))),
            )
        )

        composeTestRule.onNodeWithText("1 und").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomate").performClick()

        assertEquals(listOf<OwnRecipeEditorEvent>(OwnRecipeEditorEvent.IngredientClicked("tomate")), eventos)
    }

    @Test
    fun caSav0306_unCampoInvalidoMuestraElAvisoYSuMensaje() {
        mostrar(
            OwnRecipeEditorUiState(
                recipeId = "r1",
                draft = OwnRecipeDraft(name = "Pasta verde rápida", category = RecipeCategory.PASTAS, cookMinutes = "0"),
                error = OwnRecipeError.Invalid(OwnRecipeField.COOK_MINUTES),
            )
        )

        composeTestRule.onNodeWithText("⚠ No pudimos guardar los cambios").assertIsDisplayed()
        composeTestRule.onNodeWithText("Revisa el tiempo de cocción. Debe ser mayor a 0 minutos.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ingresa un valor entre 1 y 240 minutos.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun caSav0401_editarMuestraEliminarDescartarYSusDatos() {
        mostrar(
            OwnRecipeEditorUiState(
                recipeId = "r1",
                draft = OwnRecipeDraft(name = "Pasta verde rápida", category = RecipeCategory.PASTAS),
            )
        )

        composeTestRule.onNode(hasText("Editar receta") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Pastas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Eliminar").performClick()
        composeTestRule.onNodeWithText("Descartar cambios").performScrollTo().performClick()

        assertEquals(
            listOf(OwnRecipeEditorEvent.DeleteClicked, OwnRecipeEditorEvent.DiscardClicked),
            eventos
        )
    }

    @Test
    fun caSav0404_laFlechaDescartaSinGuardar() {
        mostrar(OwnRecipeEditorUiState())

        composeTestRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(listOf<OwnRecipeEditorEvent>(OwnRecipeEditorEvent.DiscardClicked), eventos)
    }
}
