package com.smartfood.app.core.designsystem

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.smartfood.app.core.designsystem.components.AbovePopupPositionProvider
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nombre: AbovePopupPositionProviderTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias del posicionamiento del popup anclado (spec 002, HU-UI-03, RNF-UI-01).
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
    fun `CA-UI-03-04 sin espacio arriba el popup se muestra debajo`() {
        val anchor = IntRect(left = 60, top = 200, right = 660, bottom = 300)

        val offset = posicion(anchor, IntSize(600, 300))

        assertEquals(300 + 16, offset.y)
    }

    @Test
    fun `CA-UI-03-04 el popup no invade la barra de estado`() {
        val anchor = IntRect(left = 60, top = 390, right = 660, bottom = 490)

        // Arriba quedaría en y = 74, por encima del límite de 80: debe ir debajo.
        val offset = posicion(anchor, IntSize(600, 300))

        assertEquals(490 + 16, offset.y)
    }

    @Test
    fun `CA-UI-03-04 debajo nunca se sale de la ventana`() {
        val anchor = IntRect(left = 60, top = 100, right = 660, bottom = 1500)

        val offset = posicion(anchor, IntSize(600, 300))

        assertEquals(window.height - 300, offset.y)
    }

    @Test
    fun `el popup se centra sobre el campo y se ajusta a los bordes laterales`() {
        val centrado = posicion(IntRect(100, 900, 500, 1000), IntSize(200, 100))
        assertEquals(200, centrado.x)

        val pegadoALaDerecha = posicion(IntRect(600, 900, 720, 1000), IntSize(400, 100))
        assertEquals(window.width - 400, pegadoALaDerecha.x)
    }
}
