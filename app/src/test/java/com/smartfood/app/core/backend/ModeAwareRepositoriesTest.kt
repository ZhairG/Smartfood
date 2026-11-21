package com.smartfood.app.core.backend

import com.smartfood.app.feature.auth.data.repository.ModeAwarePasswordRecoveryRepository
import com.smartfood.app.feature.inventory.data.repository.ModeAwarePhotoRepository
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.profile.data.repository.ModeAwareEmailChangeRepository
import com.smartfood.app.feature.profile.data.repository.ModeAwareProfilePhotoRepository
import com.smartfood.app.testutil.FakeProfilePhotoRepository
import com.smartfood.app.testutil.FakePhotoRepository
import com.smartfood.app.testutil.FakeEmailChangeRepository
import com.smartfood.app.testutil.FakeBackendModeProvider
import com.smartfood.app.testutil.FakePasswordRecoveryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: ModeAwareRepositoriesTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Los repositorios que se registran en Hilt delegan según el interruptor del backend (spec 012,
 * RF-SPK-02, CA-SPK-01-01 y CA-SPK-01-02).
 */
class ModeAwareRepositoriesTest {

    @Test
    fun `CA-SPK-01-01 CA-SPK-01-02 la recuperacion usa el codigo en Blaze y el enlace en el modo gratuito`() = runTest {
        val mode = FakeBackendModeProvider(BackendMode.FREE)
        val codigo = FakePasswordRecoveryRepository(usesCode = true)
        val enlace = FakePasswordRecoveryRepository(usesCode = false)
        val repository = ModeAwarePasswordRecoveryRepository(mode, blaze = codigo, free = enlace)

        assertFalse(repository.usesCode)
        repository.requestCode("demo@smartfood.com")
        assertEquals(1, enlace.requestCodeCalls)

        mode.mode = BackendMode.BLAZE
        assertTrue(repository.usesCode)
        repository.requestCode("demo@smartfood.com")
        assertEquals(1, codigo.requestCodeCalls)
    }

    @Test
    fun `CA-SPK-03-01 el cambio de correo usa el codigo en Blaze y el enlace en el modo gratuito`() = runTest {
        val mode = FakeBackendModeProvider(BackendMode.FREE)
        val codigo = FakeEmailChangeRepository(usesCode = true)
        val enlace = FakeEmailChangeRepository(usesCode = false)
        val repository = ModeAwareEmailChangeRepository(mode, blaze = codigo, free = enlace)

        assertFalse(repository.usesCode)
        repository.requestCode("nuevo@correo.com")
        assertEquals(listOf("nuevo@correo.com"), enlace.requested)

        mode.mode = BackendMode.BLAZE
        assertTrue(repository.usesCode)
        repository.requestCode("otro@correo.com")
        assertEquals(listOf("otro@correo.com"), codigo.requested)
    }

    @Test
    fun `CA-SPK-05-01 la foto de un producto sube a Storage en Blaze y a Firestore en el modo gratuito`() = runTest {
        val mode = FakeBackendModeProvider(BackendMode.FREE)
        val storage = FakePhotoRepository()
        val firestore = FakePhotoRepository()
        val repository = ModeAwarePhotoRepository(mode, blaze = storage, free = firestore)

        repository.upload("item1", LocalPhoto("/fotos/a.jpg"))
        assertEquals(1, firestore.uploads.size)

        mode.mode = BackendMode.BLAZE
        repository.upload("item1", LocalPhoto("/fotos/b.jpg"))
        assertEquals(1, storage.uploads.size)
    }

    @Test
    fun `CA-SPK-05-03 CA-SPK-05-05 borrar decide por la ruta y no toca Storage en el modo gratuito`() = runTest {
        val mode = FakeBackendModeProvider(BackendMode.FREE)
        val storage = FakePhotoRepository()
        val firestore = FakePhotoRepository()
        val repository = ModeAwarePhotoRepository(mode, blaze = storage, free = firestore)

        // Una foto de media se borra en Firestore en cualquier modo.
        assertTrue(repository.delete("users/uid1/media/inv-item1-a").isSuccess)
        mode.mode = BackendMode.BLAZE
        assertTrue(repository.delete("users/uid1/media/inv-item1-b").isSuccess)
        assertEquals(listOf("users/uid1/media/inv-item1-a", "users/uid1/media/inv-item1-b"), firestore.deletedPaths)

        // Una foto de Storage se borra en Blaze y, sin Blaze, termina bien sin tocar Storage.
        assertTrue(repository.delete("users/uid1/inventory/item1/foto.jpg").isSuccess)
        mode.mode = BackendMode.FREE
        assertTrue(repository.delete("users/uid1/inventory/item1/otra.jpg").isSuccess)
        assertEquals(listOf("users/uid1/inventory/item1/foto.jpg"), storage.deletedPaths)
    }

    @Test
    fun `CA-SPK-05-02 la foto del perfil sube segun el modo y borra segun la ruta`() = runTest {
        val mode = FakeBackendModeProvider(BackendMode.FREE)
        val storage = FakeProfilePhotoRepository()
        val firestore = FakeProfilePhotoRepository()
        val repository = ModeAwareProfilePhotoRepository(mode, blaze = storage, free = firestore)

        repository.upload("/fotos/perfil.jpg")
        assertEquals(listOf("/fotos/perfil.jpg"), firestore.uploads)
        assertTrue(storage.uploads.isEmpty())

        // Sin Blaze, la foto anterior que sigue en Storage no se toca (CA-SPK-05-05).
        assertTrue(repository.delete("users/uid1/profile/vieja.jpg").isSuccess)
        assertTrue(storage.deletes.isEmpty())
        assertTrue(repository.delete("users/uid1/media/profile-1").isSuccess)
        assertEquals(listOf("users/uid1/media/profile-1"), firestore.deletes)
    }
}
