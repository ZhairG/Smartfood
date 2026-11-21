package com.smartfood.app.feature.profile.data.repository

import com.smartfood.app.core.backend.BackendMode
import com.smartfood.app.core.backend.BackendModeProvider
import com.smartfood.app.core.media.MediaRef
import com.smartfood.app.feature.profile.domain.repository.ProfilePhotoRepository
import javax.inject.Inject

/**
 * Nombre: ModeAwareProfilePhotoRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Foto del perfil que se registra en Hilt (spec 012, RF-SPK-02), con el mismo criterio que las fotos del
 * inventario: sube según el modo y borra según la ruta. Sin Blaze, una foto de Storage no se toca (CA-SPK-05-05).
 * @param mode interruptor del backend.
 * @param blaze foto en Storage (spec 008).
 * @param free foto en Firestore (spec 012).
 */
class ModeAwareProfilePhotoRepository(
    private val mode: BackendModeProvider,
    private val blaze: ProfilePhotoRepository,
    private val free: ProfilePhotoRepository
) : ProfilePhotoRepository {

    @Inject
    constructor(
        mode: BackendModeProvider,
        storage: FirebaseStorageProfilePhotoRepository,
        firestore: FirestoreProfilePhotoRepository
    ) : this(mode, blaze = storage, free = firestore)

    override suspend fun upload(localPath: String): Result<Pair<String, String>> =
        if (mode.current() == BackendMode.BLAZE) blaze.upload(localPath) else free.upload(localPath)

    override suspend fun delete(path: String): Result<Unit> = when {
        MediaRef.isMediaPath(path) -> free.delete(path)
        mode.current() == BackendMode.BLAZE -> blaze.delete(path)
        else -> Result.success(Unit)
    }
}
