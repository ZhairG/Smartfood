package com.smartfood.app.feature.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.SmartFoodBrandHeader
import com.smartfood.app.core.designsystem.components.SmartFoodOfflineDialog
import com.smartfood.app.core.designsystem.theme.FormMetrics
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.formMetricsForWindow
import com.smartfood.app.core.designsystem.util.ClearFocusOnKeyboardDismiss
import com.smartfood.app.core.designsystem.util.SystemBarEffect
import com.smartfood.app.core.designsystem.util.clearFocusOnTap
import com.smartfood.app.core.designsystem.util.panelTopCorners
import kotlin.math.roundToInt

/**
 * Nombre: AuthScreenScaffold.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contenedor común de las pantallas de autenticación (spec 002, plan §3).
 * - Elige las medidas según el alto útil ([formMetricsForWindow]) para que el formulario quepa sin scroll.
 * - Encoge el encabezado de marca al mismo ritmo que sube el teclado (CA-UI-02-05).
 * - Reduce el panel con el inset del teclado; el sistema de foco de Compose mantiene visible el campo
 *   enfocado, sin scroll programático (CA-UI-02-04, RNF-UI-02).
 * - Quita el foco al tocar fuera de los campos o al cerrar el teclado (CA-UI-02-06 y CA-UI-02-07).
 *
 * @param headerContent Título y subtítulo, en la parte superior del panel.
 * @param modifier Modificador del contenedor.
 * @param isOffline Muestra el diálogo de desconexión (HU-06 de la spec 001).
 * @param isReconnecting Indica si hay un reintento en curso dentro del diálogo.
 * @param onRetryConnection Acción del botón "Reconectar".
 * @param topHeaderContent Contenido opcional sobre la marca (ej. indicador de pasos); nunca se colapsa.
 * @param formContent Campos y acciones del formulario.
 */
@Composable
fun AuthScreenScaffold(
    headerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false,
    isReconnecting: Boolean = false,
    onRetryConnection: () -> Unit = {},
    topHeaderContent: (@Composable ColumnScope.() -> Unit)? = null,
    formContent: @Composable ColumnScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current

    SystemBarEffect(isLightBackground = false)
    ClearFocusOnKeyboardDismiss()

    if (isOffline) {
        SmartFoodOfflineDialog(isReconnecting = isReconnecting, onReconnect = onRetryConnection)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val metrics = formMetricsForWindow(maxHeight)

        CompositionLocalProvider(LocalFormMetrics provides metrics) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clearFocusOnTap(focusManager)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                if (topHeaderContent != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = topHeaderContent
                    )
                }

                CollapsibleBrandHeader(
                    metrics = metrics,
                    hasTopHeader = topHeaderContent != null
                )

                AuthFormPanel(
                    metrics = metrics,
                    headerContent = headerContent,
                    formContent = formContent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Encabezado de marca que se encoge a medida que sube el teclado. El alto se calcula en la fase de layout a partir
 * del inset del teclado, que ya viene animado por el sistema: no hay animaciones propias ni recomposiciones por
 * frame (ADR-UI-01).
 */
@Composable
private fun CollapsibleBrandHeader(metrics: FormMetrics, hasTopHeader: Boolean) {
    val imeInsets = WindowInsets.ime
    val paddingTop = if (hasTopHeader) metrics.headerPaddingTop / 2 else metrics.headerPaddingTop

    SmartFoodBrandHeader(
        smartColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val fullHeight = placeable.height
                val imeBottom = imeInsets.getBottom(this)
                val collapsed = if (fullHeight == 0) 1f else (imeBottom.toFloat() / fullHeight).coerceIn(0f, 1f)
                val visibleHeight = (fullHeight * (1f - collapsed)).roundToInt()
                layout(placeable.width, visibleHeight) {
                    placeable.placeWithLayer(0, visibleHeight - fullHeight) { alpha = 1f - collapsed }
                }
            }
            .padding(top = paddingTop, bottom = metrics.headerPaddingBottom)
            .wrapContentWidth(Alignment.CenterHorizontally)
    )
}

/**
 * Panel redondeado del formulario. El padding inferior es el mayor entre la barra de navegación y el teclado, así
 * que el área de scroll se reduce mientras sube el teclado y el campo enfocado sigue visible (ADR-UI-02).
 */
@Composable
private fun AuthFormPanel(
    metrics: FormMetrics,
    headerContent: @Composable ColumnScope.() -> Unit,
    formContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panelTopCorners(MaterialTheme.colorScheme.surface, 45.dp)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom))
            .padding(top = metrics.panelPaddingTop),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = metrics.formHorizontalPadding),
            content = headerContent
        )

        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = metrics.formHorizontalPadding)
                .padding(bottom = metrics.panelPaddingBottom),
            content = formContent
        )
    }
}
