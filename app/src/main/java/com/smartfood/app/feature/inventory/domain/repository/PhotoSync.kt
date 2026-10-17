package com.smartfood.app.feature.inventory.domain.repository

import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: PhotoSync.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato para subir y borrar las fotos de los productos en segundo plano, aunque no haya conexión en
 * ese momento (spec 006 §13, HU-INV-10, T046). Las fotos esperan en el teléfono y se suben solas cuando vuelve la
 * conexión, con reintentos.
 */
interface PhotoSync {

    /**
     * Guarda la foto en el teléfono y programa su subida para el producto [itemId] (CA-INV-05-06). Si ya había una
     * subida pendiente para ese producto, la reemplaza.
     *
     * @param replacedPath Ruta en la nube de la foto anterior, que se borra después de subir la nueva (CA-INV-05-05).
     */
    fun enqueueUpload(itemId: String, photo: LocalPhoto, replacedPath: String?)

    /** Programa el borrado de la foto que está en [path] en la nube (CA-INV-04-02, CA-INV-10-03). */
    fun enqueueDelete(path: String)

    /** Cancela la subida pendiente del producto [itemId] y borra su foto del teléfono, si había. */
    fun cancelUpload(itemId: String)

    /** Fotos que esperan subirse: id del producto → ruta del archivo en el teléfono (CA-INV-10-04). */
    fun pendingPhotos(): Flow<Map<String, String>>

    /** `true` si hay fotos esperando subirse (CA-INV-10-07). */
    fun hasPending(): Boolean

    /** Cancela todas las subidas y borrados pendientes y borra las fotos del teléfono. Se usa al cerrar sesión. */
    fun clearAll()
}
