package com.smartfood.app.feature.inventory.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.inventory.data.mapper.mapInventoryError
import com.smartfood.app.feature.inventory.data.photo.PhotoCompressor
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.InventoryException
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Nombre: FirebaseStoragePhotoRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [PhotoRepository] sobre Cloud Storage (spec 006, RF-INV-04, RNF-INV-02, plan §4).
 * Cada foto se reduce con [PhotoCompressor] y se sube a `users/{uid}/inventory/{itemId}/{uuid}.jpg`, protegida por
 * `storage.rules`. El nombre único evita que la caché muestre una foto vieja; por eso las fotos no se sobrescriben.
 *
 * @param storage Instancia de [FirebaseStorage].
 * @param auth Instancia de [FirebaseAuth] de la que sale el uid del usuario con sesión activa.
 * @param compressor Reduce la foto antes de subirla.
 */
class FirebaseStoragePhotoRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val compressor: PhotoCompressor
) : PhotoRepository {

    override suspend fun upload(itemId: String, photo: LocalPhoto): Result<ItemPhoto> = withContext(Dispatchers.IO) {
        safeRunCatching {
            val uid = auth.currentUser?.uid ?: throw InventoryException(InventoryError.NotAuthenticated)
            val file = File(photo.filePath)
            // Una foto ilegible no se puede subir: es un fallo de la foto, no un error desconocido (CA-INV-05-06).
            val bytes = try {
                compressor.compress(file)
            } catch (e: IOException) {
                throw InventoryException(InventoryError.PhotoUploadFailed)
            }
            val path = "$USERS/$uid/$INVENTORY/$itemId/${UUID.randomUUID()}.jpg"
            val reference = storage.reference.child(path)
            reference.putBytes(bytes, StorageMetadata.Builder().setContentType(JPEG).build()).await()
            val url = reference.downloadUrl.await().toString()
            // El archivo temporal solo se borra si la subida salió bien: si falla, el formulario lo conserva para
            // reintentar (CA-INV-05-06).
            file.delete()
            ItemPhoto(path, url)
        }.mapInventoryError()
    }

    override suspend fun delete(path: String): Result<Unit> = safeRunCatching {
        try {
            storage.reference.child(path).delete().await()
        } catch (e: StorageException) {
            // Borrar es idempotente: si la foto ya no existe, el resultado es el mismo.
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }
        Unit
    }.mapInventoryError()

    private companion object {
        const val USERS = "users"
        const val INVENTORY = "inventory"
        const val JPEG = "image/jpeg"
    }
}
