package com.smartfood.app.feature.inventory.domain.usecase

import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.ItemDraft
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.ValidItem
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import javax.inject.Inject

/**
 * Nombre: SaveItemUseCases.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Casos de uso para guardar un producto con su foto opcional: el alta (HU-INV-02) y la edición
 * (HU-INV-03, HU-INV-05). Suben la foto antes del documento y, si el documento no se guarda, borran la foto
 * recién subida para no dejar el producto a medias (CA-INV-05-06).
 */

/**
 * Agrega un producto nuevo al inventario.
 *
 * @param repository Repositorio de productos.
 * @param photoRepository Repositorio de fotos en la nube.
 * @param validate Reglas del formulario.
 * @param reminderScheduler Aviso diario de productos por vencer.
 */
class AddItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val photoRepository: PhotoRepository,
    private val validate: ValidateItemUseCase,
    private val reminderScheduler: ExpiryReminderScheduler
) {
    /**
     * Valida, genera el id, sube la foto si hay y guarda el producto (CA-INV-02-06). Con éxito programa el aviso
     * diario, que no se duplica si ya estaba programado (CA-INV-08-01).
     *
     * @param photo Foto tomada con la cámara, o `null` si el producto va sin foto.
     * @return El producto guardado.
     */
    suspend operator fun invoke(draft: ItemDraft, photo: LocalPhoto?): Result<InventoryItem> {
        val valid = validate(draft).getOrElse { return Result.failure(it) }
        val id = repository.newItemId()
        val uploaded = photo?.let { photoRepository.upload(id, it).getOrElse { error -> return Result.failure(error) } }
        val item = valid.toItem(id, uploaded)
        repository.saveItem(item).onFailure { error ->
            uploaded?.let { photoRepository.delete(it.path) }
            return Result.failure(error)
        }
        reminderScheduler.schedule()
        return Result.success(item)
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
 * @param photoRepository Repositorio de fotos en la nube.
 * @param validate Reglas del formulario.
 */
class UpdateItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val photoRepository: PhotoRepository,
    private val validate: ValidateItemUseCase
) {
    /**
     * Valida, sube la foto nueva si cambió, guarda el producto y, solo después, borra la foto anterior si se
     * reemplazó o se quitó (CA-INV-03-02, CA-INV-05-04, CA-INV-05-05). Si el borrado de la foto anterior falla, el
     * cambio ya quedó guardado y la operación no falla.
     *
     * @param original Producto tal como está guardado.
     * @return El producto actualizado.
     */
    suspend operator fun invoke(original: InventoryItem, draft: ItemDraft, photoEdit: PhotoEdit): Result<InventoryItem> {
        val valid = validate(draft, originalExpiresOn = original.expiresOn).getOrElse { return Result.failure(it) }
        val newPhoto = when (photoEdit) {
            PhotoEdit.Keep -> original.photo
            PhotoEdit.Remove -> null
            is PhotoEdit.Replace ->
                photoRepository.upload(original.id, photoEdit.photo).getOrElse { return Result.failure(it) }
        }
        val item = valid.toItem(original.id, newPhoto)
        repository.saveItem(item).onFailure { error ->
            if (photoEdit is PhotoEdit.Replace) newPhoto?.let { photoRepository.delete(it.path) }
            return Result.failure(error)
        }
        if (photoEdit != PhotoEdit.Keep) original.photo?.let { photoRepository.delete(it.path) }
        return Result.success(item)
    }
}

/** Arma el producto a partir de los datos validados. */
private fun ValidItem.toItem(id: String, photo: ItemPhoto?) =
    InventoryItem(id = id, name = name, category = category, expiresOn = expiresOn, quantity = quantity, photo = photo, price = price)
