package com.smartfood.app.feature.inventory.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.smartfood.app.feature.inventory.data.local.DataStoreInventoryPreferences
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Nombre: DataStoreInventoryPreferencesTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [DataStoreInventoryPreferences] con un DataStore temporal (spec 006, T020).
 * DataStore admite una sola instancia activa por archivo, así que cada prueba usa su propio archivo temporal.
 */
class DataStoreInventoryPreferencesTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    @Test
    fun `CA-INV-08-05 por defecto el permiso de notificaciones no se ha pedido`() = runTest {
        val preferences = DataStoreInventoryPreferences(nuevoDataStore())

        assertFalse(preferences.wasNotificationPermissionRequested())
    }

    @Test
    fun `CA-INV-08-05 marcar el permiso se conserva en la siguiente lectura`() = runTest {
        val preferences = DataStoreInventoryPreferences(nuevoDataStore())

        preferences.markNotificationPermissionRequested()

        assertTrue(preferences.wasNotificationPermissionRequested())
    }

    /** DataStore respaldado por un archivo temporal nuevo; el alcance muere con la prueba. */
    private fun TestScope.nuevoDataStore() = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(carpeta.root, "prefs.preferences_pb") }
    )
}
