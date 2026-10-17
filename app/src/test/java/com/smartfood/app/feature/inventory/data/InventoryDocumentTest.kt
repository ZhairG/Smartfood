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
 * T017, RF-INV-01, plan §3).
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
    fun `RF-INV-01 un producto completo va y vuelve igual`() {
        val fields = completo.toDocumentFields()

        assertEquals("2026-10-20", fields[InventoryDocument.EXPIRES_ON])
        assertEquals("DAIRY", fields[InventoryDocument.CATEGORY])
        assertEquals("LITERS", fields[InventoryDocument.UNIT])
        assertEquals(completo, inventoryItemFromDocument("abc", fields))
    }

    @Test
    fun `CA-INV-02-08 sin cantidad no escribe quantity ni unit`() {
        val fields = completo.copy(quantity = null, photo = null).toDocumentFields()

        assertFalse(InventoryDocument.QUANTITY in fields)
        assertFalse(InventoryDocument.UNIT in fields)
        assertFalse(InventoryDocument.PHOTO_PATH in fields)
        assertFalse(InventoryDocument.PHOTO_URL in fields)
    }

    @Test
    fun `escribe el nombre normalizado sin tildes y en minusculas`() {
        val fields = completo.copy(name = " Lácteo  Fresco ").toDocumentFields()

        assertEquals("lacteo fresco", fields[InventoryDocument.NAME_NORMALIZED])
    }

    @Test
    fun `CA-INV-01-08 lee una cantidad entera guardada como Long`() {
        val item = inventoryItemFromDocument(
            "x", minimo + mapOf(InventoryDocument.QUANTITY to 3L, InventoryDocument.UNIT to "UNITS")
        )

        assertEquals(Quantity(3.0, QuantityUnit.UNITS), item?.quantity)
    }

    @Test
    fun `una categoria desconocida se lee como OTHER`() {
        val item = inventoryItemFromDocument("x", minimo + (InventoryDocument.CATEGORY to "SNACKS"))

        assertEquals(FoodCategory.OTHER, item?.category)
    }

    @Test
    fun `una unidad desconocida o una cantidad no positiva descartan la cantidad`() {
        val unidadRara = inventoryItemFromDocument(
            "x", minimo + mapOf(InventoryDocument.QUANTITY to 2.0, InventoryDocument.UNIT to "CUPS")
        )
        val cero = inventoryItemFromDocument(
            "x", minimo + mapOf(InventoryDocument.QUANTITY to 0, InventoryDocument.UNIT to "UNITS")
        )

        assertNull(unidadRara?.quantity)
        assertNull(cero?.quantity)
        assertEquals("Arroz", unidadRara?.name)
    }

    @Test
    fun `sin nombre o sin una fecha valida el documento se omite`() {
        assertNull(inventoryItemFromDocument("x", minimo - InventoryDocument.NAME))
        assertNull(inventoryItemFromDocument("x", minimo + (InventoryDocument.NAME to "  ")))
        assertNull(inventoryItemFromDocument("x", minimo - InventoryDocument.EXPIRES_ON))
        assertNull(inventoryItemFromDocument("x", minimo + (InventoryDocument.EXPIRES_ON to "20/10/2026")))
    }

    @Test
    fun `una foto incompleta se ignora`() {
        val item = inventoryItemFromDocument("x", minimo + (InventoryDocument.PHOTO_PATH to "ruta"))

        assertNull(item?.photo)
    }

    @Test
    fun `CA-INV-02-10 el precio va y vuelve, y sin precio no se escribe`() {
        val conPrecio = completo.copy(price = 12_500L)
        assertEquals(12_500L, conPrecio.toDocumentFields()[InventoryDocument.PRICE])
        assertEquals(conPrecio, inventoryItemFromDocument("abc", conPrecio.toDocumentFields()))
        assertFalse(InventoryDocument.PRICE in completo.toDocumentFields())
    }

    @Test
    fun `un precio fuera de rango se descarta al leer`() {
        assertNull(inventoryItemFromDocument("x", minimo + mapOf(InventoryDocument.PRICE to 0L))?.price)
        assertNull(inventoryItemFromDocument("x", minimo + mapOf(InventoryDocument.PRICE to "12500"))?.price)
    }

    // T047: al editar con `update`, lo que el producto ya no tiene se borra y la foto solo va si cambió.
    private val borrar = Any()

    @Test
    fun `CA-INV-03-02 al editar se borran la cantidad y el precio que se quitaron`() {
        val campos = completo.copy(quantity = null, price = null).toUpdateFields(photoChanged = false, delete = borrar)

        assertEquals(borrar, campos[InventoryDocument.QUANTITY])
        assertEquals(borrar, campos[InventoryDocument.UNIT])
        assertEquals(borrar, campos[InventoryDocument.PRICE])
        assertEquals("Leche Entera", campos[InventoryDocument.NAME])
    }

    @Test
    fun `T047 sin cambio de foto la edicion no escribe sus campos`() {
        val campos = completo.toUpdateFields(photoChanged = false, delete = borrar)

        assertFalse(InventoryDocument.PHOTO_PATH in campos)
        assertFalse(InventoryDocument.PHOTO_URL in campos)
        assertFalse(InventoryDocument.CREATED_AT in campos)
    }

    @Test
    fun `CA-INV-05-04 quitar la foto borra sus dos campos`() {
        val campos = completo.copy(photo = null).toUpdateFields(photoChanged = true, delete = borrar)

        assertEquals(borrar, campos[InventoryDocument.PHOTO_PATH])
        assertEquals(borrar, campos[InventoryDocument.PHOTO_URL])
    }

    @Test
    fun `T048 poner una foto escribe su ruta y su URL`() {
        val campos = ItemPhoto("users/u1/inventory/abc/g.jpg", "https://g").toPhotoFields(borrar)

        assertEquals("users/u1/inventory/abc/g.jpg", campos[InventoryDocument.PHOTO_PATH])
        assertEquals("https://g", campos[InventoryDocument.PHOTO_URL])
    }
}
