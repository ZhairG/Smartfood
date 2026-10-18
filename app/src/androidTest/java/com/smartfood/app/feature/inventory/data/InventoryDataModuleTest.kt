package com.smartfood.app.feature.inventory.data

import com.smartfood.app.feature.inventory.data.local.DataStoreInventoryPreferences
import com.smartfood.app.feature.inventory.data.reminder.WorkManagerExpiryReminderScheduler
import com.smartfood.app.feature.inventory.data.repository.FirebaseStoragePhotoRepository
import com.smartfood.app.feature.inventory.data.repository.FirestoreInventoryRepository
import com.smartfood.app.feature.inventory.data.speech.AndroidSpeechToText
import com.smartfood.app.feature.inventory.data.sync.WorkManagerPhotoSync
import com.smartfood.app.feature.inventory.domain.repository.ExpiryReminderScheduler
import com.smartfood.app.feature.inventory.domain.repository.InventoryPreferences
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoSync
import com.smartfood.app.feature.inventory.domain.repository.SpeechToText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/**
 * Nombre: InventoryDataModuleTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Comprueba que el grafo de Hilt de la app provee los repositorios del inventario con sus
 * implementaciones reales (spec 006, T015 y T021, Issue #69), el reloj del dominio (T031, Issue #39) y el aviso de
 * vencimiento (T038, Issue #44). No usa la red: solo construye el grafo, una vez (#105).
 */
@HiltAndroidTest
class InventoryDataModuleTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var inventoryRepository: InventoryRepository

    @Inject
    lateinit var otroInventoryRepository: InventoryRepository

    @Inject
    lateinit var photoRepository: PhotoRepository

    @Inject
    lateinit var speechToText: SpeechToText

    @Inject
    lateinit var photoSync: PhotoSync

    @Inject
    lateinit var otroPhotoSync: PhotoSync

    @Inject
    lateinit var inventoryPreferences: InventoryPreferences

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var otroClock: Clock

    @Inject
    lateinit var expiryReminderScheduler: ExpiryReminderScheduler

    @Inject
    lateinit var otroExpiryReminderScheduler: ExpiryReminderScheduler

    @Before
    fun inyectar() {
        hiltRule.inject()
    }

    @Test
    fun t019_t020_t021_t031_t038_t048_elGrafoProveeLasImplementacionesRealesYLasUnicas() {
        // T021: inventario y fotos con Firebase, y un solo repositorio en la app.
        assertTrue("inventario", inventoryRepository is FirestoreInventoryRepository)
        assertTrue("fotos", photoRepository is FirebaseStoragePhotoRepository)
        assertSame("inventario único", inventoryRepository, otroInventoryRepository)
        // T048: las fotos se sincronizan con WorkManager.
        assertTrue("sincronización de fotos", photoSync is WorkManagerPhotoSync)
        assertSame("sincronización única", photoSync, otroPhotoSync)
        // T019 y T020: voz de Android y preferencias sobre el DataStore.
        assertTrue("voz", speechToText is AndroidSpeechToText)
        assertTrue("preferencias", inventoryPreferences is DataStoreInventoryPreferences)
        // T031: el reloj del dominio usa la zona del teléfono.
        assertEquals("zona del reloj", ZoneId.systemDefault(), clock.zone)
        assertSame("reloj único", clock, otroClock)
        // T038: el aviso de vencimiento con WorkManager.
        assertTrue("aviso de vencimiento", expiryReminderScheduler is WorkManagerExpiryReminderScheduler)
        assertSame("aviso único", expiryReminderScheduler, otroExpiryReminderScheduler)
    }
}
