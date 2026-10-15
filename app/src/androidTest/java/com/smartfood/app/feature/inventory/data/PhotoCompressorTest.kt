package com.smartfood.app.feature.inventory.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfood.app.feature.inventory.data.photo.PhotoCompressor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Nombre: PhotoCompressorTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Prueba en el dispositivo de [PhotoCompressor] (spec 006, T018, RNF-INV-02): una foto grande y girada
 * por EXIF sale enderezada, con el lado mayor en 1080 px, en JPEG y con menos de 2 MB. No usa la red.
 */
class PhotoCompressorTest {

    private val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    private val archivos = mutableListOf<File>()

    @After
    fun borrarArchivos() {
        archivos.forEach { it.delete() }
    }

    /** Crea un JPEG de [width]x[height] con la orientación EXIF indicada, como lo guarda la cámara. */
    private fun fotoDePrueba(width: Int, height: Int, orientation: Int): File {
        val file = File(cacheDir, "prueba_${System.nanoTime()}.jpg").also { archivos += it }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply { eraseColor(Color.rgb(46, 125, 50)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        return file
    }

    private fun bounds(bytes: ByteArray) = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
    }

    @Test
    fun rnfInv02_fotoGrandeGiradaSaleEnderezadaA1080EnJpegYMenosDe2Mb() {
        val bytes = PhotoCompressor().compress(fotoDePrueba(4000, 3000, ExifInterface.ORIENTATION_ROTATE_90))

        val resultado = bounds(bytes)
        // Girada 90°: el ancho y el alto se intercambian.
        assertEquals(810, resultado.outWidth)
        assertEquals(1080, resultado.outHeight)
        assertEquals("image/jpeg", resultado.outMimeType)
        assertTrue("Pesa ${bytes.size} bytes", bytes.size < 2 * 1024 * 1024)
    }

    @Test
    fun rnfInv02_fotoPequenaNoSeAgranda() {
        val bytes = PhotoCompressor().compress(fotoDePrueba(800, 600, ExifInterface.ORIENTATION_NORMAL))

        val resultado = bounds(bytes)
        assertEquals(800, resultado.outWidth)
        assertEquals(600, resultado.outHeight)
    }

    @Test(expected = IOException::class)
    fun unArchivoQueNoEsImagenLanzaIOException() {
        val file = File(cacheDir, "no_imagen.jpg").also { archivos += it }
        file.writeText("no soy una foto")

        PhotoCompressor().compress(file)
    }
}
