package com.smartfood.app.core.designsystem

import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.theme.FormMetrics
import com.smartfood.app.core.designsystem.theme.formMetricsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: FormMetricsTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de la selección de medidas por alto disponible (spec 002, HU-UI-01).
 */
class FormMetricsTest {

    @Test
    fun `CA-UI-01-03 desde 1000 dp de alto util se usan las medidas de Figma`() {
        assertEquals(FormMetrics.Regular, formMetricsFor(1000.dp))
        assertEquals(FormMetrics.Regular, formMetricsFor(1280.dp))
    }

    @Test
    fun `CA-UI-01-04 entre 760 y 1000 dp de alto util se usa la version intermedia`() {
        assertEquals(FormMetrics.Medium, formMetricsFor(760.dp))
        // V2436: 857 dp de pantalla menos ~57 dp de barras.
        assertEquals(FormMetrics.Medium, formMetricsFor(800.dp))
        assertEquals(FormMetrics.Medium, formMetricsFor(999.dp))
    }

    @Test
    fun `CA-UI-01-05 por debajo de 760 dp de alto util se usa la version compacta con la marca en fila`() {
        // Teléfono de 360×720 dp: ~660 dp útiles.
        val metrics = formMetricsFor(660.dp)

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
}
