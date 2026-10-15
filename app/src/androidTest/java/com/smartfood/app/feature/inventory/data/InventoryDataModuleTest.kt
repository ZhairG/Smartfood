package com.smartfood.app.feature.inventory.data

import com.smartfood.app.feature.inventory.data.repository.FirebaseStoragePhotoRepository
import com.smartfood.app.feature.inventory.data.repository.FirestoreInventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.InventoryRepository
import com.smartfood.app.feature.inventory.domain.repository.PhotoRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Nombre: InventoryDataModuleTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Comprueba que el grafo de Hilt de la app provee los repositorios del inventario con sus
 * implementaciones reales (spec 006, T015 y T021, Issue #69). No usa la red: solo construye el grafo.
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

    @Before
    fun inyectar() {
        hiltRule.inject()
    }

    @Test
    fun t021_inventarioYFotosSeInyectanConSusImplementacionesDeFirebase() {
        assertTrue(inventoryRepository is FirestoreInventoryRepository)
        assertTrue(photoRepository is FirebaseStoragePhotoRepository)
    }

    @Test
    fun t021_losRepositoriosSonUnicosEnLaApp() {
        assertSame(inventoryRepository, otroInventoryRepository)
    }
}
