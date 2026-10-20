package com.smartfood.app.feature.profile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.inventory.data.photo.PhotoCompressor
import com.smartfood.app.feature.profile.domain.repository.ProfilePhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Nombre: FirebaseStorageProfilePhotoRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [ProfilePhotoRepository] sobre Cloud Storage (spec 008, plan §5, RF-PER-02). La foto
 * se reduce con [PhotoCompressor] (1080 px, JPEG) y se sube a `users/{uid}/profile/{uuid}.jpg`, protegida por
 * `storage.rules`. A diferencia del inventario, no usa WorkManager: sin red, la subida falla con `Network` y la foto
 * anterior se conserva (CA-PER-02-09).
 *
 * @param storage Instancia de [FirebaseStorage].
 * @param auth Instancia de [FirebaseAuth], de la que sale el uid.
 * @param compressor Reduce la foto antes de subirla (el mismo del inventario).
 */
class FirebaseStorageProfilePhotoRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val compressor: PhotoCompressor,
) : ProfilePhotoRepository {

    override suspend fun upload(localPath: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        safeRunCatching {
            val uid = auth.currentUser?.uid ?: throw AuthException(AuthError.AccountNotFound)
            val file = File(localPath)
            val bytes = try {
                compressor.compress(file)
            } catch (e: IOException) {
                throw AuthException(AuthError.Unknown("La foto no se pudo leer"))
            }
            val path = "$USERS/$uid/$PROFILE/${UUID.randomUUID()}.jpg"
            val reference = storage.reference.child(path)
            val task = reference.putBytes(bytes, StorageMetadata.Builder().setContentType(JPEG).build())
            // Sin red, Storage reintenta varios minutos: se espera como mucho UPLOAD_WAIT_MS y se informa como red.
            val done = withTimeoutOrNull(UPLOAD_WAIT_MS) { task.await(); Unit }
            if (done == null) {
                task.cancel()
                throw AuthException(AuthError.Network)
            }
            val url = reference.downloadUrl.await().toString()
            // La foto del teléfono era temporal (cacheDir/profile): ya no se necesita.
            file.delete()
            path to url
        }.mapStorageError()
    }

    override suspend fun delete(path: String): Result<Unit> = safeRunCatching {
        try {
            storage.reference.child(path).delete().await()
        } catch (e: StorageException) {
            // Borrar es idempotente: si la foto ya no existe, el resultado es el mismo.
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }
        Unit
    }.mapStorageError()

    /** Los reintentos agotados de Storage son falta de red; lo demás lo clasifica el mapper de Firebase. */
    private fun <T> Result<T>.mapStorageError(): Result<T> = recoverCatching { error ->
        if (error is StorageException && error.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED) {
            throw AuthException(AuthError.Network)
        }
        throw error
    }.mapFirebaseError()

    private companion object {
        const val USERS = "users"
        const val PROFILE = "profile"
        const val JPEG = "image/jpeg"
        const val UPLOAD_WAIT_MS = 30_000L
    }
}
