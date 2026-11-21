package com.smartfood.app.feature.recipes.domain.repository

import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.model.InventoryChange

/**
 * Nombre: CookingRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Contrato del cierre de la cocina guiada (spec 009, plan §6, CA-RIA-04-08 y 04-09): el descuento del
 * inventario y el registro de lo rescatado se escriben juntos, y una misma operación nunca se aplica dos veces.
 */
interface CookingRepository {

    /** Id nuevo para una operación de cierre; es el id del registro en `users/{uid}/cookings`. */
    fun newOperationId(): String

    /** Si el registro de [operationId] ya existe (en el teléfono o en el servidor): el cierre ya se aplicó. */
    suspend fun isFinished(operationId: String): Boolean

    /**
     * Escribe [changes] y [record] en un solo lote: o se aplica todo o nada. Como el inventario (spec 006 §13), espera
     * al servidor un tiempo corto: sin conexión devuelve [WriteOutcome.PENDING] y el lote sigue en la cola.
     */
    suspend fun finishCooking(record: CookingRecord, changes: List<InventoryChange>): Result<WriteOutcome>
}
