package com.smartfood.app.feature.recipes.ui.cooking

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.RecipeStep
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: CookingScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de las pantallas [CookingIntroScreen] y [CookingScreen] (spec 009, HU-RIA-04, T023; Figma 557:2819 y 557:2781).
 */
class CookingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<CookingEvent>()

    @Test
    fun caRia0403_antesDeEmpezarMuestraListaYComenzarGuia() {
        composeTestRule.setContent {
            SmartfoodTheme {
                CookingIntroScreen(
                    state = CookingUiState(
                        isLoading = false,
                        name = "Arepas con hogao",
                        beforeStarting = listOf("Ten listas las arepas", "Pica el tomate")
                    ),
                    onEvent = { events.add(it) },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Arepas con hogao, paso a paso").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prepara antes de iniciar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ten listas las arepas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Omitir video").assertIsDisplayed()
        composeTestRule.onNodeWithText("Comenzar guía").assertIsDisplayed()
        composeTestRule.onNodeWithText("Comenzar guía").performClick()

        assertTrue(events.contains(CookingEvent.StartGuideClicked))
    }

    @Test
    fun caRia0404_pasosDeCocinaYControlesAvanzanYRetroceden() {
        val steps = listOf(
            RecipeStep("Calentar arepas", "Calienta las arepas en una sartén", minutes = 5),
            RecipeStep("Servir", "Sirve el hogao sobre las arepas", minutes = null)
        )

        composeTestRule.setContent {
            SmartfoodTheme {
                CookingScreen(
                    state = CookingUiState(
                        isLoading = false,
                        name = "Arepas con hogao",
                        steps = steps,
                        currentStepIndex = 0,
                        timerSecondsRemaining = 300
                    ),
                    onEvent = { events.add(it) },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("PASO 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calentar arepas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anterior").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Siguiente").assertIsEnabled()
        composeTestRule.onNodeWithText("¿Necesitas ayuda? Ver técnica de sofrito").assertIsDisplayed()

        composeTestRule.onNodeWithText("Siguiente").performClick()
        assertTrue(events.contains(CookingEvent.NextStepClicked))
    }

    @Test
    fun caRia0405_dialogoConfirmarMuestraAlerta() {
        composeTestRule.setContent {
            SmartfoodTheme {
                CookingScreen(
                    state = CookingUiState(
                        isLoading = false,
                        isFinishConfirmVisible = true
                    ),
                    onEvent = { events.add(it) },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("¿Cocinaste esta receta?").assertIsDisplayed()
    }
}
