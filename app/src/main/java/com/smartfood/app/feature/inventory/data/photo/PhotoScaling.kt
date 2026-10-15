package com.smartfood.app.feature.inventory.data.photo

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Nombre: PhotoScaling.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Cálculos puros para reducir una foto antes de subirla (spec 006, RNF-INV-02, plan §4): el muestreo al
 * decodificar, el tamaño final con el lado mayor en 1080 px y la transformación según la orientación EXIF. No usa
 * Android, así que se prueba en la JVM.
 */

/** Lado mayor de la foto subida, en píxeles (RNF-INV-02). */
const val MAX_PHOTO_SIDE = 1080

/** Calidad JPEG de la foto subida (RNF-INV-02). */
const val PHOTO_JPEG_QUALITY = 80

/** Ancho y alto en píxeles. */
data class PixelSize(val width: Int, val height: Int)

/**
 * Transformación que endereza una foto según su orientación EXIF.
 *
 * @property rotationDegrees Giro en sentido horario: 0, 90, 180 o 270.
 * @property mirror Si la imagen se refleja horizontalmente antes de girar (cámaras frontales).
 */
data class ExifTransform(val rotationDegrees: Int, val mirror: Boolean)

/**
 * La mayor potencia de 2 que deja el lado mayor en al menos [maxSide] al decodificar: así la imagen en memoria es
 * pequeña y el escalado final no pierde nitidez.
 */
fun inSampleSizeFor(width: Int, height: Int, maxSide: Int = MAX_PHOTO_SIDE): Int {
    val longest = max(width, height)
    var sample = 1
    while (longest / (sample * 2) >= maxSide) sample *= 2
    return sample
}

/** Tamaño con el lado mayor en [maxSide] y la misma proporción. Nunca agranda una imagen más pequeña. */
fun scaledSize(width: Int, height: Int, maxSide: Int = MAX_PHOTO_SIDE): PixelSize {
    val longest = max(width, height)
    if (longest <= maxSide) return PixelSize(width, height)
    val factor = maxSide.toFloat() / longest
    return PixelSize((width * factor).roundToInt(), (height * factor).roundToInt())
}

/**
 * Transformación para cada valor de `ExifInterface.ORIENTATION_*`. Los valores se repiten aquí para no depender de
 * Android; un valor desconocido o indefinido no transforma.
 */
fun exifTransform(orientation: Int): ExifTransform = when (orientation) {
    ORIENTATION_FLIP_HORIZONTAL -> ExifTransform(0, mirror = true)
    ORIENTATION_ROTATE_180 -> ExifTransform(180, mirror = false)
    ORIENTATION_FLIP_VERTICAL -> ExifTransform(180, mirror = true)
    ORIENTATION_TRANSPOSE -> ExifTransform(90, mirror = true)
    ORIENTATION_ROTATE_90 -> ExifTransform(90, mirror = false)
    ORIENTATION_TRANSVERSE -> ExifTransform(270, mirror = true)
    ORIENTATION_ROTATE_270 -> ExifTransform(270, mirror = false)
    else -> ExifTransform(0, mirror = false)
}

private const val ORIENTATION_FLIP_HORIZONTAL = 2
private const val ORIENTATION_ROTATE_180 = 3
private const val ORIENTATION_FLIP_VERTICAL = 4
private const val ORIENTATION_TRANSPOSE = 5
private const val ORIENTATION_ROTATE_90 = 6
private const val ORIENTATION_TRANSVERSE = 7
private const val ORIENTATION_ROTATE_270 = 8
