package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Nombre: AnchoredPopup.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Popup flotante anclado a un elemento, que se coloca encima de él (spec 002, HU-UI-03).
 * Se posiciona respecto a los límites reales del ancla, sin desplazamientos fijos (RNF-UI-01).
 */

/**
 * Coloca el popup encima del ancla, centrado horizontalmente. Si no cabe encima (por debajo de [minTopPx]),
 * lo coloca debajo (CA-UI-03-04). En ambos casos lo mantiene dentro de la ventana.
 *
 * @param gapPx Separación entre el ancla y el popup.
 * @param minTopPx Borde superior utilizable de la ventana (por ejemplo, el alto de la barra de estado).
 */
class AbovePopupPositionProvider(
    private val gapPx: Int,
    private val minTopPx: Int
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2).coerceIn(0, maxX)

        val aboveY = anchorBounds.top - gapPx - popupContentSize.height
        val y = if (aboveY >= minTopPx) {
            aboveY
        } else {
            val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
            (anchorBounds.bottom + gapPx).coerceAtMost(maxY)
        }
        return IntOffset(x, y)
    }
}

/**
 * Muestra [popupContent] flotando encima de [content] mientras [visible] sea `true`. El popup tiene el ancho del
 * ancla (como máximo [maxPopupWidth]) y **no** toma el foco, así que el teclado sigue abierto (CA-UI-03-03).
 *
 * @param visible Si es `true`, el popup se muestra.
 * @param popupContent Contenido del popup.
 * @param modifier Modificador del ancla.
 * @param maxPopupWidth Ancho máximo del popup (por ejemplo, los 283 dp del checklist de Figma); sin límite por defecto.
 * @param content Elemento al que se ancla el popup.
 */
@Composable
fun AnchoredPopup(
    visible: Boolean,
    popupContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    maxPopupWidth: Dp = Dp.Infinity,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val positionProvider = remember(density, statusBarTop) {
        AbovePopupPositionProvider(gapPx = with(density) { 8.dp.roundToPx() }, minTopPx = statusBarTop)
    }
    var anchorWidthPx by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.onSizeChanged { anchorWidthPx = it.width }) {
        content()

        if (visible && anchorWidthPx > 0) {
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(focusable = false)
            ) {
                val popupWidth = with(density) { anchorWidthPx.toDp() }.coerceAtMost(maxPopupWidth)
                Box(modifier = Modifier.width(popupWidth)) {
                    popupContent()
                }
            }
        }
    }
}
