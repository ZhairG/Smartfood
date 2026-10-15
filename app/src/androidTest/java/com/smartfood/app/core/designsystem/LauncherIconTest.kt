package com.smartfood.app.core.designsystem

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfood.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Nombre: LauncherIconTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Verifica la configuración del ícono oficial (spec 002, HU-UI-06): ícono adaptativo con fondo
 * GreenLight, capa frontal generada desde el logo y capa monocromática para íconos temáticos.
 */
class LauncherIconTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun adaptiveIcon(resId: Int): AdaptiveIconDrawable {
        val drawable = ContextCompat.getDrawable(context, resId)
        assertTrue("El ícono debe ser adaptativo y fue $drawable", drawable is AdaptiveIconDrawable)
        return drawable as AdaptiveIconDrawable
    }

    @Test
    fun ca_ui_06_01_iconoAdaptativoConFondoGreenLight() {
        listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round).forEach { resId ->
            val icon = adaptiveIcon(resId)
            val background = icon.background
            assertTrue("El fondo debe ser un color sólido", background is ColorDrawable)
            assertEquals(GREEN_LIGHT, (background as ColorDrawable).color)
            assertNotNull("Debe tener la capa frontal del logo", icon.foreground)
        }
    }

    @Test
    fun ca_ui_06_03_iconoTematicoTieneSilueta() {
        assumeTrue("Los íconos temáticos existen desde Android 13", Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)

        assertNotNull(adaptiveIcon(R.mipmap.ic_launcher).monochrome)
    }

    private companion object {
        /** GreenLight (#F0FDF4), el mismo valor que @color/smartfood_green_light. */
        const val GREEN_LIGHT = 0xFFF0FDF4.toInt()
    }
}
