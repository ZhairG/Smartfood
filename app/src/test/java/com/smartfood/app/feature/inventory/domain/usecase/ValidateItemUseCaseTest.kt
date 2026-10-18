package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryError.Validation
import com.smartfood.app.feature.inventory.domain.model.InventoryError.Validation.Reason
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.ValidItem
import com.smartfood.app.testutil.fixedClock
import com.smartfood.app.testutil.inventoryError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: ValidateItemUseCaseTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ValidateItemUseCase]: campos obligatorios, nombre, fecha, cantidad y precio
 * (RF-INV-03). Cada prueba es una tabla de casos con su descripción (#105).
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
        priceText: String = "",
    ) = ItemDraft(name, expiresOn, category, quantityText, unit, priceText)

    @Test
    fun `CA-INV-02-02 CA-INV-02-08 CA-INV-02-10 el formulario esta completo solo con nombre, fecha y categoria`() {
        listOf(
            "los tres campos" to (draft() to true),
            "nombre con espacios" to (draft(name = "   ") to false),
            "sin fecha" to (draft(expiresOn = null) to false),
            "sin categoría" to (draft(category = null) to false),
            // La cantidad y el precio no cambian si el formulario está completo.
            "cantidad inválida" to (draft(quantityText = "abc") to true),
            "cantidad sin categoría" to (draft(category = null, quantityText = "2") to false),
            "precio inválido" to (draft(priceText = "abc") to true)
        ).forEach { (caso, entrada) ->
            assertEquals(caso, entrada.second, validate.isComplete(entrada.first))
        }
    }

    @Test
    fun `CA-INV-02-02 CA-INV-02-03 CA-INV-02-04 CA-INV-03-03 CA-INV-02-09 CA-INV-02-11 cada dato invalido falla en su campo`() {
        val vencida = today.minusDays(3)
        val casos = mutableListOf(
            Triple("sin fecha", draft(expiresOn = null), Validation(InventoryField.EXPIRY, Reason.EMPTY)),
            Triple("sin categoría", draft(category = null), Validation(InventoryField.CATEGORY, Reason.EMPTY)),
            Triple("nombre con espacios", draft(name = "   "), Validation(InventoryField.NAME, Reason.EMPTY)),
            Triple("nombre de 51", draft(name = "a".repeat(51)), Validation(InventoryField.NAME, Reason.TOO_LONG)),
            // Con varios errores se informa el primer campo del formulario.
            Triple(
                "varios errores",
                draft(name = "", expiresOn = null, category = null, quantityText = "abc"),
                Validation(InventoryField.NAME, Reason.EMPTY)
            ),
            Triple("fecha de ayer", draft(expiresOn = today.minusDays(1)), Validation(InventoryField.EXPIRY, Reason.PAST_DATE))
        )
        listOf("0", "0,00", "-1", "10000", "9999,01", "1,234", "abc", "1e3", "1.000,5", ",5", "1,").forEach {
            casos += Triple("cantidad \"$it\"", draft(quantityText = it), Validation(InventoryField.QUANTITY, Reason.INVALID))
        }
        listOf("0", "abc", "12,5", "12.5", "100000000", "-3", "1.2345").forEach {
            casos += Triple("precio \"$it\"", draft(priceText = it), Validation(InventoryField.PRICE, Reason.INVALID))
        }
        casos.forEach { (caso, entrada, error) -> assertEquals(caso, error, validate(entrada).inventoryError) }

        // CA-INV-03-03: en la edición, una fecha nueva anterior a hoy tampoco se acepta.
        assertEquals(
            Validation(InventoryField.EXPIRY, Reason.PAST_DATE),
            validate(draft(expiresOn = today.minusDays(1)), originalExpiresOn = vencida).inventoryError
        )
    }

    @Test
    fun `CA-INV-02-03 CA-INV-02-04 CA-INV-03-03 CA-INV-02-08 CA-INV-02-09 CA-INV-02-10 los datos validos se normalizan`() {
        val vencida = today.minusDays(3)
        listOf<Triple<String, Any?, Any?>>(
            // CA-INV-02-03: los espacios al inicio y al final se ignoran.
            Triple("nombre de 50 con espacios", ok(draft(name = "  " + "a".repeat(50) + "  ")).name, "a".repeat(50)),
            Triple("nombre con espacios", ok(draft(name = "  Carne ")).name, "Carne"),
            // CA-INV-02-04 y CA-INV-03-03: hoy se acepta y la edición conserva una fecha ya vencida.
            Triple("fecha de hoy", ok(draft(expiresOn = today)).expiresOn, today),
            Triple("fecha vencida conservada", ok(draft(expiresOn = vencida), originalExpiresOn = vencida).expiresOn, vencida),
            // CA-INV-02-08 y 02-09: cantidad opcional, con coma o punto, en unidades por defecto y con sus límites.
            Triple("sin cantidad", ok(draft(quantityText = "  ", unit = QuantityUnit.KILOGRAMS)).quantity, null),
            Triple("coma decimal", ok(draft(quantityText = "1,5", unit = QuantityUnit.KILOGRAMS)).quantity, Quantity(1.5, QuantityUnit.KILOGRAMS)),
            Triple("punto decimal", ok(draft(quantityText = " 0.75 ", unit = QuantityUnit.LITERS)).quantity, Quantity(0.75, QuantityUnit.LITERS)),
            Triple("sin unidad", ok(draft(quantityText = "2")).quantity, Quantity(2.0, QuantityUnit.UNITS)),
            Triple("límite inferior", ok(draft(quantityText = "0,01")).quantity, Quantity(0.01, QuantityUnit.UNITS)),
            Triple("límite superior", ok(draft(quantityText = "9999")).quantity, Quantity(9999.0, QuantityUnit.UNITS)),
            // CA-INV-02-10: precio opcional y con puntos de miles.
            Triple("sin precio", ok(draft()).price, null),
            Triple("precio", ok(draft(priceText = "12500")).price, 12_500L),
            Triple("precio con puntos", ok(draft(priceText = " 12.500 ")).price, 12_500L),
            Triple("precio máximo", ok(draft(priceText = "99.999.999")).price, 99_999_999L)
        ).forEach { (caso, valor, esperado) -> assertEquals(caso, esperado, valor) }
    }

    private fun ok(draft: ItemDraft, originalExpiresOn: LocalDate? = null): ValidItem =
        validate(draft, originalExpiresOn = originalExpiresOn).getOrThrow()
}
