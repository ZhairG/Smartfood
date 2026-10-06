package com.smartfood.app.core.designsystem

import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.core.designsystem.theme.resolveIsDark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> ThemeModeTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias de la resolución del modo de color (spec 002, HU-UI-05).<br>
 * </build>
 */
class ThemeModeTest {

    @Test
    fun `CA-UI-05-03 el modo por defecto es claro`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.Default)
    }

    @Test
    fun `CA-UI-05-01 el modo claro ignora el modo oscuro del sistema`() {
        assertFalse(ThemeMode.LIGHT.resolveIsDark(systemIsDark = true))
        assertFalse(ThemeMode.LIGHT.resolveIsDark(systemIsDark = false))
    }

    @Test
    fun `el modo oscuro siempre es oscuro`() {
        assertTrue(ThemeMode.DARK.resolveIsDark(systemIsDark = false))
        assertTrue(ThemeMode.DARK.resolveIsDark(systemIsDark = true))
    }

    @Test
    fun `CA-UI-05-03 el modo sistema sigue al sistema`() {
        assertTrue(ThemeMode.SYSTEM.resolveIsDark(systemIsDark = true))
        assertFalse(ThemeMode.SYSTEM.resolveIsDark(systemIsDark = false))
    }
}
