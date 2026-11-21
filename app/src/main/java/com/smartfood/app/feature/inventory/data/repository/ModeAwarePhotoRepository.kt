package com.smartfood.app.feature.inventory.data.repository

import com.smartfood.app.core.backend.BackendMode
import com.smartfood.app.core.backend.BackendModeProvider
import com.smartfood.app.core.media.MediaRef
import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import javax.inject.Inject

/**
 * Nombre: ModeAwarePhotoRepository.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Fotos del inventario que se registran en Hilt (spec 012, RF-SPK-02).
 * - Subir: a Storage en el modo Blaze y a Firestore en el gratuito.
 * - Borrar: según la **ruta**, no el modo. Una foto de `media` se borra en Firestore en cualquier modo; una de Storage
 *   se borra en Storage en el modo Blaze y, en el gratuito, no se toca, porque Storage no responde (CA-SPK-05-05).
 * @param mode interruptor del backend.
 * @param blaze fotos en Storage (spec 006).
 * @param free fotos en Firestore (spec 012).
 */
class ModeAwarePhotoRepository(
    private val mode: BackendModeProvider,
    private val blaze: PhotoRepository,
    private val free: PhotoRepository
) : PhotoRepository {

    @Inject
    constructor(
        mode: BackendModeProvider,
        storage: FirebaseStoragePhotoRepository,
        firestore: FirestorePhotoRepository
    ) : this(mode, blaze = storage, free = firestore)

    override suspend fun upload(itemId: String, photo: LocalPhoto): Result<ItemPhoto> =
        if (mode.current() == BackendMode.BLAZE) blaze.upload(itemId, photo) else free.upload(itemId, photo)

    override suspend fun delete(path: String): Result<Unit> = when {
        MediaRef.isMediaPath(path) -> free.delete(path)
        mode.current() == BackendMode.BLAZE -> blaze.delete(path)
        else -> Result.success(Unit)
    }
}
