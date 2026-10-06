package com.smartfood.app.core.designsystem

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.smartfood.app.core.designsystem.components.AbovePopupPositionProvider
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * <build>
 * <b>Nombre:</b> AbovePopupPositionProviderTest.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Pruebas unitarias del posicionamiento del popup anclado (spec 002, HU-UI-03, RNF-UI-01).<br>
 * </build>
 */
class AbovePopupPositionProviderTest {

    private val window = IntSize(720, 1600)
    private val provider = AbovePopupPositionProvider(gapPx = 16, minTopPx = 80)

    private fun posicion(anchor: IntRect, popup: IntSize): IntOffset =
        provider.calculatePosition(anchor, window, LayoutDirection.Ltr, popup)

    @Test
    fun `CA-UI-03-01 con espacio arriba el popup queda encima del campo sin taparlo`() {
        val anchor = IntRect(left = 60, top = 900, right = 660, bottom = 1000)
        val popup = IntSize(600, 300)

        val offset = posicion(anchor, popup)

        assertEquals(IntOffset(60, 900 - 16 - 300), offset)
        assert(offset.y + popup.height <= anchor.top) { "El popup no debe tapar el campo" }
    }

    @Test
    fun `CA-UI-03-04 sin espacio suficiente sigue encima ajustado al borde superior`() {
        val anchor = IntRect(left = 60, top = 390, right = 660, bottom = 490)

        // Encima quedaría en y = 74, por encima del límite de 80: se ajusta a 80 y no pasa debajo del campo.
        val offset = posicion(anchor, IntSize(600, 300))

        assertEquals(80, offset.y)
        assert(offset.y < anchor.bottom) { "El popup nunca debe quedar debajo del campo" }
    }

    @Test
    fun `CA-UI-03-04 el popup no invade la barra de estado aunque el campo este arriba del todo`() {
        val anchor = IntRect(left = 60, top = 100, right = 660, bottom = 200)

        val offset = posicion(anchor, IntSize(600, 300))

        assertEquals(80, offset.y)
    }

    @Test
    fun `CA-UI-03-04 el popup nunca se sale de la ventana`() {
        val offset = posicion(IntRect(left = 60, top = 100, right = 660, bottom = 1500), IntSize(600, 1700))

        assertEquals(0, offset.y)
    }

    @Test
    fun `el popup se centra sobre el campo y se ajusta a los bordes laterales`() {
        val centrado = posicion(IntRect(100, 900, 500, 1000), IntSize(200, 100))
        assertEquals(200, centrado.x)

        val pegadoALaDerecha = posicion(IntRect(600, 900, 720, 1000), IntSize(400, 100))
        assertEquals(window.width - 400, pegadoALaDerecha.x)
    }
}
