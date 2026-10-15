package com.smartfood.app.feature.inventory.domain.repository

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: InventoryRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para guardar y leer los productos del inventario del usuario con sesión activa (RF-INV-01).
 */
interface InventoryRepository {

    /** Productos del usuario. Emite de nuevo con cada cambio, sin recargar (CA-INV-01-07). */
    fun observeItems(): Flow<List<InventoryItem>>

    /** Obtiene el producto con el [id] indicado, o un fallo con `InventoryError.NotFound`. */
    suspend fun getItem(id: String): Result<InventoryItem>

    /** Genera el identificador de un producto nuevo sin escribir nada, para poder subir su foto antes de guardarlo. */
    fun newItemId(): String

    /** Crea el producto o reemplaza el que tenga el mismo id. */
    suspend fun saveItem(item: InventoryItem): Result<Unit>

    /** Elimina el producto con el [id] indicado. */
    suspend fun deleteItem(id: String): Result<Unit>
}
