package com.smartfood.app.core.designsystem

import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.theme.FormMetrics
import com.smartfood.app.core.designsystem.theme.formMetricsFor
import com.smartfood.app.core.designsystem.theme.smaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> FormMetricsTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias de la selección de medidas por alto disponible (spec 002, HU-UI-01).<br>
 * </build>
 */
class FormMetricsTest {

    @Test
    fun `CA-UI-01-03 desde 720 dp de alto util se parte de las medidas de Figma`() {
        assertEquals(FormMetrics.Regular, formMetricsFor(720.dp))
        // V2436: 857 dp de pantalla menos ~57 dp de barras.
        assertEquals(FormMetrics.Regular, formMetricsFor(800.dp))
        assertEquals(FormMetrics.Regular, formMetricsFor(1280.dp))
    }

    @Test
    fun `CA-UI-01-04 entre 620 y 720 dp de alto util se parte de la version intermedia`() {
        assertEquals(FormMetrics.Medium, formMetricsFor(620.dp))
        // Teléfono de 360×720 dp: ~660 dp útiles.
        assertEquals(FormMetrics.Medium, formMetricsFor(660.dp))
        assertEquals(FormMetrics.Medium, formMetricsFor(719.dp))
    }

    @Test
    fun `CA-UI-01-05 por debajo de 620 dp de alto util se parte de la version compacta con la marca en fila`() {
        val metrics = formMetricsFor(600.dp)

        assertEquals(FormMetrics.Compact, metrics)
        assertTrue(metrics.inlineBrand)
        assertFalse(FormMetrics.Medium.inlineBrand)
    }

    @Test
    fun `los perfiles se reducen de forma monotona`() {
        val perfiles = listOf(FormMetrics.Regular, FormMetrics.Medium, FormMetrics.Compact)

        perfiles.zipWithNext { mayor, menor ->
            assertTrue(menor.logoSize <= mayor.logoSize)
            assertTrue(menor.inputHeight <= mayor.inputHeight)
            assertTrue(menor.buttonHeight <= mayor.buttonHeight)
            assertTrue(menor.sectionSpacing <= mayor.sectionSpacing)
            assertTrue(menor.successIconSize <= mayor.successIconSize)
        }
    }

    @Test
    fun `CA-UI-07-03 si una pantalla no cabe se baja de perfil hasta el compacto`() {
        assertEquals(FormMetrics.Medium, FormMetrics.Regular.smaller())
        assertEquals(FormMetrics.Compact, FormMetrics.Medium.smaller())
        assertEquals(null, FormMetrics.Compact.smaller())
    }
}
