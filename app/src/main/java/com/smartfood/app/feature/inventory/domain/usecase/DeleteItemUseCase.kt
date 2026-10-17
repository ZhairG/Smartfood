package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import javax.inject.Inject

/**
 * Nombre: DeleteItemUseCase.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Elimina un producto del inventario junto con su foto, aunque no haya conexión (HU-INV-04, CA-INV-10-03).
 *
 * @param repository Repositorio de productos.
 * @param photoSync Subida y borrado de fotos en segundo plano.
 */
class DeleteItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val photoSync: PhotoSync
) {
    /**
     * Borra el producto, cancela su foto pendiente y encola el borrado de su foto en la nube, si tenía
     * (CA-INV-04-02). Sin conexión, la foto se borra cuando vuelve la conexión (CA-INV-10-03).
     *
     * @return Si el servidor ya confirmó el borrado.
     */
    suspend operator fun invoke(item: InventoryItem): Result<WriteOutcome> =
        repository.deleteItem(item.id).onSuccess {
            photoSync.cancelUpload(item.id)
            item.photo?.let { photoSync.enqueueDelete(it.path) }
        }
}
