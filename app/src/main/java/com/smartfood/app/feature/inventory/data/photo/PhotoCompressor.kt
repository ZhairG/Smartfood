package com.smartfood.app.feature.inventory.data.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Nombre: PhotoCompressor.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Reduce una foto local antes de subirla (spec 006, RNF-INV-02, plan §4): la decodifica con muestreo,
 * la endereza según su orientación EXIF, deja el lado mayor en 1080 px y la comprime en JPEG con calidad 80.
 * Los cálculos viven en `PhotoScaling.kt` y se prueban en la JVM.
 */
class PhotoCompressor @Inject constructor() {

    /**
     * Devuelve los bytes JPEG reducidos de [file].
     *
     * @throws IOException si el archivo no existe o no es una imagen.
     */
    fun compress(file: File): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("No es una imagen: ${file.name}")

        val options = BitmapFactory.Options().apply {
            inSampleSize = inSampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val sampled = BitmapFactory.decodeFile(file.path, options)
            ?: throw IOException("No se pudo decodificar: ${file.name}")

        val transform = exifTransform(
            ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        )
        val target = scaledSize(sampled.width, sampled.height)
        val factor = target.width.toFloat() / sampled.width
        // Escala, reflejo y giro en una sola pasada para no crear bitmaps intermedios.
        val matrix = Matrix().apply {
            postScale(if (transform.mirror) -factor else factor, factor)
            postRotate(transform.rotationDegrees.toFloat())
        }
        val output = Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
        if (output !== sampled) sampled.recycle()

        return try {
            ByteArrayOutputStream().use { stream ->
                output.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, stream)
                stream.toByteArray()
            }
        } finally {
            output.recycle()
        }
    }
}
