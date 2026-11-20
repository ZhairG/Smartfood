package com.smartfood.app.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.ActionSuccessScreen
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.GreenDark
import com.smartfood.app.core.designsystem.theme.GreenLight
import com.smartfood.app.core.designsystem.theme.GreenMain
import com.smartfood.app.core.designsystem.theme.InputDark
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.SurfaceDark
import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.feature.recipes.ui.components.RecipeFilterPill
import com.smartfood.app.feature.recipes.ui.components.RecipeSearchField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Nombre: ColorModeTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Verifica que, con el teléfono en modo oscuro, la app se ve clara por defecto (spec 002, HU-UI-05), y
 * que con el tema oscuro el encabezado, el buscador y el chip seleccionado usan los colores legibles (spec 013).
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

    @Test
    fun ca_osc_02_01_encabezadoOscuroEsVerdeOscuro() {
        composeTestRule.setContent {
            SmartfoodTheme(themeMode = ThemeMode.DARK) {
                SmartFoodTopBar(title = "Editar Producto", onBack = {}, modifier = Modifier.testTag(MUESTRA))
            }
        }

        val fondo = pixel(x = 4.dp)

        assertTrue("El encabezado debe ser GreenDark y fue $fondo", fondo.cercanoA(GreenDark))
    }

    @Test
    fun ca_osc_02_03_chipSeleccionadoOscuroEsVerdeBrillante() {
        composeTestRule.setContent {
            SmartfoodTheme(themeMode = ThemeMode.DARK) {
                // Entre el borde de 1 dp y el texto, que empieza a 12 dp.
                Box(modifier = Modifier.testTag(MUESTRA)) { RecipeFilterPill(text = "Para ti", selected = true, onClick = {}) }
            }
        }

        val fondo = pixel(x = 6.dp)

        assertTrue("El chip seleccionado debe ser GreenMain y fue $fondo", fondo.cercanoA(GreenMain))
    }

    @Test
    fun ca_osc_03_01_buscadorOscuroEsInputDark() {
        composeTestRule.setContent {
            SmartfoodTheme(themeMode = ThemeMode.DARK) {
                // La lupa empieza a 16 dp; a 8 dp solo está el relleno.
                RecipeSearchField(
                    query = "",
                    placeholder = "Buscar receta o ingrediente...",
                    onQueryChanged = {},
                    onClearQuery = {},
                    modifier = Modifier.testTag(MUESTRA)
                )
            }
        }

        val fondo = pixel(x = 8.dp)

        assertTrue("El buscador debe ser InputDark y fue $fondo", fondo.cercanoA(InputDark))
    }

    /** Color del píxel a [x] del borde izquierdo y a media altura del nodo [MUESTRA]. */
    private fun pixel(x: Dp): Color {
        val bitmap = composeTestRule.onNodeWithTag(MUESTRA).captureToImage().asAndroidBitmap()
        val px = with(composeTestRule.density) { x.roundToPx() }
        return Color(bitmap.getPixel(px, bitmap.height / 2))
    }

    private fun Color.cercanoA(otro: Color, tolerancia: Float = 0.02f): Boolean =
        kotlin.math.abs(red - otro.red) <= tolerancia &&
            kotlin.math.abs(green - otro.green) <= tolerancia &&
            kotlin.math.abs(blue - otro.blue) <= tolerancia

    private companion object {
        const val MUESTRA = "muestra"
    }
}
