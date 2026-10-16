/**
 * Nombre: InventoryScreenTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas instrumentadas de la pantalla stateless [InventoryScreen] (spec 006, HU-INV-01,
 * CA-INV-01-01, CA-INV-01-02, CA-INV-01-03, CA-INV-01-05 y CA-INV-01-08) y de sus eventos.
 */
package com.smartfood.app.feature.inventory.ui.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.InventorySection
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class InventoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 9, 10)

    private fun item(
        id: String,
        name: String,
        category: FoodCategory = FoodCategory.OTHER,
        expiresOn: LocalDate = today,
        quantity: Quantity? = null,
        photo: ItemPhoto? = null,
        price: Long? = null,
    ) = InventoryItem(
        id = id,
        name = name,
        category = category,
        expiresOn = expiresOn,
        quantity = quantity,
        photo = photo,
        price = price
    )

    private fun show(
        state: InventoryUiState = InventoryUiState(isLoading = false),
        onEvent: (InventoryEvent) -> Unit = {}
    ) {
        composeTestRule.setContent {
            SmartfoodTheme {
                InventoryScreen(state = state, onEvent = onEvent)
            }
        }
    }

    @Test
    fun CA_INV_01_01_lasSeccionesAparecenConSusProductos() {
        show(
            InventoryUiState(
                isLoading = false,
                sections = listOf(
                    InventorySection(
                        status = ExpiryStatus.EXPIRED,
                        items = listOf(item(id = "1", name = "Banano", category = FoodCategory.FRUITS))
                    ),
                    InventorySection(
                        status = ExpiryStatus.EXPIRING_SOON,
                        items = listOf(item(id = "2", name = "Tomate", category = FoodCategory.VEGETABLES))
                    ),
                    InventorySection(
                        status = ExpiryStatus.FRESH,
                        items = listOf(item(id = "3", name = "Pasta", category = FoodCategory.GRAINS))
                    )
                )
            )
        )

        composeTestRule.onNodeWithText("Vencido").assertIsDisplayed()
        composeTestRule.onNodeWithText("Próximo a vencer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fresco").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banano").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pasta").assertIsDisplayed()
    }

    @Test
    fun CA_INV_01_02_laFilaMuestraFechaNombreCategoriaYLapiz() {
        show(
            InventoryUiState(
                isLoading = false,
                sections = listOf(
                    InventorySection(
                        status = ExpiryStatus.EXPIRED,
                        items = listOf(
                            item(
                                id = "1",
                                name = "Banano",
                                category = FoodCategory.FRUITS,
                                expiresOn = LocalDate.of(2026, 9, 9)
                            )
                        )
                    )
                )
            )
        )

        composeTestRule.onNodeWithText("09/09/2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banano").assertIsDisplayed()
        composeTestRule.onNodeWithText("Frutas").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Editar Banano").assertIsDisplayed()
    }

    @Test
    fun CA_INV_01_03_sinFotoSeMuestraElBloqueDelColorDelEstado() {
        show(
            InventoryUiState(
                isLoading = false,
                sections = listOf(
                    InventorySection(
                        status = ExpiryStatus.FRESH,
                        items = listOf(item(id = "1", name = "Pasta", category = FoodCategory.GRAINS))
                    )
                )
            )
        )

        // El bloque del estado se describe con su nombre, no solo con el color (RNF-INV-04).
        composeTestRule.onNodeWithContentDescription("Fresco").assertIsDisplayed()
    }

    @Test
    fun CA_INV_01_03_conFotoSeMuestraLaFotoDelProducto() {
        show(
            InventoryUiState(
                isLoading = false,
                sections = listOf(
                    InventorySection(
                        status = ExpiryStatus.EXPIRED,
                        items = listOf(
                            item(
                                id = "1",
                                name = "Leche Entera",
                                category = FoodCategory.DAIRY,
                                photo = ItemPhoto(path = "photos/1.jpg", url = "https://example.com/1.jpg")
                            )
                        )
                    )
                )
            )
        )

        composeTestRule.onNodeWithContentDescription("Foto de Leche Entera").assertIsDisplayed()
    }

    @Test
    fun CA_INV_01_05_sinProductosSeInvitaAAnadirElPrimero() {
        var lastEvent: InventoryEvent? = null
        show(
            state = InventoryUiState(isLoading = false, sections = emptyList()),
            onEvent = { lastEvent = it }
        )

        composeTestRule.onNodeWithText("Tu inventario está vacío").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Añade tus alimentos para hacer seguimiento de sus fechas de vencimiento y evitar el desperdicio.")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Añadir Producto").performClick()

        assertEquals(InventoryEvent.AddClicked, lastEvent)
    }

    @Test
    fun CA_INV_01_08_conCantidadSeMuestraJuntoAlNombre() {
        show(
            InventoryUiState(
                isLoading = false,
                sections = listOf(
                    InventorySection(
                        status = ExpiryStatus.EXPIRING_SOON,
                        items = listOf(
                            item(
                                id = "1",
                                name = "Leche Entera",
                                category = FoodCategory.DAIRY,
                                quantity = Quantity(amount = 1.5, unit = QuantityUnit.LITERS)
                            ),
                            item(id = "2", name = "Arroz", category = FoodCategory.GRAINS)
                        )
                    )
                )
            )
        )

        // La etiqueta solo lleva la categoría y la cantidad va aparte, después del nombre.
        composeTestRule.onNodeWithText("Lácteos").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,5 L").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leche Entera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cereales").assertIsDisplayed()
    }

    @Test
    fun alTocarLaLupa_seEmiteSearchClicked() {
        var lastEvent: InventoryEvent? = null
        show(onEvent = { lastEvent = it })

        composeTestRule.onNodeWithContentDescription("Buscar productos").performClick()

        assertEquals(InventoryEvent.SearchClicked, lastEvent)
    }

    @Test
    fun alTocarElBotonAnadir_seEmiteAddClicked() {
        var lastEvent: InventoryEvent? = null
        show(onEvent = { lastEvent = it })

        composeTestRule.onNodeWithContentDescription("Añadir producto").performClick()

        assertEquals(InventoryEvent.AddClicked, lastEvent)
    }

    @Test
    fun alTocarElLapiz_seEmiteEditClickedConElIdDelProducto() {
        var lastEvent: InventoryEvent? = null
        show(
            state = InventoryUiState(
                isLoading = false,
                sections = listOf(
                    InventorySection(
                        status = ExpiryStatus.FRESH,
                        items = listOf(item(id = "item-99", name = "Tomate", category = FoodCategory.VEGETABLES))
                    )
                )
            ),
            onEvent = { lastEvent = it }
        )

        composeTestRule.onNodeWithContentDescription("Editar Tomate").performClick()

        assertEquals(InventoryEvent.EditClicked("item-99"), lastEvent)
    }

    @Test
    fun conErrorDeCargaSeMuestraElMensajeYSeOfreceReintentar() {
        var lastEvent: InventoryEvent? = null
        show(
            state = InventoryUiState(isLoading = false, error = InventoryError.Network),
            onEvent = { lastEvent = it }
        )

        composeTestRule
            .onNodeWithText("Sin conexión a Internet. Verifica tu red e inténtalo de nuevo.")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Reintentar").performClick()

        assertEquals(InventoryEvent.RetryClicked, lastEvent)
    }

    @Test
    fun caInv0109_laFilaMuestraElPrecioJuntoALaFecha() {
        show(
            InventoryUiState(
                isLoading = false,
                sections = listOf(
                    InventorySection(
                        ExpiryStatus.FRESH,
                        listOf(item(id = "1", name = "Carne", expiresOn = LocalDate.of(2026, 9, 20), price = 12_500L))
                    )
                )
            )
        )

        composeTestRule.onNodeWithText("20/09/2026 · $ 12.500").assertIsDisplayed()
    }
}
