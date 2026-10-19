package com.smartfood.app.feature.profile.ui.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.MealBudget
import com.smartfood.app.feature.profile.domain.model.SpiceLevel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: FoodPreferencesScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [FoodPreferencesScreen] sin estado (spec 008, HU-PER-03, T013; Figma
 * `376:1260`, `593:1976` y `593:1898`): los cuatro grupos con sus chips, las elecciones guardadas resaltadas, el evento
 * de cada chip, "Guardar Preferencias" deshabilitado sin cambios y los avisos de confirmación y guardado.
 */
class FoodPreferencesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<FoodPreferencesEvent>()

    private val elegidas = FoodPreferences(
        restrictions = setOf(DietaryRestriction.VEGETARIAN),
        cuisines = setOf(Cuisine.ITALIAN),
        spice = SpiceLevel.MEDIUM,
        budget = MealBudget.MODERATE,
    )

    private fun estado(
        saved: FoodPreferences = elegidas,
        draft: FoodPreferences = saved,
        dialog: FoodPreferencesDialog? = null,
    ) = FoodPreferencesUiState(
        isLoading = false,
        saved = saved,
        draft = draft,
        dialog = dialog
    )

    private fun mostrar(state: FoodPreferencesUiState) {
        composeTestRule.setContent {
            SmartfoodTheme { FoodPreferencesScreen(state = state, onEvent = { eventos += it }) }
        }
    }

    private fun botonGuardar() = composeTestRule.onNodeWithText("Guardar Preferencias")

    @Test
    fun caPer0301_muestraLosCuatroGruposConSusChipsYResaltaLoGuardado() {
        mostrar(estado())

        composeTestRule.onNodeWithText("Preferencias alimentarias").assertIsDisplayed()
        listOf("Restricciones alimentarias", "Cocina Favorita", "Nivel de Picante", "Presupuesto por comida")
            .forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }

        listOf("Vegetariano", "Vegano", "Sin gluten", "Sin lácteos", "Sin frutos secos").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
        listOf("Colombiana", "Italiana", "Asiática", "Mexicana", "Mediterránea").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
        listOf("Suave", "Medio", "Picante", "Económico", "Moderado", "Sin restricción").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }

        composeTestRule.onNodeWithText("Vegetariano").assertIsSelected()
        composeTestRule.onNodeWithText("Italiana").assertIsSelected()
        composeTestRule.onNodeWithText("Medio").assertIsSelected()
        composeTestRule.onNodeWithText("Moderado").assertIsSelected()
        composeTestRule.onNodeWithText("Vegano").assertIsNotSelected()
        composeTestRule.onNodeWithText("Picante").assertIsNotSelected()
    }

    @Test
    fun caPer0301_tocarUnChipEmiteElEventoDeSuGrupo() {
        mostrar(estado(saved = FoodPreferences(), draft = FoodPreferences()))

        composeTestRule.onNodeWithText("Vegano").performClick()
        composeTestRule.onNodeWithText("Asiática").performClick()
        composeTestRule.onNodeWithText("Picante").performClick()
        composeTestRule.onNodeWithText("Económico").performClick()

        assertEquals(
            listOf(
                FoodPreferencesEvent.RestrictionToggled(DietaryRestriction.VEGAN),
                FoodPreferencesEvent.CuisineToggled(Cuisine.ASIAN),
                FoodPreferencesEvent.SpiceSelected(SpiceLevel.HOT),
                FoodPreferencesEvent.BudgetSelected(MealBudget.ECONOMIC)
            ),
            eventos
        )
    }

    @Test
    fun caPer0303_guardarSeVeDeshabilitadoSinCambios() {
        mostrar(estado(saved = elegidas, draft = elegidas))

        botonGuardar().assertIsNotEnabled()
    }

    @Test
    fun caPer0303_guardarHabilitadoEmiteSaveClicked() {
        val draft = elegidas.copy(spice = SpiceLevel.HOT)
        mostrar(estado(saved = elegidas, draft = draft))

        botonGuardar().assertIsEnabled().performClick()

        assertEquals(listOf(FoodPreferencesEvent.SaveClicked), eventos)
    }

    @Test
    fun caPer0303_laPreguntaConfirmaOCancelaElGuardado() {
        mostrar(estado(dialog = FoodPreferencesDialog.ConfirmSave))

        composeTestRule.onNodeWithText("¿Desea guardar los cambios?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.onNodeWithText("Guardar").performClick()

        assertEquals(
            listOf(FoodPreferencesEvent.DialogDismissed, FoodPreferencesEvent.SaveConfirmed),
            eventos
        )
    }

    @Test
    fun caPer0303_elAvisoDeGuardadoMuestraElResultado() {
        mostrar(estado(dialog = FoodPreferencesDialog.Saved))

        composeTestRule.onNodeWithText("Guardado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferencias se actualizó").assertIsDisplayed()
    }
}
