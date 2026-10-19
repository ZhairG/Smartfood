package com.smartfood.app.feature.profile.domain.repository

/**
 * Nombre: ProfilePhotoRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para subir y borrar la foto de perfil en la nube (spec 008, plan §3 y §5, RF-PER-02). La
 * implementación la reduce a 1080 px en JPEG antes de subirla (CA-PER-02-09).
 */
interface ProfilePhotoRepository {

    /**
     * Sube la foto que está en [localPath] (archivo del teléfono).
     *
     * @return La ruta en Cloud Storage y la URL de descarga, en ese orden.
     */
    suspend fun upload(localPath: String): Result<Pair<String, String>>

    /** Borra la foto de la ruta [path] de Cloud Storage. Borrar una que ya no existe no es un error. */
    suspend fun delete(path: String): Result<Unit>
}
