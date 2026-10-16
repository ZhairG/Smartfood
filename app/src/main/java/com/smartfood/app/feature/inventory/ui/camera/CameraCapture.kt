package com.smartfood.app.feature.inventory.ui.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
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

/**
 * Copia la foto elegida en la galería a `filesDir/photos/{uuid}.jpg`, la misma carpeta de la cámara, para que siga el
 * mismo camino: compresión, subida y borrado (CA-INV-05-07). Se llama fuera del hilo principal.
 *
 * @param context Contexto de la app.
 * @param uri Foto elegida en el selector de fotos.
 * @return El archivo copiado, o `null` si no se pudo leer o el permiso sobre la foto ya no está vigente.
 */
fun copyPickedPhoto(context: Context, uri: Uri): File? {
    val directory = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source -> file.outputStream().use { target -> source.copyTo(target) } }
        file
    } catch (e: IOException) {
        file.delete()
        null
    } catch (e: SecurityException) {
        // El selector da permiso temporal sobre la foto; si se revocó, se ignora como una foto ilegible.
        file.delete()
        null
    }
}
