package com.smartfood.app.feature.inventory.data

import com.smartfood.app.feature.inventory.data.photo.ExifTransform
import com.smartfood.app.feature.inventory.data.photo.PixelSize
import com.smartfood.app.feature.inventory.data.photo.exifTransform
import com.smartfood.app.feature.inventory.data.photo.inSampleSizeFor
import com.smartfood.app.feature.inventory.data.photo.scaledSize
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nombre: PhotoScalingTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de los cálculos para reducir las fotos (spec 006, T018, RNF-INV-02, plan §4).
 */
class PhotoScalingTest {

    @Test
    fun `RNF-INV-02 una foto de 4000x3000 se muestrea a la mitad y queda en 1080x810`() {
        assertEquals(2, inSampleSizeFor(4000, 3000))
        // Tras muestrear: 2000x1500.
        assertEquals(PixelSize(1080, 810), scaledSize(2000, 1500))
    }

    @Test
    fun `RNF-INV-02 una foto vertical conserva la proporcion con el alto en 1080`() {
        assertEquals(2, inSampleSizeFor(3000, 4000))
        assertEquals(PixelSize(810, 1080), scaledSize(1500, 2000))
    }

    @Test
    fun `el muestreo nunca deja el lado mayor por debajo de 1080`() {
        assertEquals(4, inSampleSizeFor(8000, 6000))
        assertEquals(1, inSampleSizeFor(2159, 1000))
        assertEquals(2, inSampleSizeFor(2160, 1000))
    }

    @Test
    fun `una foto pequena o de 1080 exacto no se agranda ni se muestrea`() {
        assertEquals(1, inSampleSizeFor(800, 600))
        assertEquals(PixelSize(800, 600), scaledSize(800, 600))
        assertEquals(1, inSampleSizeFor(1080, 720))
        assertEquals(PixelSize(1080, 720), scaledSize(1080, 720))
    }

    @Test
    fun `cada orientacion EXIF tiene su giro y reflejo`() {
        assertEquals(ExifTransform(0, mirror = false), exifTransform(1))   // NORMAL
        assertEquals(ExifTransform(0, mirror = true), exifTransform(2))    // FLIP_HORIZONTAL
        assertEquals(ExifTransform(180, mirror = false), exifTransform(3)) // ROTATE_180
        assertEquals(ExifTransform(180, mirror = true), exifTransform(4))  // FLIP_VERTICAL
        assertEquals(ExifTransform(90, mirror = true), exifTransform(5))   // TRANSPOSE
        assertEquals(ExifTransform(90, mirror = false), exifTransform(6))  // ROTATE_90
        assertEquals(ExifTransform(270, mirror = true), exifTransform(7))  // TRANSVERSE
        assertEquals(ExifTransform(270, mirror = false), exifTransform(8)) // ROTATE_270
    }

    @Test
    fun `una orientacion indefinida o desconocida no transforma`() {
        assertEquals(ExifTransform(0, mirror = false), exifTransform(0))
        assertEquals(ExifTransform(0, mirror = false), exifTransform(42))
    }
}
