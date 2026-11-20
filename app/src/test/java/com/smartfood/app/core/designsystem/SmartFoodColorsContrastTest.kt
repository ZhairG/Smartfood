package com.smartfood.app.core.designsystem

import androidx.compose.ui.graphics.Color
import com.smartfood.app.core.designsystem.theme.Black
import com.smartfood.app.core.designsystem.theme.DarkColorScheme
import com.smartfood.app.core.designsystem.theme.DarkSmartFoodColors
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.GreenDark
import com.smartfood.app.core.designsystem.theme.GreenDark2
import com.smartfood.app.core.designsystem.theme.GreenLight
import com.smartfood.app.core.designsystem.theme.GreenMain
import com.smartfood.app.core.designsystem.theme.GreenMain20
import com.smartfood.app.core.designsystem.theme.GreenSoft
import com.smartfood.app.core.designsystem.theme.GreyDivider
import com.smartfood.app.core.designsystem.theme.LightColorScheme
import com.smartfood.app.core.designsystem.theme.LightSmartFoodColors
import com.smartfood.app.core.designsystem.theme.SurfaceLight
import com.smartfood.app.core.designsystem.theme.White
import com.smartfood.app.core.designsystem.theme.YellowSoft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Nombre: SmartFoodColorsContrastTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Comprueba el contraste WCAG 2.1 de los pares de roles del modo oscuro y que el modo claro conserva sus
 * tokens (spec 013, HU-OSC-01 y HU-OSC-02).
 */
class SmartFoodColorsContrastTest {

    private val dark = DarkSmartFoodColors
    private val light = LightSmartFoodColors
    private val scheme = DarkColorScheme

    @Test
    fun ca_osc_01_01_textosDelModoOscuroLleganA45() {
        val pares = listOf(
            "encabezado" to (dark.header to dark.onHeader),
            "correo y avatar sobre el encabezado" to (dark.header to dark.onHeaderVariant),
            "cuerpo de \"¿Qué cocinamos hoy?\" (ADR-OSC-03)" to (dark.header to GreenSoft),
            "relleno marcado" to (dark.checkedFill to dark.onCheckedFill),
            "resaltado" to (dark.highlight to dark.onHighlight),
            "etiqueta de categoría" to (dark.tag to dark.onTag),
            "chip sin seleccionar y buscador" to (dark.softFill to scheme.onSurface),
            "aviso sin conexión de Guardadas" to (dark.greenSoft to scheme.onSurface),
            "relleno de acción (botón flotante, chip seleccionado)" to (scheme.primary to scheme.onPrimary),
            "contenedor verde (íconos del Perfil, etiquetas)" to (scheme.primaryContainer to scheme.onPrimaryContainer),
            "checklist de contraseña" to (scheme.surfaceVariant to scheme.onSurface),
            "títulos verdes sobre el panel" to (scheme.surface to scheme.primary),
        )

        pares.forEach { (nombre, par) -> assertContraste(nombre, par.first, par.second, minimo = 4.5) }
    }

    @Test
    fun ca_osc_01_02_iconosDelModoOscuroLleganA3() {
        val pares = listOf(
            "ícono verde en su contenedor" to (scheme.primaryContainer to dark.greenIcon),
            "ícono verde sobre verde suave" to (dark.greenSoft to dark.greenIcon),
            "botón flotante sobre el panel" to (scheme.surface to scheme.primary),
            "relleno marcado sobre una hoja" to (scheme.surfaceVariant to dark.checkedFill),
            "texto verde de la pestaña activa (GreenCheck)" to (scheme.surface to GreenCheck),
        )

        pares.forEach { (nombre, par) -> assertContraste(nombre, par.first, par.second, minimo = 3.0) }
    }

    @Test
    fun ca_osc_01_03_elModoClaroConservaSusTokens() {
        assertEquals(GreenDark, light.header)
        assertEquals(White, light.onHeader)
        assertEquals(GreenLight, light.onHeaderVariant)
        assertEquals(GreenCheck, light.checkedFill)
        assertEquals(White, light.onCheckedFill)
        assertEquals(SurfaceLight, light.softFill)
        assertEquals(GreyDivider, light.divider)
        assertEquals(GreenSoft, light.greenSoft)
        assertEquals(YellowSoft, light.highlight)
        assertEquals(GreenDark, light.onHighlight)
        assertEquals(GreenMain20, light.tag)
        assertEquals(GreenCheck, light.onTag)
        assertEquals(GreenCheck, light.greenIcon)
        assertEquals(White.copy(alpha = 0.35f), light.veil)

        // El esquema claro de Material no cambia (CA-UI-05-04 enmendado).
        assertEquals(GreenDark, LightColorScheme.primary)
        assertEquals(White, LightColorScheme.onPrimary)
        assertEquals(GreenLight, LightColorScheme.primaryContainer)
        assertEquals(GreenDark, LightColorScheme.onPrimaryContainer)
        assertEquals(GreenLight, LightColorScheme.surface)
        assertEquals(White, LightColorScheme.surfaceVariant)
    }

    @Test
    fun ca_osc_02_01_elEncabezadoOscuroEsVerdeOscuroConBlanco() {
        assertEquals(GreenDark, dark.header)
        assertEquals(White, dark.onHeader)
    }

    @Test
    fun ca_osc_02_03_losRellenosDeAccionSonBrillantesConContenidoOscuro() {
        assertEquals(GreenMain, scheme.primary)
        assertEquals(GreenDark2, scheme.onPrimary)
        assertEquals(GreenMain, dark.checkedFill)
        assertEquals(GreenDark2, dark.onCheckedFill)
    }

    @Test
    fun ca_osc_03_06_elVeloOscuroEsNegro() {
        assertEquals(Black.copy(alpha = 0.35f), dark.veil)
    }

    private fun assertContraste(nombre: String, fondo: Color, frente: Color, minimo: Double) {
        val contraste = contraste(fondo, frente)
        assertTrue("$nombre: contraste ${"%.2f".format(contraste)}:1, mínimo $minimo:1", contraste >= minimo)
    }

    /** Razón de contraste WCAG 2.1 entre dos colores opacos. */
    private fun contraste(a: Color, b: Color): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Luminancia relativa WCAG 2.1 de un color sRGB. */
    private fun luminancia(color: Color): Double {
        fun canal(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * canal(color.red) + 0.7152 * canal(color.green) + 0.0722 * canal(color.blue)
    }
}
