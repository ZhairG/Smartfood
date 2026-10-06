package com.smartfood.app.core.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> CountdownFormatTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 06/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias de [formatCountdown], el formato `m:ss` de la cuenta regresiva (spec 003,<br>
 * HU-REC-04).<br>
 * </build>
 */
class CountdownFormatTest {

    @Test
    fun `CA-REC-04-01 menos de un minuto se muestra como 0 y dos digitos`() {
        assertEquals("0:59", formatCountdown(59))
        assertEquals("0:05", formatCountdown(5))
    }

    @Test
    fun `CA-REC-04-02 la espera de la ventana de una hora se muestra en minutos y segundos`() {
        assertEquals("37:12", formatCountdown(2_232))
        assertEquals("1:00", formatCountdown(60))
    }

    @Test
    fun `valores negativos se muestran como 0 00`() {
        assertEquals("0:00", formatCountdown(-3))
    }
}
