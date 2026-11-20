package com.smartfood.app.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.SmartFoodMotion
import com.smartfood.app.core.designsystem.theme.spacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Nombre: BottomSheet.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Hoja inferior de la app (spec 007, CA-ANI-04-01). Reemplaza a `ModalBottomSheet` de Material, cuyo
 * resorte no admite una duración fija: el velo y la hoja entran y salen en 300 ms, ease-in-out, como el prototipo.
 * Se cierra tocando el velo, con "atrás" o arrastrando la manija hacia abajo más de un tercio del alto de la hoja.
 */

/** Ámbito del contenido de la hoja: una columna que además puede cerrar la hoja con animación. */
interface SmartFoodSheetScope : ColumnScope {
    /** Inicia la salida de la hoja y ejecuta [action] cuando termina. Un segundo cierre durante la salida no hace nada. */
    fun close(action: () -> Unit)
}

/**
 * Hoja inferior con velo, manija y entrada y salida de 300 ms.
 *
 * @param onDismissRequest Acción al cerrar con el velo, "atrás" o el arrastre; se llama al terminar la salida.
 * @param containerColor Color de la hoja.
 * @param shape Forma de la hoja; por defecto, esquinas superiores con el radio del panel.
 * @param content Contenido; los botones que cierran la hoja llaman [SmartFoodSheetScope.close].
 */
@Composable
fun SmartFoodBottomSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    shape: Shape = RoundedCornerShape(topStart = MaterialTheme.spacing.panelRadius, topEnd = MaterialTheme.spacing.panelRadius),
    content: @Composable SmartFoodSheetScope.() -> Unit,
) {
    val motion = remember { SheetMotion() }
    val dismiss = { motion.close(onDismissRequest) }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        SheetWindow()
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visibleState = motion.visibleState,
                enter = fadeIn(SmartFoodMotion.standard()),
                exit = fadeOut(SmartFoodMotion.standard())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss)
                )
            }
            AnimatedVisibility(
                visibleState = motion.visibleState,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(SmartFoodMotion.standard()) { it },
                exit = slideOutVertically(SmartFoodMotion.standard()) { it }
            ) {
                SheetBody(motion = motion, containerColor = containerColor, shape = shape, onDragDismiss = dismiss, content = content)
            }
        }
    }

    val action = motion.pendingAction
    if (action != null && motion.visibleState.isIdle && !motion.visibleState.currentState) {
        LaunchedEffect(action) { action() }
    }
}

/** Cuerpo de la hoja: manija que se arrastra y contenido. */
@Composable
private fun SheetBody(
    motion: SheetMotion,
    containerColor: Color,
    shape: Shape,
    onDragDismiss: () -> Unit,
    content: @Composable SmartFoodSheetScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    var heightPx by remember { mutableIntStateOf(0) }
    val dragState = rememberDraggableState { delta ->
        scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, drag.value.roundToInt()) }
            .onSizeChanged { heightPx = it.height }
            .background(containerColor, shape)
            .imePadding()
    ) {
        // Manija (como la de Material): se arrastra hacia abajo para cerrar.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (drag.value > heightPx / DismissFraction) {
                            onDragDismiss()
                        } else {
                            drag.animateTo(0f, SmartFoodMotion.standard())
                        }
                    }
                )
                .padding(vertical = HandleVerticalPadding)
        ) {
            Box(
                modifier = Modifier
                    .size(width = HandleWidth, height = HandleHeight)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HandleAlpha), RoundedCornerShape(50))
            )
        }
        val sheetScope = remember(this, motion) { SheetScopeImpl(this, motion) }
        sheetScope.content()
    }
}

/** La ventana de la hoja no oscurece por sí misma (el velo es de Compose) y sus barras siguen el tema (spec 013). */
@Composable
private fun SheetWindow() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val dark = LocalIsDarkTheme.current
    SideEffect {
        window.setDimAmount(0f)
        window.setWindowAnimations(0)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

/** Estado de entrada y salida de la hoja. */
@Stable
private class SheetMotion {
    val visibleState = MutableTransitionState(false).apply { targetState = true }
    var pendingAction: (() -> Unit)? by mutableStateOf(null)

    fun close(action: () -> Unit) {
        if (pendingAction != null) return
        pendingAction = action
        visibleState.targetState = false
    }
}

private class SheetScopeImpl(column: ColumnScope, private val motion: SheetMotion) :
    SmartFoodSheetScope, ColumnScope by column {
    override fun close(action: () -> Unit) = motion.close(action)
}

/** Opacidad del velo (la de `ModalBottomSheet`). */
private const val ScrimAlpha = 0.32f

/** La hoja se cierra si se arrastra más de un tercio de su alto. */
private const val DismissFraction = 3

private val HandleWidth = 32.dp
private val HandleHeight = 4.dp
private val HandleVerticalPadding = 22.dp
private const val HandleAlpha = 0.4f
