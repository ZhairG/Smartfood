package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError.Validation
import com.smartfood.app.feature.inventory.domain.model.InventoryError.Validation.Reason
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: ValidateItemUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ValidateItemUseCase]: campos obligatorios, nombre, fecha y cantidad
 * (RF-INV-03).
 */
class ValidateItemUseCaseTest {

    private val today = LocalDate.of(2026, 9, 10)
    private val validate = ValidateItemUseCase(fixedClock(today))

    private fun draft(
        name: String = "Carne",
        expiresOn: LocalDate? = today.plusDays(5),
        category: FoodCategory? = FoodCategory.PROTEINS,
        quantityText: String = "",
        unit: QuantityUnit? = null,
    ) = ItemDraft(name, expiresOn, category, quantityText, unit)

    // CA-INV-02-02: botón habilitado solo con los tres campos obligatorios.

    @Test
    fun `CA-INV-02-02 con nombre, fecha y categoria el formulario esta completo`() {
        assertTrue(validate.isComplete(draft()))
    }

    @Test
    fun `CA-INV-02-02 sin nombre, fecha o categoria el formulario esta incompleto`() {
        assertFalse(validate.isComplete(draft(name = "   ")))
        assertFalse(validate.isComplete(draft(expiresOn = null)))
        assertFalse(validate.isComplete(draft(category = null)))
    }

    @Test
    fun `CA-INV-02-08 la cantidad no cambia si el formulario esta completo`() {
        assertTrue(validate.isComplete(draft(quantityText = "abc")))
        assertFalse(validate.isComplete(draft(category = null, quantityText = "2")))
    }

    @Test
    fun `CA-INV-02-02 los campos obligatorios vacios fallan con EMPTY en su campo`() {
        assertEquals(Validation(InventoryField.EXPIRY, Reason.EMPTY), validate(draft(expiresOn = null)).inventoryError)
        assertEquals(Validation(InventoryField.CATEGORY, Reason.EMPTY), validate(draft(category = null)).inventoryError)
    }

    // CA-INV-02-03: nombre.

    @Test
    fun `CA-INV-02-03 un nombre con solo espacios es un nombre vacio`() {
        assertEquals(Validation(InventoryField.NAME, Reason.EMPTY), validate(draft(name = "   ")).inventoryError)
    }

    @Test
    fun `CA-INV-02-03 un nombre de mas de 50 caracteres es demasiado largo`() {
        assertEquals(
            Validation(InventoryField.NAME, Reason.TOO_LONG),
            validate(draft(name = "a".repeat(51))).inventoryError
        )
    }

    @Test
    fun `CA-INV-02-03 los espacios al inicio y al final se ignoran`() {
        val padded = "  " + "a".repeat(50) + "  "

        assertEquals("a".repeat(50), validate(draft(name = padded)).getOrThrow().name)
        assertEquals("Carne", validate(draft(name = "  Carne ")).getOrThrow().name)
    }

    @Test
    fun `CA-INV-02-03 con varios errores se informa el primer campo del formulario`() {
        val result = validate(draft(name = "", expiresOn = null, category = null, quantityText = "abc"))

        assertEquals(Validation(InventoryField.NAME, Reason.EMPTY), result.inventoryError)
    }

    // CA-INV-02-04 y CA-INV-03-03: fecha.

    @Test
    fun `CA-INV-02-04 una fecha anterior a hoy no se acepta`() {
        assertEquals(
            Validation(InventoryField.EXPIRY, Reason.PAST_DATE),
            validate(draft(expiresOn = today.minusDays(1))).inventoryError
        )
    }

    @Test
    fun `CA-INV-02-04 la fecha de hoy se acepta`() {
        assertEquals(today, validate(draft(expiresOn = today)).getOrThrow().expiresOn)
    }

    @Test
    fun `CA-INV-03-03 en la edicion se puede conservar una fecha ya vencida`() {
        val stored = today.minusDays(3)

        assertEquals(stored, validate(draft(expiresOn = stored), originalExpiresOn = stored).getOrThrow().expiresOn)
    }

    @Test
    fun `CA-INV-03-03 en la edicion una fecha nueva anterior a hoy no se acepta`() {
        val result = validate(draft(expiresOn = today.minusDays(1)), originalExpiresOn = today.minusDays(3))

        assertEquals(Validation(InventoryField.EXPIRY, Reason.PAST_DATE), result.inventoryError)
    }

    // CA-INV-02-08 y CA-INV-02-09: cantidad.

    @Test
    fun `CA-INV-02-08 sin cantidad el producto se guarda sin cantidad ni unidad`() {
        assertNull(validate(draft(quantityText = "  ", unit = QuantityUnit.KILOGRAMS)).getOrThrow().quantity)
    }

    @Test
    fun `CA-INV-02-08 la cantidad acepta coma o punto decimal`() {
        assertEquals(
            Quantity(1.5, QuantityUnit.KILOGRAMS),
            validate(draft(quantityText = "1,5", unit = QuantityUnit.KILOGRAMS)).getOrThrow().quantity
        )
        assertEquals(
            Quantity(0.75, QuantityUnit.LITERS),
            validate(draft(quantityText = " 0.75 ", unit = QuantityUnit.LITERS)).getOrThrow().quantity
        )
    }

    @Test
    fun `CA-INV-02-08 una cantidad sin unidad usa unidades`() {
        assertEquals(Quantity(2.0, QuantityUnit.UNITS), validate(draft(quantityText = "2")).getOrThrow().quantity)
    }

    @Test
    fun `CA-INV-02-09 los limites 0,01 y 9999 se aceptan`() {
        assertEquals(0.01, validate(draft(quantityText = "0,01")).getOrThrow().quantity?.amount)
        assertEquals(9999.0, validate(draft(quantityText = "9999")).getOrThrow().quantity?.amount)
    }

    @Test
    fun `CA-INV-02-09 una cantidad invalida marca el campo cantidad`() {
        listOf("0", "0,00", "-1", "10000", "9999,01", "1,234", "abc", "1e3", "1.000,5", ",5", "1,").forEach { text ->
            assertEquals(
                "Cantidad \"$text\"",
                Validation(InventoryField.QUANTITY, Reason.INVALID),
                validate(draft(quantityText = text)).inventoryError
            )
        }
    }
}
