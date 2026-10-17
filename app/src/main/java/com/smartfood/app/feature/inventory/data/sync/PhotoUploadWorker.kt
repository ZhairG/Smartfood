package com.smartfood.app.feature.inventory.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.inventory.data.photo.PhotoCompressor
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.toInventoryError
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Nombre: PhotoUploadWorker.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Sube en segundo plano la foto de un producto y la pone en su documento (spec 006 §13, T048). Corre
 * solo con conexión y se reintenta solo, así que un producto guardado sin red recibe su foto cuando vuelve la
 * conexión (CA-INV-05-06, CA-INV-10-04). Nunca deja un producto con una foto rota.
 *
 * @param appContext Contexto de la app.
 * @param params Datos del trabajo: [KEY_ITEM_ID], [KEY_FILE_PATH] y, si reemplaza otra, [KEY_REPLACED_PATH].
 * @param authRepository Sesión activa: sin sesión no se sube nada.
 * @param inventoryRepository Productos, para poner la foto en el documento.
 * @param photoRepository Fotos en la nube.
 * @param photoSync Para encolar el borrado de la foto anterior si no se pudo borrar enseguida.
 * @param store Fotos que esperan subirse en el teléfono.
 * @param compressor Comprueba que la foto se pueda leer.
 */
@HiltWorker
class PhotoUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val inventoryRepository: InventoryRepository,
    private val photoRepository: PhotoRepository,
    private val photoSync: PhotoSync,
    private val store: PendingPhotoStore,
    private val compressor: PhotoCompressor
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        val file = File(inputData.getString(KEY_FILE_PATH) ?: return Result.failure())
        // Sin sesión no hay a qué cuenta subirla; al cerrar sesión se descartan las pendientes (CA-INV-10-07).
        if (authRepository.currentUserId() == null) return Result.success()
        // Si ya no está, se canceló o la reemplazó otra foto más nueva: no hay nada que subir.
        if (!file.exists()) return Result.success()
        // Una foto ilegible nunca se va a poder subir: el producto queda sin foto (CA-INV-05-06).
        if (!compressor.isReadable(file)) {
            store.remove(file)
            return Result.success()
        }

        val photo = photoRepository.upload(itemId, LocalPhoto(file.path)).getOrElse { error ->
            return when (error.toInventoryError()) {
                InventoryError.NotAuthenticated -> Result.success()
                // Las reglas la rechazan: reintentar no cambia nada.
                InventoryError.PermissionDenied -> discard(file)
                // Un fallo de la subida que se repite muchas veces se abandona para no reintentar para siempre.
                InventoryError.PhotoUploadFailed ->
                    if (runAttemptCount >= MAX_ATTEMPTS) discard(file) else Result.retry()
                else -> Result.retry()
            }
        }

        inventoryRepository.setPhoto(itemId, photo).onFailure { error ->
            // La foto subida no quedó en ningún producto: se borra para no dejarla huérfana en la nube.
            photoRepository.delete(photo.path)
            return when (error.toInventoryError()) {
                // El producto se borró mientras la foto esperaba, o las reglas no dejan ponerla: no hay a qué ponerla.
                InventoryError.NotFound, InventoryError.PermissionDenied -> discard(file)
                else -> Result.retry()
            }
        }

        store.remove(file)
        // La foto anterior se borra solo después de que la nueva quedó en el producto (CA-INV-05-05).
        inputData.getString(KEY_REPLACED_PATH)?.let { replaced ->
            photoRepository.delete(replaced).onFailure { photoSync.enqueueDelete(replaced) }
        }
        return Result.success()
    }

    /** Borra la foto del teléfono y termina sin reintentar: el producto queda sin esa foto. */
    private fun discard(file: File): Result {
        store.remove(file)
        return Result.success()
    }

    companion object {
        /** Id del producto al que va la foto. */
        const val KEY_ITEM_ID = "itemId"

        /** Ruta de la foto en `filesDir/pending`. */
        const val KEY_FILE_PATH = "filePath"

        /** Ruta en la nube de la foto que reemplaza, para borrarla después (CA-INV-05-05). */
        const val KEY_REPLACED_PATH = "replacedPath"

        /** Intentos tras los que se abandona una subida que falla siempre por la foto. */
        const val MAX_ATTEMPTS = 5
    }
}
