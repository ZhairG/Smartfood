package com.smartfood.app.feature.inventory.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nombre: WorkManagerPhotoSync.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Implementación de [PhotoSync] sobre WorkManager (spec 006 §13, T048). Las fotos esperan en
 * [PendingPhotoStore] y se suben o se borran con [PhotoUploadWorker] y [PhotoDeleteWorker], que corren solo con
 * conexión y se reintentan con espera exponencial. Todos los trabajos llevan la etiqueta [TAG] para cancelarlos
 * juntos al cerrar sesión (CA-INV-10-07).
 *
 * @param context Contexto de la app, para obtener WorkManager.
 * @param store Fotos que esperan subirse.
 */
@Singleton
class WorkManagerPhotoSync @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: PendingPhotoStore
) : PhotoSync {

    private val workManager get() = WorkManager.getInstance(context)

    override fun enqueueUpload(itemId: String, photo: LocalPhoto, replacedPath: String?) {
        val file = store.put(itemId, File(photo.filePath))
        val request = request<PhotoUploadWorker>(
            workDataOf(
                PhotoUploadWorker.KEY_ITEM_ID to itemId,
                PhotoUploadWorker.KEY_FILE_PATH to file.path,
                PhotoUploadWorker.KEY_REPLACED_PATH to replacedPath
            )
        )
        // Una foto nueva del mismo producto reemplaza a la que todavía no se subía.
        workManager.enqueueUniqueWork(uploadName(itemId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun enqueueDelete(path: String) {
        val request = request<PhotoDeleteWorker>(workDataOf(PhotoDeleteWorker.KEY_PATH to path))
        workManager.enqueueUniqueWork("$DELETE_PREFIX$path", ExistingWorkPolicy.KEEP, request)
    }

    override fun cancelUpload(itemId: String) {
        workManager.cancelUniqueWork(uploadName(itemId))
        store.removeAll(itemId)
    }

    override fun pendingPhotos(): Flow<Map<String, String>> = store.pending

    override fun hasPending(): Boolean = store.pending.value.isNotEmpty()

    override fun clearAll() {
        workManager.cancelAllWorkByTag(TAG)
        store.clear()
    }

    private inline fun <reified W : ListenableWorker> request(data: Data): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<W>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()

    private fun uploadName(itemId: String) = "$UPLOAD_PREFIX$itemId"

    companion object {
        /** Etiqueta común de los trabajos de fotos (§13). */
        const val TAG = "photo-sync"
        private const val UPLOAD_PREFIX = "photo-upload-"
        private const val DELETE_PREFIX = "photo-delete-"
        private const val BACKOFF_SECONDS = 30L
    }
}
