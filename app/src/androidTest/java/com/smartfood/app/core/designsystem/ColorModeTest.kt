package com.smartfood.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.smartfood.app.core.designsystem.components.ActionSuccessScreen
import com.smartfood.app.core.designsystem.theme.GreenLight
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.SurfaceDark
import com.smartfood.app.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> ColorModeTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Verifica que, con el teléfono en modo oscuro, la app se ve clara por defecto (spec 002,<br>
 * HU-UI-05).<br>
 * </build>
 */
class ColorModeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Muestra [content] simulando un teléfono en modo oscuro. */
    private fun conTelefonoOscuro(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.DarkMode(true)) { content() }
        }
    }

    @Test
    fun ca_ui_05_01_temaPorDefectoEsClaroConTelefonoOscuro() {
        var isDark: Boolean? = null
        var surface: Color? = null

        conTelefonoOscuro {
            SmartfoodTheme {
                isDark = LocalIsDarkTheme.current
                surface = MaterialTheme.colorScheme.surface
            }
        }

        assertEquals(false, isDark)
        assertEquals(GreenLight, surface)  // panel claro de Figma (CA-UI-07-02)
    }

    @Test
    fun ca_ui_05_03_modoSistemaSigueAlTelefono() {
        var isDark: Boolean? = null
        var surface: Color? = null

        conTelefonoOscuro {
            SmartfoodTheme(themeMode = ThemeMode.SYSTEM) {
                isDark = LocalIsDarkTheme.current
                surface = MaterialTheme.colorScheme.surface
            }
        }

        assertEquals(true, isDark)
        assertEquals(SurfaceDark, surface)
    }

    @Test
    fun ca_ui_05_01_pantallaDeExitoEsClaraConTelefonoOscuro() {
        conTelefonoOscuro {
            SmartfoodTheme {
                ActionSuccessScreen(
                    title = "¡Cuenta creada con éxito!",
                    body = "Ya puedes acceder a todas nuestras funciones.",
                    buttonText = "Continuar",
                    onButtonClick = {}
                )
            }
        }

        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val fondo = Color(bitmap.getPixel(4, 4))

        assertTrue("El fondo debe ser GreenLight y fue $fondo", fondo.cercanoA(GreenLight))
    }

    private fun Color.cercanoA(otro: Color, tolerancia: Float = 0.02f): Boolean =
        kotlin.math.abs(red - otro.red) <= tolerancia &&
            kotlin.math.abs(green - otro.green) <= tolerancia &&
            kotlin.math.abs(blue - otro.blue) <= tolerancia
}
