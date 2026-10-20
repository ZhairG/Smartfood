package com.smartfood.app.core.preferences

import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.testutil.FakeThemePreferences
import com.smartfood.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: ThemeViewModelTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de [ThemeViewModel] (spec 008, HU-PER-06, T018): arranca en el tema por defecto y
 * refleja el modo guardado en el teléfono en cuanto cambia (CA-PER-06-03 y 06-04).
 */
class ThemeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeThemePreferences()

    @Test
    fun `CA-PER-06-03 arranca en el tema por defecto`() {
        assertEquals(ThemeMode.Default, ThemeViewModel(preferences).themeMode.value)
    }

    @Test
    fun `CA-PER-06-04 refleja el tema guardado`() = runTest {
        val viewModel = ThemeViewModel(preferences)

        preferences.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }
}
