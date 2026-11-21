package com.smartfood.app.feature.profile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.smartfood.app.core.common.safeRunCatching
import com.smartfood.app.core.media.FirestoreMediaStore
import com.smartfood.app.core.media.MediaRef
import com.smartfood.app.feature.auth.data.mapper.mapFirebaseError
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.AuthException
import com.smartfood.app.feature.inventory.data.photo.PhotoCompressor
import com.smartfood.app.feature.profile.domain.repository.ProfilePhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Nombre: FirestoreProfilePhotoRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Foto del perfil en el modo gratuito (spec 012, CA-SPK-05-02): se reduce hasta
 * [FirestoreMediaStore.MAX_BYTES] y se guarda en `users/{uid}/media/profile-{uuid}`. Igual que con Storage, sin red se
 * espera como mucho [UPLOAD_WAIT_MS] y falla con `Network`, y la foto anterior se conserva (CA-PER-02-09).
 * @param store almacén de fotos en Firestore.
 * @param auth instancia de Firebase Authentication, de la que sale el uid.
 * @param compressor reduce la foto antes de guardarla (el mismo del inventario).
 */
class FirestoreProfilePhotoRepository @Inject constructor(
    private val store: FirestoreMediaStore,
    private val auth: FirebaseAuth,
    private val compressor: PhotoCompressor,
) : ProfilePhotoRepository {

    override suspend fun upload(localPath: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        safeRunCatching {
            val uid = auth.currentUser?.uid ?: throw AuthException(AuthError.AccountNotFound)
            val file = File(localPath)
            val bytes = try {
                compressor.compressToFit(file, FirestoreMediaStore.MAX_BYTES)
            } catch (e: IOException) {
                throw AuthException(AuthError.Unknown("La foto no se pudo leer"))
            }
            val ref = MediaRef.profilePhoto(uid, UUID.randomUUID().toString())
            withTimeoutOrNull(UPLOAD_WAIT_MS) { store.write(ref, bytes) } ?: throw AuthException(AuthError.Network)
            // La foto del teléfono era temporal (cacheDir/profile): ya no se necesita.
            file.delete()
            ref.path to ref.url
        }.mapFirebaseError()
    }

    override suspend fun delete(path: String): Result<Unit> = safeRunCatching {
        MediaRef.fromPath(path)?.let { store.delete(it) }
        Unit
    }.mapFirebaseError()

    private companion object {
        const val UPLOAD_WAIT_MS = 30_000L
    }
}
