package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.ui.graphics.Color
import com.smartfood.app.core.designsystem.theme.ErrorRed
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.YellowExpiring
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nombre: ExpiringSoonColorTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Verifica el color de cada estado del inventario (spec 006, CA-INV-01-01, T027). El amarillo es el
 * token nuevo `YellowExpiring`; Vencido y Fresco siguen usando los tokens que ya existían.
 */
class ExpiringSoonColorTest {

    // CA-INV-01-01: "Vencido" (rojo), "Próximo a vencer" (amarillo) y "Fresco" (verde). Figma `476:3114`.
    @Test
    fun ca_inv_01_01_colorDeCadaEstado() {
        assertEquals(ErrorRed, ExpiryStatus.EXPIRED.color)
        assertEquals(YellowExpiring, ExpiryStatus.EXPIRING_SOON.color)
        assertEquals(GreenCheck, ExpiryStatus.FRESH.color)
    }

    // CA-INV-01-01: el amarillo es el del Figma (nodo `476:3121`), no el naranja provisional del #39.
    @Test
    fun ca_inv_01_01_amarilloDelFigma() {
        assertEquals(Color(0xFFF6E938), YellowExpiring)
    }
}
