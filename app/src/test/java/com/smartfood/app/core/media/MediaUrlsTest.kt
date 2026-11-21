package com.smartfood.app.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: MediaUrlsTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Rutas y URLs de las fotos guardadas en Firestore (spec 012, RF-SPK-06).
 */
class MediaUrlsTest {

    @Test
    fun `CA-SPK-05-01 la foto de un producto tiene ruta de media y URL smartfood-media`() {
        val ref = MediaRef.inventoryPhoto("uid1", "item1", "abc")
        assertEquals("users/uid1/media/inv-item1-abc", ref.path)
        assertEquals("smartfood-media://users/uid1/media/inv-item1-abc", ref.url)
    }

    @Test
    fun `CA-SPK-05-06 la ruta y la URL vuelven a dar la misma foto`() {
        val ref = MediaRef.profilePhoto("uid1", "xyz")
        assertEquals(ref, MediaRef.fromPath(ref.path))
        assertEquals(ref, MediaRef.fromUrl(ref.url))
    }

    @Test
    fun `CA-SPK-05-05 una ruta o URL de Storage no es de media`() {
        assertFalse(MediaRef.isMediaPath("users/uid1/inventory/item1/foto.jpg"))
        assertFalse(MediaRef.isMediaPath("users/uid1/profile/foto.jpg"))
        assertNull(MediaRef.fromUrl("https://firebasestorage.googleapis.com/v0/b/x/o/foto.jpg"))
        assertTrue(MediaRef.isMediaPath("users/uid1/media/profile-1"))
    }
}
