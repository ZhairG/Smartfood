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
 * Los cálculos viven en `PhotoScaling.kt` y se prueban en la JVM. En el modo gratuito (spec 012), [compressToFit] además
 * asegura que la foto quepa en un documento de Firestore.
 */
class PhotoCompressor @Inject constructor() {

    /**
     * `true` si [file] existe y es una imagen que se puede decodificar. `PhotoUploadWorker` lo usa para distinguir una
     * foto ilegible, que no se va a poder subir nunca, de un fallo pasajero de la subida (CA-INV-05-06).
     */
    fun isReadable(file: File): Boolean {
        if (!file.exists()) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    /**
     * Devuelve los bytes JPEG reducidos de [file].
     *
     * @throws IOException si el archivo no existe o no es una imagen.
     */
    fun compress(file: File): ByteArray {
        val output = decodeReduced(file)
        return try {
            encode(output, PHOTO_JPEG_QUALITY)
        } finally {
            output.recycle()
        }
    }

    /**
     * Igual que [compress], pero baja la calidad (80, 60 y 40) y después el tamaño (75 % cada vez) hasta que la foto pese
     * [maxBytes] o menos, para guardarla en un documento de Firestore (spec 012, CA-SPK-05-01). Una foto normal de
     * 1080 px pesa unos 300 KB, así que casi siempre basta el primer intento.
     *
     * @throws IOException si el archivo no existe, no es una imagen o no se puede reducir lo suficiente.
     */
    fun compressToFit(file: File, maxBytes: Int): ByteArray {
        var bitmap = decodeReduced(file)
        try {
            repeat(MAX_SHRINKS + 1) {
                for (quality in FIT_QUALITIES) {
                    val bytes = encode(bitmap, quality)
                    if (bytes.size <= maxBytes) return bytes
                }
                val smaller = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * SHRINK_FACTOR).toInt().coerceAtLeast(1),
                    (bitmap.height * SHRINK_FACTOR).toInt().coerceAtLeast(1),
                    true
                )
                if (smaller !== bitmap) bitmap.recycle()
                bitmap = smaller
            }
        } finally {
            bitmap.recycle()
        }
        throw IOException("No se pudo reducir ${file.name} a $maxBytes bytes")
    }

    /** Decodifica [file] con muestreo, la endereza y deja el lado mayor en 1080 px. */
    private fun decodeReduced(file: File): Bitmap {
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
        return output
    }

    private fun encode(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }

    private companion object {
        val FIT_QUALITIES = listOf(PHOTO_JPEG_QUALITY, 60, 40)
        const val SHRINK_FACTOR = 0.75f
        const val MAX_SHRINKS = 4
    }
}
