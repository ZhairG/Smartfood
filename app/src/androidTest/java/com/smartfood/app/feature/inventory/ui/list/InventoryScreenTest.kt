/**
 * Nombre: InventoryScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de la pantalla stateless [InventoryScreen] (spec 006, HU-INV-01 y HU-INV-10):
 * las secciones y sus filas, los productos pendientes de sincronizar y los estados de carga, vacío y error con sus
 * eventos. Cada prueba monta la pantalla una sola vez y recorre varios estados cambiando el `UiState` (#105).
 */
package com.smartfood.app.feature.inventory.ui.list

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.InventorySection
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.testutil.PantallaEnFlujo
import com.smartfood.app.testutil.inventoryItem
import com.smartfood.app.testutil.mostrado
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class InventoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 10)
    private val tema = mutableStateOf(ThemeMode.LIGHT)

    private val flujo = PantallaEnFlujo<InventoryUiState, InventoryEvent>(
        composeTestRule,
        InventoryUiState(isLoading = false)
    ) { state, onEvent ->
        SmartfoodTheme(themeMode = tema.value) { InventoryScreen(state = state, onEvent = onEvent) }
    }

    private fun conProductos(vararg sections: InventorySection) =
        InventoryUiState(isLoading = false, sections = sections.toList())

    @Test
    fun caInv0101_0102_0103_0108_0109_laListaMuestraSusSeccionesYLosDatosDeCadaFila() {
        // CA-INV-01-01, 01-02 y 01-03: las tres secciones y la fila con fecha, nombre, categoría, lápiz y bloque de color.
        flujo.mostrar(
            conProductos(
                InventorySection(
                    ExpiryStatus.EXPIRED,
                    listOf(inventoryItem("1", "Banano", FoodCategory.FRUITS, expiresOn = LocalDate.of(2026, 9, 9)))
                ),
                InventorySection(ExpiryStatus.EXPIRING_SOON, listOf(inventoryItem("item-99", "Tomate", FoodCategory.VEGETABLES))),
                InventorySection(ExpiryStatus.FRESH, listOf(inventoryItem("3", "Pasta", FoodCategory.GRAINS)))
            )
        )
        composeTestRule.mostrado("Vencido", "Próximo a vencer", "Fresco", "Banano", "Tomate", "Pasta", "09/09/2026", "Frutas")
        composeTestRule.onNodeWithContentDescription("Editar Banano").assertIsDisplayed()
        // El bloque del estado se describe con su nombre, no solo con el color (RNF-INV-04).
        composeTestRule.onNodeWithContentDescription("Fresco").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Editar Tomate").performClick()
        assertEquals("tocar el lápiz", listOf<InventoryEvent>(InventoryEvent.EditClicked("item-99")), flujo.eventos)

        // CA-INV-01-03 y 01-08: la foto del producto y la cantidad aparte de la categoría.
        flujo.paso(
            conProductos(
                InventorySection(
                    ExpiryStatus.EXPIRING_SOON,
                    listOf(
                        inventoryItem("1", "Leche Entera", FoodCategory.OTHER, photo = ItemPhoto(path = "photos/1.jpg", url = "https://example.com/1.jpg")),
                        inventoryItem("2", "Yogur", FoodCategory.DAIRY, quantity = Quantity(1.5, QuantityUnit.LITERS)),
                        inventoryItem("3", "Arroz", FoodCategory.GRAINS)
                    )
                )
            )
        )
        composeTestRule.onNodeWithContentDescription("Foto de Leche Entera").assertIsDisplayed()
        composeTestRule.mostrado("Lácteos", "1,5 L", "Yogur", "Cereales")

        // CA-INV-01-08 y 01-09: singular y plural de las unidades, y el precio junto a la fecha.
        flujo.paso(
            conProductos(
                InventorySection(
                    ExpiryStatus.FRESH,
                    listOf(
                        inventoryItem("1", "Pollo", quantity = Quantity(1.0, QuantityUnit.UNITS)),
                        inventoryItem("2", "Huevos", quantity = Quantity(12.0, QuantityUnit.UNITS)),
                        inventoryItem("3", "Carne", expiresOn = LocalDate.of(2026, 9, 20), price = 12_500L)
                    )
                )
            )
        )
        composeTestRule.mostrado("1 unidad", "12 unidades", "20/09/2026 · $ 12.500")
    }

    @Test
    fun t049_caInv1002_1004_1006_soloElPendienteLlevaSuIconoEnModoClaroYOscuro() {
        val pendiente = InventoryItem(
            id = "1",
            name = "Leche Entera",
            category = FoodCategory.DAIRY,
            expiresOn = today.plusDays(10),
            localPhotoPath = "/data/files/pending/1/a.jpg",
            isPendingSync = true
        )
        flujo.mostrar(conProductos(InventorySection(ExpiryStatus.FRESH, listOf(pendiente, inventoryItem("2", "Arroz")))))

        // CA-INV-10-02 y 10-06: solo el producto pendiente lleva el ícono, que se anuncia.
        composeTestRule.onAllNodesWithContentDescription("Pendiente de sincronizar").assertCountEquals(1)
        composeTestRule.onNodeWithContentDescription("Pendiente de sincronizar").assertIsDisplayed()
        // CA-INV-10-04: la miniatura usa la foto del teléfono aunque no haya foto en la nube.
        composeTestRule.onNodeWithContentDescription("Foto de Leche Entera").assertExists()

        tema.value = ThemeMode.DARK
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Pendiente de sincronizar").assertIsDisplayed()
    }

    @Test
    fun caInv0101_0105_laCargaElVacioYElErrorOfrecenSusAcciones() {
        // #45: mientras carga se ve el esqueleto, que TalkBack anuncia como "Cargando inventario".
        flujo.mostrar(InventoryUiState(isLoading = true))
        composeTestRule.onNodeWithContentDescription("Cargando inventario").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu inventario está vacío").assertDoesNotExist()

        // CA-INV-01-05: sin productos se invita a añadir el primero.
        flujo.paso(InventoryUiState(isLoading = false, sections = emptyList()))
        composeTestRule.mostrado("Tu inventario está vacío")
        composeTestRule.mostrado("Añade tus alimentos para hacer seguimiento de sus fechas de vencimiento y evitar el desperdicio.")
        composeTestRule.onNodeWithText("Añadir Producto").performClick()
        composeTestRule.onNodeWithContentDescription("Añadir producto").performClick()
        composeTestRule.onNodeWithContentDescription("Buscar productos").performClick()
        assertEquals(
            "botón del vacío, botón flotante y lupa",
            listOf(InventoryEvent.AddClicked, InventoryEvent.AddClicked, InventoryEvent.SearchClicked),
            flujo.eventos
        )

        // Un error de carga muestra su mensaje y ofrece reintentar.
        flujo.paso(InventoryUiState(isLoading = false, error = InventoryError.Network))
        composeTestRule.mostrado("Sin conexión a Internet. Verifica tu red e inténtalo de nuevo.")
        composeTestRule.onNodeWithText("Reintentar").performClick()
        assertEquals("reintentar", listOf<InventoryEvent>(InventoryEvent.RetryClicked), flujo.eventos)
    }
}
