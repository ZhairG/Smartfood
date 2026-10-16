package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryError.Validation.Reason
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.domain.model.ValidItem
import com.smartfood.app.feature.inventory.domain.model.inventoryFailure
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Nombre: ValidateItemUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Único lugar con las reglas del formulario del producto, iguales para el alta y la edición
 * (RF-INV-03).
 *
 * @param clock Reloj del que se toma "hoy" para rechazar fechas pasadas.
 */
class ValidateItemUseCase @Inject constructor(
    private val clock: Clock
) {
    /**
     * Valida el borrador. Si hay varios errores, informa el del primer campo en el orden del formulario.
     *
     * @param originalExpiresOn Fecha guardada del producto en edición, que se puede conservar aunque ya haya pasado
     * (CA-INV-03-03); `null` en el alta.
     * @return Los datos listos para guardar, o un fallo con [InventoryError.Validation].
     */
    operator fun invoke(draft: ItemDraft, originalExpiresOn: LocalDate? = null): Result<ValidItem> {
        val name = draft.name.trim()
        val expiresOn = draft.expiresOn
        val category = draft.category
        return when {
            name.isEmpty() -> invalid(InventoryField.NAME, Reason.EMPTY)
            name.length > MAX_NAME_LENGTH -> invalid(InventoryField.NAME, Reason.TOO_LONG)
            expiresOn == null -> invalid(InventoryField.EXPIRY, Reason.EMPTY)
            expiresOn.isBefore(LocalDate.now(clock)) && expiresOn != originalExpiresOn ->
                invalid(InventoryField.EXPIRY, Reason.PAST_DATE)
            category == null -> invalid(InventoryField.CATEGORY, Reason.EMPTY)
            else -> parseQuantity(draft).mapCatching { quantity ->
                ValidItem(name, expiresOn, category, quantity, parsePrice(draft).getOrThrow())
            }
        }
    }

    /** Indica si están los tres campos obligatorios, para habilitar "Añadir al Inventario" (CA-INV-02-02). */
    fun isComplete(draft: ItemDraft): Boolean =
        draft.name.isNotBlank() && draft.expiresOn != null && draft.category != null

    /**
     * Interpreta la cantidad opcional (CA-INV-02-08, CA-INV-02-09). Un texto vacío es "sin cantidad" y descarta la
     * unidad; si hay número sin unidad, se usa [QuantityUnit.UNITS].
     */
    private fun parseQuantity(draft: ItemDraft): Result<Quantity?> {
        val text = draft.quantityText.trim().replace(',', '.')
        if (text.isEmpty()) return Result.success(null)
        val amount = text.takeIf { QUANTITY_PATTERN.matches(it) }?.toBigDecimalOrNull()?.stripTrailingZeros()
        if (amount == null || amount.signum() <= 0 || amount > MAX_QUANTITY || amount.scale() > MAX_DECIMALS) {
            return invalid(InventoryField.QUANTITY, Reason.INVALID)
        }
        return Result.success(Quantity(amount.toDouble(), draft.unit ?: QuantityUnit.UNITS))
    }

    /**
     * Interpreta el precio opcional en pesos enteros (CA-INV-02-10, CA-INV-02-11). Acepta "12500" o "12.500" (puntos
     * de miles); vacío es "sin precio".
     */
    private fun parsePrice(draft: ItemDraft): Result<Long?> {
        val text = draft.priceText.trim()
        if (text.isEmpty()) return Result.success(null)
        val price = text.takeIf { PRICE_PATTERN.matches(it) }?.replace(".", "")?.toLongOrNull()
        if (price == null || price <= 0 || price > MAX_PRICE) return invalid(InventoryField.PRICE, Reason.INVALID)
        return Result.success(price)
    }

    private fun <T> invalid(field: InventoryField, reason: Reason): Result<T> =
        inventoryFailure(InventoryError.Validation(field, reason))

    companion object {
        /** Longitud máxima del nombre recortado (CA-INV-02-03). */
        const val MAX_NAME_LENGTH = 50

        private const val MAX_DECIMALS = 2
        private val MAX_QUANTITY = BigDecimal(9999)

        /** Solo dígitos con un separador decimal opcional: sin signo, exponente ni separador de miles. */
        private val QUANTITY_PATTERN = Regex("""\d+(\.\d+)?""")

        /** Precio máximo en pesos (CA-INV-02-11). */
        const val MAX_PRICE = 99_999_999L

        /** Dígitos sin separadores ("12500") o con puntos de miles bien agrupados ("12.500"). */
        private val PRICE_PATTERN = Regex("""\d{1,3}(\.\d{3})+|\d+""")
    }
}
