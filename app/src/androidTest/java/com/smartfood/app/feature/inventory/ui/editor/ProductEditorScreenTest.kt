/**
 * Nombre: ProductEditorScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de [ProductEditorScreen] sin estado (spec 006, T032, T050, T052, T054 a T056).
 * Cada prueba monta la pantalla una sola vez y recorre varios estados de un mismo flujo cambiando el `UiState` (#105):
 * el alta con su botón y sus errores, la hoja de categoría, la edición y su confirmación, la foto y el diálogo de carga.
 */
package com.smartfood.app.feature.inventory.ui.editor

import androidx.compose.runtime.mutableStateOf
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
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ProductEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventos = mutableListOf<ProductEditorEvent>()
    private val estado = mutableStateOf(ProductEditorUiState())

    private val formularioCompleto = ProductEditorUiState(
        name = "Carne",
        expiresOn = LocalDate.of(2026, 9, 20),
        category = FoodCategory.PROTEINS,
        isSaveEnabled = true
    )

    /** Monta la pantalla una sola vez; cada paso del flujo cambia [estado]. */
    private fun mostrar(inicial: ProductEditorUiState) {
        estado.value = inicial
        composeTestRule.setContent {
            SmartfoodTheme { ProductEditorScreen(state = estado.value, onEvent = { eventos += it }) }
        }
    }

    private fun paso(nuevo: ProductEditorUiState) {
        eventos.clear()
        estado.value = nuevo
        composeTestRule.waitForIdle()
    }

    private val botonAnadir get() = composeTestRule.onNode(hasText("Añadir al Inventario") and hasClickAction())

    @Test
    fun t032_t052_t054_t056_caInv0201_0202_0203_0208_0211_0212_altaConSuBotonYSusErrores() {
        mostrar(ProductEditorUiState())
        // CA-INV-02-01 y 02-08: el alta muestra el formulario, con la cantidad en unidades.
        listOf("Añadir Producto", "Ej. Carne", "Fecha de vencimiento", "Seleccionar categoría", "Cantidad", "unidades")
            .forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
        composeTestRule.onNodeWithContentDescription("Tomar foto").assertIsDisplayed()
        // CA-INV-02-12: sin guardar no hay diálogo de carga.
        composeTestRule.onNodeWithText("Guardando producto…").assertDoesNotExist()

        // CA-INV-02-02 y T054: con campos vacíos el botón se ve gris, pero tocarlo pide guardar (y mostrar el aviso).
        paso(ProductEditorUiState(name = "Carne"))
        botonAnadir.assertIsNotEnabled()
        composeTestRule.onNodeWithText("Añadir al Inventario").performClick()
        assertEquals("tocar el botón gris", listOf<ProductEditorEvent>(ProductEditorEvent.SaveClicked), eventos)
        paso(ProductEditorUiState(name = "Carne", showMissingFields = true))
        composeTestRule.onNodeWithText("⚠ Los campos no pueden estar vacíos").assertIsDisplayed()

        // CA-INV-02-03 y 02-11: los errores de validación muestran su mensaje.
        paso(formularioCompleto.copy(name = " ", error = validacion(InventoryField.NAME, InventoryError.Validation.Reason.EMPTY)))
        composeTestRule.onNodeWithText("Escribe el nombre del producto.").assertIsDisplayed()
        paso(formularioCompleto.copy(priceText = "0", error = validacion(InventoryField.PRICE, InventoryError.Validation.Reason.INVALID)))
        composeTestRule.onNodeWithContentDescription("Precio (opcional)").assertExists()
        composeTestRule.onNodeWithText("Escribe un precio en pesos, sin decimales, mayor que 0 y hasta 99.999.999.")
            .assertIsDisplayed()

        // CA-INV-02-02: con los tres campos el botón guarda.
        paso(formularioCompleto)
        botonAnadir.assertIsEnabled().performClick()
        assertEquals("guardar el alta completa", listOf<ProductEditorEvent>(ProductEditorEvent.SaveClicked), eventos)
    }

    @Test
    fun t032_caInv0205_laHojaDeCategoriaGuardaLaElegidaYCancelarNoCambiaNada() {
        mostrar(ProductEditorUiState(dialog = EditorDialog.CategorySheet))
        composeTestRule.onNodeWithText("Seleccione una categoría").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lácteos").performClick()
        composeTestRule.onNodeWithText("Guardar").performClick()
        assertEquals("elegir Lácteos", listOf<ProductEditorEvent>(ProductEditorEvent.CategoryPicked(FoodCategory.DAIRY)), eventos)

        eventos.clear()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        assertEquals("cancelar la hoja", listOf<ProductEditorEvent>(ProductEditorEvent.DialogDismissed), eventos)
    }

    @Test
    fun t032_caInv0401_0403_laEdicionPideConfirmarYCancelarNoElimina() {
        mostrar(formularioCompleto.copy(mode = EditorMode.EDIT))
        composeTestRule.onNodeWithText("Editar Producto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeTestRule.onNode(hasText("Eliminar") and hasClickAction()).performClick()
        assertEquals("tocar Eliminar", listOf<ProductEditorEvent>(ProductEditorEvent.DeleteClicked), eventos)

        paso(formularioCompleto.copy(mode = EditorMode.EDIT, dialog = EditorDialog.ConfirmDelete))
        composeTestRule.onNodeWithText("¿Desea eliminar el producto?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        assertEquals("cancelar la confirmación", listOf<ProductEditorEvent>(ProductEditorEvent.DialogDismissed), eventos)
    }

    @Test
    fun t050_t055_caInv0504_0507_1004_laFotoSeAgregaSeQuitaYSeVePendiente() {
        // CA-INV-05-04 y 05-07: sin foto la papelera está deshabilitada y tocar el recuadro pide agregar una.
        mostrar(ProductEditorUiState())
        composeTestRule.onNodeWithContentDescription("Eliminar la foto").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Tomar foto").performClick()
        assertEquals("tocar la foto", listOf<ProductEditorEvent>(ProductEditorEvent.AddPhotoClicked), eventos)

        // CA-INV-05-07: la hoja "Agregar foto" ofrece la galería.
        paso(ProductEditorUiState(dialog = EditorDialog.PhotoSourceSheet))
        composeTestRule.onNodeWithText("Agregar foto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Galería").performClick()
        assertEquals("elegir la galería", listOf<ProductEditorEvent>(ProductEditorEvent.GalleryChosen), eventos)

        // CA-INV-05-04: con foto, la papelera pide confirmar.
        paso(ProductEditorUiState(photo = PhotoUiState.Local("/no/existe.jpg")))
        composeTestRule.onNodeWithContentDescription("Eliminar la foto").assertIsEnabled().performClick()
        assertEquals("quitar la foto", listOf<ProductEditorEvent>(ProductEditorEvent.RemovePhotoClicked), eventos)

        // CA-INV-10-04 (T050): la foto que espera subirse se ve y se puede quitar.
        paso(ProductEditorUiState(mode = EditorMode.EDIT, photo = PhotoUiState.Pending("/files/pending/1/a.jpg")))
        composeTestRule.onNodeWithContentDescription("Foto del producto").assertExists()
        composeTestRule.onNodeWithContentDescription("Eliminar la foto").assertIsEnabled()
    }

    @Test
    fun t056_caInv0212_elDialogoDeCargaDiceLaAccionEnCurso() {
        mostrar(ProductEditorUiState(savingAction = SavingAction.ADD))
        composeTestRule.onNodeWithText("Guardando producto…").assertIsDisplayed()

        paso(ProductEditorUiState(mode = EditorMode.EDIT, savingAction = SavingAction.UPDATE))
        composeTestRule.onNodeWithText("Actualizando producto…").assertIsDisplayed()

        paso(ProductEditorUiState(mode = EditorMode.EDIT, savingAction = SavingAction.DELETE))
        composeTestRule.onNodeWithText("Eliminando producto…").assertIsDisplayed()
    }

    private fun validacion(field: InventoryField, reason: InventoryError.Validation.Reason) =
        InventoryError.Validation(field, reason)
}
