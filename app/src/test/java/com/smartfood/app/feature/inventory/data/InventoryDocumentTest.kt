package com.smartfood.app.feature.inventory.data

import com.smartfood.app.feature.inventory.data.repository.InventoryDocument
import com.smartfood.app.feature.inventory.data.repository.inventoryItemFromDocument
import com.smartfood.app.feature.inventory.data.repository.toDocumentFields
import com.smartfood.app.feature.inventory.data.repository.toPhotoFields
import com.smartfood.app.feature.inventory.data.repository.toUpdateFields
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: InventoryDocumentTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de la conversión entre [InventoryItem] y su documento de Firestore (spec 006,
 * T017, RF-INV-01, plan §3): la escritura, la lectura tolerante y la edición (#105).
 */
class InventoryDocumentTest {

    private val completo = InventoryItem(
        id = "abc",
        name = "Leche Entera",
        category = FoodCategory.DAIRY,
        expiresOn = LocalDate.of(2026, 10, 20),
        quantity = Quantity(1.5, QuantityUnit.LITERS),
        photo = ItemPhoto("users/u1/inventory/abc/f.jpg", "https://foto"),
    )

    private val minimo = mapOf<String, Any?>(
        InventoryDocument.NAME to "Arroz",
        InventoryDocument.CATEGORY to "GRAINS",
        InventoryDocument.EXPIRES_ON to "2026-11-01",
    )

    @Test
    fun `RF-INV-01 CA-INV-02-08 CA-INV-02-10 un producto va y vuelve y solo se escribe lo que tiene`() {
        val fields = completo.toDocumentFields()
        assertEquals("2026-10-20", fields[InventoryDocument.EXPIRES_ON])
        assertEquals("DAIRY", fields[InventoryDocument.CATEGORY])
        assertEquals("LITERS", fields[InventoryDocument.UNIT])
        assertEquals("ida y vuelta", completo, inventoryItemFromDocument("abc", fields))

        val conPrecio = completo.copy(price = 12_500L)
        assertEquals(12_500L, conPrecio.toDocumentFields()[InventoryDocument.PRICE])
        assertEquals("ida y vuelta con precio", conPrecio, inventoryItemFromDocument("abc", conPrecio.toDocumentFields()))

        // Sin cantidad, foto ni precio no se escriben sus campos.
        val sinOpcionales = completo.copy(quantity = null, photo = null).toDocumentFields()
        listOf(
            InventoryDocument.QUANTITY, InventoryDocument.UNIT, InventoryDocument.PHOTO_PATH, InventoryDocument.PHOTO_URL,
            InventoryDocument.PRICE
        ).forEach { assertFalse(it, it in sinOpcionales) }

        // El nombre normalizado va sin tildes y en minúsculas.
        assertEquals(
            "lacteo fresco",
            completo.copy(name = " Lácteo  Fresco ").toDocumentFields()[InventoryDocument.NAME_NORMALIZED]
        )
    }

    @Test
    fun `CA-INV-01-08 al leer se toleran los datos raros y se omiten los documentos sin nombre o fecha`() {
        fun leer(extra: Map<String, Any?>) = inventoryItemFromDocument("x", minimo + extra)

        // Una cantidad entera guardada como Long se lee.
        assertEquals(
            Quantity(3.0, QuantityUnit.UNITS),
            leer(mapOf(InventoryDocument.QUANTITY to 3L, InventoryDocument.UNIT to "UNITS"))?.quantity
        )
        // Una categoría desconocida se lee como OTHER.
        assertEquals(FoodCategory.OTHER, leer(mapOf(InventoryDocument.CATEGORY to "SNACKS"))?.category)
        // Una unidad desconocida o una cantidad no positiva descartan solo la cantidad.
        val unidadRara = leer(mapOf(InventoryDocument.QUANTITY to 2.0, InventoryDocument.UNIT to "CUPS"))
        assertNull(unidadRara?.quantity)
        assertEquals("Arroz", unidadRara?.name)
        assertNull(leer(mapOf(InventoryDocument.QUANTITY to 0, InventoryDocument.UNIT to "UNITS"))?.quantity)
        // Una foto incompleta y un precio fuera de rango se ignoran.
        assertNull(leer(mapOf(InventoryDocument.PHOTO_PATH to "ruta"))?.photo)
        assertNull(leer(mapOf(InventoryDocument.PRICE to 0L))?.price)
        assertNull(leer(mapOf(InventoryDocument.PRICE to "12500"))?.price)

        // Sin nombre o sin una fecha válida el documento se omite.
        listOf(
            minimo - InventoryDocument.NAME,
            minimo + (InventoryDocument.NAME to "  "),
            minimo - InventoryDocument.EXPIRES_ON,
            minimo + (InventoryDocument.EXPIRES_ON to "20/10/2026")
        ).forEach { assertNull("$it", inventoryItemFromDocument("x", it)) }
    }

    // T047: al editar con `update`, lo que el producto ya no tiene se borra y la foto solo va si cambió.
    private val borrar = Any()

    @Test
    fun `T047 T048 CA-INV-03-02 CA-INV-05-04 la edicion borra lo que se quito y la foto solo va si cambio`() {
        val sinCantidadNiPrecio = completo.copy(quantity = null, price = null)
            .toUpdateFields(photoChanged = false, delete = borrar)
        listOf(InventoryDocument.QUANTITY, InventoryDocument.UNIT, InventoryDocument.PRICE)
            .forEach { assertEquals(it, borrar, sinCantidadNiPrecio[it]) }
        assertEquals("Leche Entera", sinCantidadNiPrecio[InventoryDocument.NAME])

        // Sin cambio de foto la edición no escribe sus campos ni la fecha de creación.
        val sinCambioDeFoto = completo.toUpdateFields(photoChanged = false, delete = borrar)
        listOf(InventoryDocument.PHOTO_PATH, InventoryDocument.PHOTO_URL, InventoryDocument.CREATED_AT)
            .forEach { assertFalse(it, it in sinCambioDeFoto) }

        // CA-INV-05-04: quitar la foto borra sus dos campos.
        val sinFoto = completo.copy(photo = null).toUpdateFields(photoChanged = true, delete = borrar)
        assertEquals(borrar, sinFoto[InventoryDocument.PHOTO_PATH])
        assertEquals(borrar, sinFoto[InventoryDocument.PHOTO_URL])

        // T048: poner una foto escribe su ruta y su URL.
        val foto = ItemPhoto("users/u1/inventory/abc/g.jpg", "https://g").toPhotoFields(borrar)
        assertEquals("users/u1/inventory/abc/g.jpg", foto[InventoryDocument.PHOTO_PATH])
        assertEquals("https://g", foto[InventoryDocument.PHOTO_URL])
    }
}
