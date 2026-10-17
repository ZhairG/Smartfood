package com.smartfood.app.feature.inventory.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartfood.app.feature.auth.domain.repository.AuthRepository
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.toInventoryError
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Nombre: PhotoDeleteWorker.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Borra en segundo plano una foto de la nube (spec 006 §13, T048). Corre solo con conexión y se
 * reintenta solo, así que la foto de un producto borrado sin red se borra cuando vuelve la conexión
 * (CA-INV-04-02, CA-INV-10-03). Borrar es idempotente: una foto que ya no existe cuenta como borrada.
 *
 * @param appContext Contexto de la app.
 * @param params Datos del trabajo: [KEY_PATH].
 * @param authRepository Sesión activa: sin sesión no se puede borrar.
 * @param photoRepository Fotos en la nube.
 */
@HiltWorker
class PhotoDeleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val photoRepository: PhotoRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_PATH) ?: return Result.failure()
        if (authRepository.currentUserId() == null) return Result.success()
        return photoRepository.delete(path).fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                when (error.toInventoryError()) {
                    // Las reglas no dejan borrarla (por ejemplo, es de otra cuenta): reintentar no cambia nada.
                    InventoryError.PermissionDenied, InventoryError.NotAuthenticated -> Result.success()
                    else -> Result.retry()
                }
            }
        )
    }

    companion object {
        /** Ruta en la nube de la foto que se borra. */
        const val KEY_PATH = "path"
    }
}
