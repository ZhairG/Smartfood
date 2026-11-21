package com.smartfood.app.feature.inventory.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.core.media.FirestoreMediaStore
import com.smartfood.app.core.media.MediaRef
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.data.photo.PhotoCompressor
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Nombre: FirestorePhotoRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Fotos del inventario en el modo gratuito (spec 012, CA-SPK-05-01): cada foto se reduce hasta
 * [FirestoreMediaStore.MAX_BYTES] y se guarda en `users/{uid}/media/inv-{itemId}-{uuid}`. Nunca se sobrescribe otra, así
 * que `PhotoUploadWorker` y `PhotoDeleteWorker` siguen igual: suben la nueva y borran la anterior por su ruta
 * (CA-SPK-05-03).
 * @param store almacén de fotos en Firestore.
 * @param auth instancia de Firebase Authentication, de la que sale el uid.
 * @param compressor reduce la foto antes de guardarla.
 */
class FirestorePhotoRepository @Inject constructor(
    private val store: FirestoreMediaStore,
    private val auth: FirebaseAuth,
    private val compressor: PhotoCompressor
) : PhotoRepository {

    override suspend fun upload(itemId: String, photo: LocalPhoto): Result<ItemPhoto> = withContext(Dispatchers.IO) {
        safeRunCatching {
            val uid = auth.currentUser?.uid ?: throw InventoryException(InventoryError.NotAuthenticated)
            // Una foto ilegible no se puede subir: es un fallo de la foto (CA-INV-05-06).
            val bytes = try {
                compressor.compressToFit(File(photo.filePath), FirestoreMediaStore.MAX_BYTES)
            } catch (e: IOException) {
                throw InventoryException(InventoryError.PhotoUploadFailed)
            }
            val ref = MediaRef.inventoryPhoto(uid, itemId, UUID.randomUUID().toString())
            store.write(ref, bytes)
            ItemPhoto(ref.path, ref.url)
        }.mapInventoryError()
    }

    override suspend fun delete(path: String): Result<Unit> = safeRunCatching {
        val ref = MediaRef.fromPath(path) ?: throw InventoryException(InventoryError.NotFound)
        store.delete(ref)
    }.mapInventoryError()
}
