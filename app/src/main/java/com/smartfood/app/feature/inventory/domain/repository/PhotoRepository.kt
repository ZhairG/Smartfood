package com.smartfood.app.feature.inventory.domain.repository

import com.smartfood.app.feature.inventory.domain.model.ItemPhoto
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto

/**
 * Nombre: PhotoRepository.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para subir y borrar las fotos de los productos en la nube (RF-INV-04).
 */
interface PhotoRepository {

    /**
     * Reduce y sube la foto local del producto (RNF-INV-02).
     *
     * @param itemId Producto al que pertenece la foto.
     * @return La ruta y la URL de la foto subida.
     */
    suspend fun upload(itemId: String, photo: LocalPhoto): Result<ItemPhoto>

    /** Borra de la nube la foto que está en [path]. */
    suspend fun delete(path: String): Result<Unit>
}
