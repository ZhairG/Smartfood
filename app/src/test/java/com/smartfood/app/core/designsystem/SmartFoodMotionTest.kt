package com.smartfood.app.core.designsystem

import com.smartfood.app.core.designsystem.theme.SmartFoodMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre: SmartFoodMotionTest.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Pruebas de los tokens de movimiento (spec 007, RF-ANI-01): duraciones del Figma y curvas que van de 0 a
 * 1; las del Figma son monótonas y la de "back" se pasa del destino y regresa.
 */
class SmartFoodMotionTest {

    @Test
    fun ca_ani_01_01_lasDuracionesSonLasDelFigma() {
        assertEquals(100, SmartFoodMotion.SHORT)
        assertEquals(150, SmartFoodMotion.FAST)
        assertEquals(300, SmartFoodMotion.MEDIUM)
    }

    @Test
    fun ca_ani_01_01_lasCurvasVanDeCeroAUnoYSonMonotonas() {
        listOf(SmartFoodMotion.EaseInOut, SmartFoodMotion.EaseOut, SmartFoodMotion.EaseIn).forEach { easing ->
            assertEquals(0f, easing.transform(0f), 0.001f)
            assertEquals(1f, easing.transform(1f), 0.001f)
            var anterior = 0f
            for (i in 1..100) {
                val valor = easing.transform(i / 100f)
                assertTrue("La curva retrocede en ${i / 100f}", valor >= anterior - 0.0001f)
                anterior = valor
            }
        }
    }

    @Test
    fun ca_ani_04_02_laCurvaBackSePasaDelDestinoYRegresa() {
        val back = SmartFoodMotion.EaseInOutBack
        assertEquals(0f, back.transform(0f), 0.001f)
        assertEquals(1f, back.transform(1f), 0.001f)
        val valores = (0..100).map { back.transform(it / 100f) }
        assertTrue("Debe bajar de 0 al empezar", valores.minOrNull()!! < 0f)
        assertTrue("Debe pasar de 1 antes de terminar", valores.maxOrNull()!! > 1f)
    }
}
