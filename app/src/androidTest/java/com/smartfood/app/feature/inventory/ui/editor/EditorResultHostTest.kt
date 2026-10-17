package com.smartfood.app.feature.inventory.ui.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: EditorResultHostTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [EditorResultHost] (spec 006): el resultado que deja el editor se muestra
 * sobre la lista, incluido "Guardado en tu teléfono" cuando se guardó sin conexión (CA-INV-10-02, T050).
 */
class EditorResultHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mostrar(result: EditorResult) {
        val savedStateHandle = SavedStateHandle(mapOf(EDITOR_RESULT_KEY to result.encode()))
        composeTestRule.setContent {
            SmartfoodTheme { EditorResultHost(savedStateHandle = savedStateHandle) }
        }
    }

    @Test
    fun t050_caInv1002_sinConexionSeVeGuardadoEnTuTelefono() {
        mostrar(EditorResult.SavedOnPhone)

        composeTestRule.onNodeWithText("Guardado en tu teléfono").assertIsDisplayed()
        composeTestRule.onNodeWithText("Se subirá cuando vuelva la conexión.").assertIsDisplayed()
    }

    @Test
    fun caInv0206_conConexionSeVeProductoGuardado() {
        mostrar(EditorResult.Saved)

        composeTestRule.onNodeWithText("Producto Guardado").assertIsDisplayed()
    }
}
