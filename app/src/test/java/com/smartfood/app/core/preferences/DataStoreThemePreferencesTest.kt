package com.smartfood.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartfood.app.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Nombre: DataStoreThemePreferencesTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de [DataStoreThemePreferences] sobre un DataStore real de archivo temporal (spec 008, HU-PER-06,
 * T018): el tema por defecto es claro, se guarda el elegido y un valor desconocido vuelve al de por defecto.
 */
class DataStoreThemePreferencesTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: DataStoreThemePreferences

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "theme.preferences_pb") }
        )
        preferences = DataStoreThemePreferences(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `por defecto el tema es claro (CA-PER-06-03)`() = runTest {
        assertEquals(ThemeMode.Default, preferences.themeMode.first())
    }

    @Test
    fun `guardar el tema oscuro lo conserva (CA-PER-06-02)`() = runTest {
        preferences.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, preferences.themeMode.first())
    }

    @Test
    fun `guardar seguir al telefono lo conserva (CA-PER-06-02)`() = runTest {
        preferences.setThemeMode(ThemeMode.SYSTEM)

        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
    }

    @Test
    fun `un valor desconocido vuelve al tema por defecto (CA-PER-06-04)`() = runTest {
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "DESCONOCIDO" }

        assertEquals(ThemeMode.Default, preferences.themeMode.first())
    }
}
