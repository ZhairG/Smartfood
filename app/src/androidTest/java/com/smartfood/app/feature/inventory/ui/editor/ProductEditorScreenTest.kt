/**
 * Nombre: ProductEditorScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [ProductEditorScreen] sin estado (spec 006, T032): botón habilitado y
 * deshabilitado, campos en rojo, hoja de categoría, confirmaciones y fila de cantidad.
 */
package com.smartfood.app.feature.inventory.ui.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ProductEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<ProductEditorEvent>()

    private fun mostrar(state: ProductEditorUiState) {
        composeTestRule.setContent {
            SmartfoodTheme { ProductEditorScreen(state = state, onEvent = { eventos += it }) }
        }
    }

    private val formularioCompleto = ProductEditorUiState(
        name = "Carne",
        expiresOn = LocalDate.of(2026, 9, 20),
        category = FoodCategory.PROTEINS,
        isSaveEnabled = true
    )

    @Test
    fun t032_caInv0201_elAltaMuestraLosCamposDelFormulario() {
        mostrar(ProductEditorUiState())

        composeTestRule.onNodeWithText("Añadir Producto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ej. Carne").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fecha de vencimiento").assertIsDisplayed()
        composeTestRule.onNodeWithText("Seleccionar categoría").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Tomar foto").assertIsDisplayed()
    }

    @Test
    fun t032_caInv0202_sinLosTresCamposElBotonEstaDeshabilitado() {
        mostrar(ProductEditorUiState(name = "Carne"))

        composeTestRule.onNode(hasText("Añadir al Inventario") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun t032_caInv0202_conLosTresCamposElBotonGuarda() {
        mostrar(formularioCompleto)

        composeTestRule.onNode(hasText("Añadir al Inventario") and hasClickAction()).assertIsEnabled().performClick()

        assertEquals(listOf<ProductEditorEvent>(ProductEditorEvent.SaveClicked), eventos)
    }

    @Test
    fun t032_caInv0203_unNombreInvalidoMuestraSuMensaje() {
        mostrar(
            formularioCompleto.copy(
                name = " ",
                error = InventoryError.Validation(InventoryField.NAME, InventoryError.Validation.Reason.EMPTY)
            )
        )

        composeTestRule.onNodeWithText("Escribe el nombre del producto.").assertIsDisplayed()
    }

    @Test
    fun t032_caInv0205_laHojaDeCategoriaGuardaLaElegida() {
        mostrar(ProductEditorUiState(dialog = EditorDialog.CategorySheet))

        composeTestRule.onNodeWithText("Seleccione una categoría").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lácteos").performClick()
        composeTestRule.onNodeWithText("Guardar").performClick()

        assertEquals(listOf<ProductEditorEvent>(ProductEditorEvent.CategoryPicked(FoodCategory.DAIRY)), eventos)
    }

    @Test
    fun t032_caInv0205_cancelarLaHojaNoCambiaLaCategoria() {
        mostrar(ProductEditorUiState(dialog = EditorDialog.CategorySheet))

        composeTestRule.onNodeWithText("Cancelar").performClick()

        assertEquals(listOf<ProductEditorEvent>(ProductEditorEvent.DialogDismissed), eventos)
    }

    @Test
    fun t032_caInv0208_laFilaDeCantidadEmpiezaEnUnidades() {
        mostrar(ProductEditorUiState())

        composeTestRule.onNodeWithText("Cantidad (opcional)").assertIsDisplayed()
        composeTestRule.onNodeWithText("unidades").assertIsDisplayed()
    }

    @Test
    fun t032_caInv0401_laEdicionMuestraEliminarYGuardar() {
        mostrar(formularioCompleto.copy(mode = EditorMode.EDIT))

        composeTestRule.onNodeWithText("Editar Producto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeTestRule.onNode(hasText("Eliminar") and hasClickAction()).performClick()

        assertEquals(listOf<ProductEditorEvent>(ProductEditorEvent.DeleteClicked), eventos)
    }

    @Test
    fun t032_caInv0403_cancelarLaConfirmacionNoElimina() {
        mostrar(formularioCompleto.copy(mode = EditorMode.EDIT, dialog = EditorDialog.ConfirmDelete))

        composeTestRule.onNodeWithText("¿Desea eliminar el producto?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").performClick()

        assertEquals(listOf<ProductEditorEvent>(ProductEditorEvent.DialogDismissed), eventos)
        assertTrue(ProductEditorEvent.DeleteConfirmed !in eventos)
    }
}
