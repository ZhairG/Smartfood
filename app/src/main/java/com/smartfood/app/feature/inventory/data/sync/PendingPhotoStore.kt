package com.smartfood.app.feature.inventory.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nombre: PendingPhotoStore.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Guarda en el teléfono las fotos que esperan subirse, en `filesDir/pending/{itemId}/{uuid}.jpg`
 * (spec 006 §13, T048). No usa `cacheDir`, porque Android puede borrarlo. Cada foto tiene un nombre único para que una
 * subida vieja no borre la foto nueva del mismo producto. Publica las fotos pendientes como id → ruta (CA-INV-10-04).
 *
 * @param root Carpeta de las fotos pendientes. La app usa `filesDir/pending`; las pruebas, una carpeta aparte.
 */
@Singleton
class PendingPhotoStore internal constructor(private val root: File) {

    /** @param context Contexto de la app, para llegar a `filesDir`. */
    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, PENDING_DIR))

    private val _pending = MutableStateFlow(scan())

    /** Fotos pendientes: id del producto → ruta de la foto más reciente. */
    val pending: StateFlow<Map<String, String>> = _pending.asStateFlow()

    /**
     * Mueve [source] a la carpeta del producto y borra las fotos que ya esperaban para ese producto: la nueva las
     * reemplaza.
     *
     * @return La foto ya guardada en la carpeta de pendientes.
     */
    fun put(itemId: String, source: File): File {
        val directory = File(root, itemId)
        directory.listFiles()?.forEach { it.delete() }
        directory.mkdirs()
        val target = File(directory, "${UUID.randomUUID()}.jpg")
        // Están en el mismo almacenamiento, así que normalmente basta con renombrar; si no, se copia.
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
        refresh()
        return target
    }

    /** Borra la foto [file] de un producto, si sigue ahí, después de subirla o al descartarla. */
    fun remove(file: File) {
        file.delete()
        file.parentFile?.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
        refresh()
    }

    /** Borra todas las fotos pendientes del producto [itemId]. */
    fun removeAll(itemId: String) {
        File(root, itemId).deleteRecursively()
        refresh()
    }

    /** Borra todas las fotos pendientes, como al cerrar sesión (CA-INV-10-07). */
    fun clear() {
        root.deleteRecursively()
        refresh()
    }

    private fun refresh() {
        _pending.value = scan()
    }

    /** Lee la carpeta: por producto, la foto más reciente. */
    private fun scan(): Map<String, String> =
        root.listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { directory ->
                directory.listFiles().orEmpty().maxByOrNull { it.lastModified() }?.let { directory.name to it.path }
            }
            .toMap()

    private companion object {
        const val PENDING_DIR = "pending"
    }
}
