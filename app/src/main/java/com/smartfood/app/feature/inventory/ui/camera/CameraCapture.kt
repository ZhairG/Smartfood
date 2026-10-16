package com.smartfood.app.feature.inventory.ui.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Nombre: CameraCapture.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Captura de la foto del producto con CameraX (spec 006, T033, plan §4). La foto se guarda en
 * `filesDir/photos/{uuid}.jpg` y no en la caché: Android no la borra sola mientras espera a subirse (#71).
 * `FirebaseStoragePhotoRepository` la borra después de subirla bien.
 */

/** Clave del `savedStateHandle` con la que la cámara devuelve la ruta de la foto al editor (plan §4). */
const val CAPTURED_PHOTO_KEY = "captured_photo"

/** Carpeta de `filesDir` donde quedan las fotos tomadas hasta que se suben. */
private const val PHOTOS_DIR = "photos"

/**
 * Toma una foto y la guarda en un archivo nuevo.
 *
 * @param context Contexto de la app, para la carpeta y el ejecutor principal.
 * @return El archivo guardado, o el error de CameraX. Si falla, no queda ningún archivo a medias.
 */
suspend fun LifecycleCameraController.takePhoto(context: Context): Result<File> {
    val directory = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    return suspendCancellableCoroutine { continuation ->
        takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    continuation.resume(Result.success(file))
                }

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    continuation.resume(Result.failure(exception))
                }
            }
        )
    }
}
