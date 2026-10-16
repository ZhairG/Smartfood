package com.smartfood.app.core.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.theme.FormMetrics
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: SmartFoodPrimaryButtonTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de la variante destructiva de [SmartFoodPrimaryButton] (#80).
 */
class SmartFoodPrimaryButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // CA-INV-04-01: con la variante destructiva habilitada, el toque llega al callback.
    @Test
    fun t025_caInv0401_destructivoHabilitadoResponde() {
        var toques = 0

        mostrarBoton(enabled = true) { toques++ }

        composeTestRule.onNodeWithText("Eliminar").assertIsEnabled()
        composeTestRule.onNodeWithText("Eliminar").performClick()

        assertEquals(1, toques)
    }

    // CA-INV-04-01: deshabilitado no responde.
    @Test
    fun t025_caInv0401_destructivoDeshabilitadoNoResponde() {
        var toques = 0

        mostrarBoton(enabled = false) { toques++ }

        composeTestRule.onNodeWithText("Eliminar").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Eliminar").performClick()

        assertEquals(0, toques)
    }

    /** Dibuja el botón destructivo con las medidas que le provee la pantalla. */
    private fun mostrarBoton(enabled: Boolean, onClick: () -> Unit) {
        composeTestRule.setContent {
            SmartfoodTheme {
                CompositionLocalProvider(LocalFormMetrics provides FormMetrics.Compact) {
                    SmartFoodPrimaryButton(
                        text = "Eliminar",
                        onClick = onClick,
                        enabled = enabled,
                        destructive = true,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
