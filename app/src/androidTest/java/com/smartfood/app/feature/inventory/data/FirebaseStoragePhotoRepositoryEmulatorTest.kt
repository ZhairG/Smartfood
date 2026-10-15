package com.smartfood.app.feature.inventory.data

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.smartfood.app.BuildConfig
import com.smartfood.app.feature.inventory.data.repository.FirebaseStoragePhotoRepository
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.LocalPhoto
import com.smartfood.app.feature.inventory.domain.model.toInventoryError
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Nombre: FirebaseStoragePhotoRepositoryEmulatorTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Prueba de punta a punta de [FirebaseStoragePhotoRepository] contra los emuladores de Auth y Storage,
 * con las reglas reales de `storage.rules` (spec 006, T018, RNF-INV-01, RNF-INV-02, CA-INV-05-06). Solo se ejecuta
 * en un build con `-PuseFirebaseEmulator=true`. Con un teléfono físico: `adb reverse` de 9099 y 9199 y
 * `-PfirebaseEmulatorHost=127.0.0.1`. Desde el #69, `FirebaseStorage` y [PhotoRepository] salen del grafo real de
 * Hilt (`CoreDataModule` e `InventoryDataModule`), así que la prueba también comprueba ese enlace.
 */
@HiltAndroidTest
class FirebaseStoragePhotoRepositoryEmulatorTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var storage: FirebaseStorage

    @Inject
    lateinit var repository: PhotoRepository
    private val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir

    @Before
    fun prepararEntornoTest() {
        assumeTrue("Solo con -PuseFirebaseEmulator=true", BuildConfig.USE_FIREBASE_EMULATOR)
        hiltRule.inject()
        runBlocking {
            auth.createUserWithEmailAndPassword("foto-${UUID.randomUUID()}@prueba.com", "Clave123*").await()
        }
    }

    @After
    fun cerrarSesion() {
        if (::auth.isInitialized) auth.signOut()
    }

    private fun fotoGrande(): File {
        val file = File(cacheDir, "captura_${System.nanoTime()}.jpg")
        val bitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.RGB_565).apply { eraseColor(0xFF2E7D32.toInt()) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        return file
    }

    @Test
    fun t018_subeReducidaBorraIdempotenteYFallaSinSesion(): Unit = runBlocking {
        val uid = auth.currentUser!!.uid
        val local = fotoGrande()

        // Subir: las reglas aceptan el JPEG reducido en la carpeta del dueño.
        val photo = repository.upload("item1", LocalPhoto(local.path)).getOrThrow()
        assertTrue(photo.path.startsWith("users/$uid/inventory/item1/"))
        assertTrue(photo.url.isNotBlank())
        assertFalse("El archivo temporal se borra tras subir", local.exists())
        val metadata = storage.reference.child(photo.path).metadata.await()
        assertEquals("image/jpeg", metadata.contentType)
        assertTrue("Pesa ${metadata.sizeBytes} bytes", metadata.sizeBytes < 2 * 1024 * 1024)

        // Borrar es idempotente.
        assertTrue(repository.delete(photo.path).isSuccess)
        assertTrue(repository.delete(photo.path).isSuccess)

        // Un archivo que no existe falla con PhotoUploadFailed sin lanzar.
        assertEquals(
            InventoryError.PhotoUploadFailed,
            repository.upload("item1", LocalPhoto(File(cacheDir, "no_existe.jpg").path)).exceptionOrNull()?.toInventoryError()
        )

        // Sin sesión falla con NotAuthenticated y conserva el archivo para reintentar (CA-INV-05-06).
        auth.signOut()
        val otra = fotoGrande()
        assertEquals(
            InventoryError.NotAuthenticated,
            repository.upload("item1", LocalPhoto(otra.path)).exceptionOrNull()?.toInventoryError()
        )
        assertTrue(otra.exists())
        otra.delete()
    }
}
