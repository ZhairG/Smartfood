package com.smartfood.app.feature.recipes.domain.model

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity

/**
 * Nombre: InventoryChange.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Cambio del inventario al terminar de cocinar (spec 009, plan §6, CA-RIA-04-08 y 04-09). Los cambios de
 * un cierre se escriben juntos, en un solo lote, con el registro de lo rescatado.
 */
sealed interface InventoryChange {

    /** Producto del inventario que cambia. */
    val item: InventoryItem

    /** Le queda [quantity] en su unidad original; el precio, la foto, el vencimiento y la categoría no cambian. */
    data class UpdateQuantity(override val item: InventoryItem, val quantity: Quantity) : InventoryChange

    /** Se acabó o no tenía cantidad: se borra. */
    data class Delete(override val item: InventoryItem) : InventoryChange
}
