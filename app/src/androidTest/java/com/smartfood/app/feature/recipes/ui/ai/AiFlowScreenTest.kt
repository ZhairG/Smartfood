package com.smartfood.app.feature.recipes.ui.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Nombre: AiFlowScreenTest.kt. Autor: SmartFood Team. Fecha: Octubre 2026. Descripción: Pruebas del flujo IA. */
class AiFlowScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun caRia0201_elContextoMuestraInventarioPresupuestoYOpciones() {
        composeRule.setContent { SmartfoodTheme { AiFlowScreen(AiUiState(), {}, {}) } }
        composeRule.onNodeWithText("Inventario detectado").assertIsDisplayed()
        composeRule.onNodeWithText("Presupuesto máximo: COP $50.000").assertIsDisplayed()
        composeRule.onNodeWithText("Generar recomendaciones").assertIsDisplayed()
    }

    @Test
    fun caRia0202_analizandoMuestraCancelar() {
        val events = mutableListOf<AiEvent>()
        composeRule.setContent { SmartfoodTheme { AiFlowScreen(AiUiState(step = AiStep.Loading), events::add, {}) } }
        composeRule.onNodeWithText("Analizando opciones").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        assertEquals(listOf(AiEvent.CancelClicked), events)
    }

    @Test
    fun caRia0203_sinCoincidenciasMuestraAjustar() {
        composeRule.setContent { SmartfoodTheme { AiFlowScreen(AiUiState(step = AiStep.NoPlan), {}, {}) } }
        composeRule.onNodeWithText("No encontramos una opción segura").assertIsDisplayed()
        composeRule.onNodeWithText("Ajustar contexto").assertIsDisplayed()
    }

    @Test
    fun caRia0301_ayudaMuestraCriteriosYSoporte() {
        composeRule.setContent { SmartfoodTheme { AiHelpScreen {} } }
        composeRule.onNodeWithText("¿Cómo decide la IA?").assertIsDisplayed()
        composeRule.onNodeWithText("Contactar soporte").assertIsDisplayed()
    }
}
