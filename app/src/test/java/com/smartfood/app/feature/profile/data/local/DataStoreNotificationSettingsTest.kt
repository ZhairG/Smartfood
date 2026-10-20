package com.smartfood.app.feature.profile.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.smartfood.app.feature.profile.domain.model.NotificationSettings
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
 * Nombre: DataStoreNotificationSettingsTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas de [DataStoreNotificationSettings] sobre un DataStore real de archivo temporal: los valores por
 * defecto y el guardado de cada interruptor (spec 008, CA-PER-04-01 y CA-PER-04-02).
 */
class DataStoreNotificationSettingsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var settings: DataStoreNotificationSettings

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "notification.preferences_pb") }
        )
        settings = DataStoreNotificationSettings(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `los tres avisos vienen encendidos la primera vez (CA-PER-04-01)`() = runTest {
        assertEquals(NotificationSettings(), settings.observe().first())
    }

    @Test
    fun `guardar un aviso apagado lo conserva y deja los demas igual (CA-PER-04-02)`() = runTest {
        settings.set(NotificationSettings(expiringProducts = false))

        assertEquals(
            NotificationSettings(expiringProducts = false, suggestedRecipes = true, emptyInventory = true),
            settings.observe().first()
        )
    }

    @Test
    fun `los tres avisos se guardan y se releen apagados (CA-PER-04-02)`() = runTest {
        val apagados = NotificationSettings(
            expiringProducts = false,
            suggestedRecipes = false,
            emptyInventory = false
        )

        settings.set(apagados)

        assertEquals(apagados, settings.observe().first())
    }
}
