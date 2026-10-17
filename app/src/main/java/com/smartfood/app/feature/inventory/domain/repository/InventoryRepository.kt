package com.smartfood.app.feature.inventory.domain.repository

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: InventoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para guardar y leer los productos del inventario del usuario con sesión activa (RF-INV-01).
 * Las escrituras funcionan sin conexión: quedan en el teléfono y se suben solas, y su [WriteOutcome] dice si el
 * servidor ya las confirmó (spec 006 §13, HU-INV-10).
 */
interface InventoryRepository {

    /**
     * Productos del usuario. Emite de nuevo con cada cambio, sin recargar (CA-INV-01-07). Los que tienen cambios sin
     * subir llegan con `isPendingSync` (CA-INV-10-02).
     */
    fun observeItems(): Flow<List<InventoryItem>>

    /** Obtiene el producto con el [id] indicado, o un fallo con `InventoryError.NotFound`. */
    suspend fun getItem(id: String): Result<InventoryItem>

    /** Genera el identificador de un producto nuevo sin escribir nada, para encolar su foto con el mismo id. */
    fun newItemId(): String

    /** Crea el producto nuevo con sus campos y las fechas del servidor. */
    suspend fun createItem(item: InventoryItem): Result<WriteOutcome>

    /**
     * Guarda los cambios de un producto existente sin tocar su fecha de creación.
     *
     * @param photoChanged `false` para no escribir los campos de la foto, que puede estar subiendo
     *   `PhotoUploadWorker` (§13).
     */
    suspend fun updateItem(item: InventoryItem, photoChanged: Boolean): Result<WriteOutcome>

    /** Elimina el producto con el [id] indicado. */
    suspend fun deleteItem(id: String): Result<WriteOutcome>

    /** Pone o quita la foto del producto; falla con `InventoryError.NotFound` si ya no existe. Lo usa el worker. */
    suspend fun setPhoto(id: String, photo: ItemPhoto?): Result<Unit>

    /** `true` si hay escrituras guardadas en el teléfono que el servidor todavía no confirmó (CA-INV-10-07). */
    suspend fun hasPendingWrites(): Boolean
}
