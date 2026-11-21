package com.smartfood.app.core.backend

import com.smartfood.app.core.backend.data.RemoteConfigBackendModeProvider
import com.smartfood.app.testutil.FakeBackendModeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Nombre: BackendModeTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas del interruptor del backend (spec 012, HU-SPK-01).
 */
class BackendModeTest {

    @Test
    fun `CA-SPK-01-04 backendMode blaze o free fuerza el modo y otro valor no`() {
        assertEquals(BackendMode.BLAZE, RemoteConfigBackendModeProvider.forcedMode("blaze"))
        assertEquals(BackendMode.FREE, RemoteConfigBackendModeProvider.forcedMode(" FREE "))
        assertNull(RemoteConfigBackendModeProvider.forcedMode(""))
        assertNull(RemoteConfigBackendModeProvider.forcedMode("spark"))
    }

    @Test
    fun `CA-SPK-01-01 el caso de uso devuelve el modo del interruptor`() {
        val provider = FakeBackendModeProvider()
        val useCase = GetBackendModeUseCase(provider)
        assertEquals(BackendMode.FREE, useCase())
        provider.mode = BackendMode.BLAZE
        assertEquals(BackendMode.BLAZE, useCase())
    }
}
