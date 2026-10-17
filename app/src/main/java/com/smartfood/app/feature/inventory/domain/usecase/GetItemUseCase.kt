package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Nombre: GetItemUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Obtiene un producto para editarlo junto con su foto que todavía espera subirse, si tiene, para que
 * "Editar Producto" la muestre desde el teléfono (spec 006 §13, CA-INV-10-04, T050).
 *
 * @param repository Repositorio de productos.
 * @param photoSync Fotos que esperan subirse.
 */
class GetItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val photoSync: PhotoSync
) {
    /** @return El producto, con `localPhotoPath` e `isPendingSync` si su foto espera subirse. */
    suspend operator fun invoke(id: String): Result<InventoryItem> =
        repository.getItem(id).map { item ->
            photoSync.pendingPhotos().first()[id]
                ?.let { path -> item.copy(localPhotoPath = path, isPendingSync = true) }
                ?: item
        }
}
