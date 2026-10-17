package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.SavedItem
import com.smartfood.app.feature.inventory.domain.model.ValidItem
import com.smartfood.app.feature.inventory.domain.model.WriteOutcome
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import javax.inject.Inject

/**
 * Nombre: SaveItemUseCases.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Casos de uso para guardar un producto con su foto opcional: el alta (HU-INV-02) y la edición
 * (HU-INV-03, HU-INV-05). Guardan el producto primero y encolan la foto, que se sube en segundo plano aunque no haya
 * conexión (spec 006 §13, CA-INV-05-06, CA-INV-10-02). Así nunca queda un producto con una foto rota.
 */

/**
 * Agrega un producto nuevo al inventario.
 *
 * @param repository Repositorio de productos.
 * @param photoSync Subida de fotos en segundo plano.
 * @param validate Reglas del formulario.
 * @param reminderScheduler Aviso diario de productos por vencer.
 */
class AddItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val photoSync: PhotoSync,
    private val validate: ValidateItemUseCase,
    private val reminderScheduler: ExpiryReminderScheduler
) {
    /**
     * Valida, genera el id, guarda el producto y, si hay foto, la encola (CA-INV-02-06, CA-INV-05-06). Con éxito
     * programa el aviso diario, que no se duplica si ya estaba programado (CA-INV-08-01).
     *
     * @param photo Foto tomada con la cámara o elegida de la galería, o `null` si el producto va sin foto.
     * @return El producto guardado y si el servidor ya lo confirmó (CA-INV-10-02).
     */
    suspend operator fun invoke(draft: ItemDraft, photo: LocalPhoto?): Result<SavedItem> {
        val valid = validate(draft).getOrElse { return Result.failure(it) }
        val id = repository.newItemId()
        val item = valid.toItem(id, photo = null)
        val outcome = repository.createItem(item).getOrElse { return Result.failure(it) }
        photo?.let { photoSync.enqueueUpload(id, it, replacedPath = null) }
        reminderScheduler.schedule()
        return Result.success(SavedItem(item.pending(outcome, photoPending = photo != null), outcome))
    }
}

/** Qué pasa con la foto de un producto al editarlo. */
sealed interface PhotoEdit {
    /** Se conserva la foto actual, si la hay. */
    data object Keep : PhotoEdit

    /** Se quita la foto (CA-INV-05-04). */
    data object Remove : PhotoEdit

    /** Se reemplaza o se agrega una foto nueva (CA-INV-05-05). */
    data class Replace(val photo: LocalPhoto) : PhotoEdit
}

/**
 * Guarda los cambios de un producto existente.
 *
 * @param repository Repositorio de productos.
 * @param photoSync Subida y borrado de fotos en segundo plano.
 * @param validate Reglas del formulario.
 */
class UpdateItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val photoSync: PhotoSync,
    private val validate: ValidateItemUseCase
) {
    /**
     * Valida y guarda el producto (CA-INV-03-02). Después, según [photoEdit]:
     * - `Keep`: no escribe los campos de la foto, que puede estar poniendo una subida pendiente.
     * - `Replace`: encola la foto nueva; el worker la pone en el producto y después borra la anterior
     *   (CA-INV-05-05). Mientras tanto el producto conserva la foto anterior.
     * - `Remove`: borra los campos de la foto, cancela una subida pendiente y encola el borrado de la anterior
     *   (CA-INV-05-04).
     *
     * @param original Producto tal como está guardado.
     * @return El producto actualizado y si el servidor ya lo confirmó (CA-INV-10-02).
     */
    suspend operator fun invoke(original: InventoryItem, draft: ItemDraft, photoEdit: PhotoEdit): Result<SavedItem> {
        val valid = validate(draft, originalExpiresOn = original.expiresOn).getOrElse { return Result.failure(it) }
        val photo = if (photoEdit == PhotoEdit.Remove) null else original.photo
        val item = valid.toItem(original.id, photo)
        val outcome = repository.updateItem(item, photoChanged = photoEdit == PhotoEdit.Remove)
            .getOrElse { return Result.failure(it) }
        when (photoEdit) {
            PhotoEdit.Keep -> Unit
            PhotoEdit.Remove -> {
                photoSync.cancelUpload(original.id)
                original.photo?.let { photoSync.enqueueDelete(it.path) }
            }
            is PhotoEdit.Replace ->
                photoSync.enqueueUpload(original.id, photoEdit.photo, replacedPath = original.photo?.path)
        }
        return Result.success(SavedItem(item.pending(outcome, photoPending = photoEdit is PhotoEdit.Replace), outcome))
    }
}

/** Arma el producto a partir de los datos validados. */
private fun ValidItem.toItem(id: String, photo: ItemPhoto?) =
    InventoryItem(id = id, name = name, category = category, expiresOn = expiresOn, quantity = quantity, photo = photo, price = price)

/** Marca el producto como pendiente si la escritura quedó en el teléfono o su foto espera subirse. */
private fun InventoryItem.pending(outcome: WriteOutcome, photoPending: Boolean) =
    copy(isPendingSync = outcome == WriteOutcome.PENDING || photoPending)
