package com.smartfood.app.feature.profile.ui.edit

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Nombre: ProfilePhotoFiles.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Archivos temporales de la foto de perfil (spec 008, plan §5, CA-PER-02-08). La cámara del sistema
 * (`ActivityResultContracts.TakePicture`) escribe en un `Uri` del `FileProvider` de la app; la foto de la galería se
 * copia junto a ella. Todo vive en `cacheDir/profile/` y se borra al subir la foto.
 */
object ProfilePhotoFiles {

    /** Debe coincidir con `android:authorities` del `FileProvider` en `AndroidManifest.xml`. */
    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    /** Archivo nuevo para la cámara del sistema y su `Uri` compartible. */
    fun newCameraTarget(context: Context): Pair<File, Uri> {
        val file = newFile(context)
        return file to FileProvider.getUriForFile(context, authority(context), file)
    }

    /** Copia la foto elegida en la galería a un archivo de la app, o `null` si no se pudo leer. */
    fun copyFromGallery(context: Context, uri: Uri): File? {
        val file = newFile(context)
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { source -> file.outputStream().use { target -> source.copyTo(target) } }
            file
        } catch (e: IOException) {
            file.delete()
            null
        } catch (e: SecurityException) {
            // El selector da permiso temporal sobre la foto; si se revocó, se trata como una foto ilegible.
            file.delete()
            null
        }
    }

    private fun newFile(context: Context): File {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    private const val DIR = "profile"
}
