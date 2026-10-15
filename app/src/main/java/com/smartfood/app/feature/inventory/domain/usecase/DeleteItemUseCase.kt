package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import javax.inject.Inject

/**
 * Nombre: DeleteItemUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Elimina un producto del inventario junto con su foto en la nube (HU-INV-04).
 *
 * @param repository Repositorio de productos.
 * @param photoRepository Repositorio de fotos en la nube.
 */
class DeleteItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val photoRepository: PhotoRepository
) {
    /**
     * Borra el documento y después la foto, si tenía (CA-INV-04-02). Si el borrado de la foto falla, el producto ya
     * no está en el inventario y la operación no falla.
     */
    suspend operator fun invoke(item: InventoryItem): Result<Unit> =
        repository.deleteItem(item.id).onSuccess {
            item.photo?.let { photoRepository.delete(it.path) }
        }
}
