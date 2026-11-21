package com.smartfood.app.testutil

import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.recipes.domain.model.CookingRecord
import com.smartfood.app.feature.recipes.domain.model.InventoryChange
import com.smartfood.app.feature.recipes.domain.repository.CookingHistoryRepository
import com.smartfood.app.feature.recipes.domain.repository.CookingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Nombre: CookingFakes.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Fakes del cierre de la cocina guiada (spec 009, plan §6, CA-RIA-04-08 y 04-09), compartidos por las
 * pruebas unitarias e instrumentadas.
 */

/**
 * [CookingRepository] en memoria. Aplica el lote sobre [inventory] (si se da) todo junto o nada, y, como las reglas
 * que niegan editar `cookings`, rechaza entero un lote que repite una operación.
 *
 * @param inventory Inventario en el que se aplican los cambios del lote.
 */
class FakeCookingRepository(private val inventory: FakeInventoryRepository? = null) : CookingRepository {
    /** Registros guardados, en orden. */
    val records = mutableListOf<CookingRecord>()

    /** Lotes aceptados: el registro y sus cambios. */
    val batches = mutableListOf<Pair<CookingRecord, List<InventoryChange>>>()

    /** Ids que entrega [newOperationId], en orden. */
    val operationIds = mutableListOf<String>()

    /** Cómo termina el lote: confirmado o en la cola sin conexión. */
    var outcome: WriteOutcome = WriteOutcome.SYNCED

    /** Si no es `null`, el lote falla con este error y no se aplica nada. */
    var failure: Throwable? = null

    private var nextId = 0

    override fun newOperationId(): String = "op${++nextId}".also { operationIds += it }

    override suspend fun isFinished(operationId: String): Boolean = records.any { it.id == operationId }

    override suspend fun finishCooking(record: CookingRecord, changes: List<InventoryChange>): Result<WriteOutcome> {
        failure?.let { return Result.failure(it) }
        if (records.any { it.id == record.id }) {
            return Result.failure(InventoryException(InventoryError.PermissionDenied))
        }
        inventory?.let { repo ->
            val updates = changes.filterIsInstance<InventoryChange.UpdateQuantity>().associateBy { it.item.id }
            val deletes = changes.filterIsInstance<InventoryChange.Delete>().map { it.item.id }.toSet()
            repo.items.value = repo.items.value
                .filterNot { it.id in deletes }
                .map { item -> updates[item.id]?.let { item.copy(quantity = it.quantity) } ?: item }
            repo.deletedIds += deletes
        }
        records += record
        batches += record to changes
        return Result.success(outcome)
    }
}

/** [CookingHistoryRepository] en memoria: [records] es el historial. */
class FakeCookingHistoryRepository : CookingHistoryRepository {
    val records = MutableStateFlow<List<CookingRecord>>(emptyList())
    override fun observeHistory(): Flow<List<CookingRecord>> = records
}
