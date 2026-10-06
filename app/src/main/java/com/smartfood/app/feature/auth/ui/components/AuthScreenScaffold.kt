package com.smartfood.app.feature.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.SmartFoodBrandHeader
import com.smartfood.app.core.designsystem.components.SmartFoodOfflineDialog
import com.smartfood.app.core.designsystem.theme.FormMetrics
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.formMetricsForWindow
import com.smartfood.app.core.designsystem.theme.smaller
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.smartfood.app.core.designsystem.util.ClearFocusOnKeyboardDismiss
import com.smartfood.app.core.designsystem.util.SystemBarEffect
import com.smartfood.app.core.designsystem.util.clearFocusOnTap
import com.smartfood.app.core.designsystem.util.panelTopCorners
import kotlin.math.roundToInt

/**
 * <build>
 * <b>Nombre:</b> AuthScreenScaffold.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 28/09/2026<br>
 * <b>Descripción:</b> Contenedor común de las pantallas de autenticación (spec 002, plan §3).<br>
 * - Elige las medidas según el alto útil ([formMetricsForWindow]) para que el formulario quepa sin scroll.<br>
 * - Encoge el encabezado de marca al mismo ritmo que sube el teclado (CA-UI-02-05).<br>
 * - Reduce el panel con el inset del teclado; el sistema de foco de Compose mantiene visible el campo enfocado, sin<br>
 *   scroll programático (CA-UI-02-04, RNF-UI-02).<br>
 * - Quita el foco al tocar fuera de los campos o al cerrar el teclado (CA-UI-02-06 y CA-UI-02-07).<br>
 * - Si sobra alto, las [FlexibleGap] del formulario lo reparten como en Figma; si falta, se encogen a su mínimo<br>
 *   (CA-UI-07-03).<br>
 * </build>
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
        // Perfil más grande que le cabe a ESTA pantalla (CA-UI-07-03): se parte del perfil que sugiere el alto útil
        // y, si con el teclado cerrado el formulario se desborda (p. ej. al aparecer un error), se baja al siguiente.
        // Solo baja, nunca sube, así no hay oscilaciones; con el teclado abierto no se evalúa. El desborde debe
        // mantenerse OVERFLOW_SETTLE_MS: los pasajeros no cuentan (p. ej. el fundido entre pasos de la recuperación,
        // en el que el paso saliente conserva su alto un instante y llevaba el perfil al compacto sin motivo).
        val initialMetrics = formMetricsForWindow(maxHeight)
        var metrics by remember(initialMetrics) { mutableStateOf(initialMetrics) }
        val scrollState = rememberScrollState()
        val imeInsets = WindowInsets.ime
        val density = LocalDensity.current
        LaunchedEffect(metrics) {
            // Antes del primer layout maxValue vale Int.MAX_VALUE y viewportSize 0: solo cuenta un desborde medido.
            snapshotFlow {
                scrollState.viewportSize > 0 && scrollState.maxValue > 0 && imeInsets.getBottom(density) == 0
            }.collectLatest { overflows ->
                if (overflows) {
                    // Si el desborde desaparece antes, collectLatest cancela la espera y no se baja de perfil.
                    delay(OVERFLOW_SETTLE_MS)
                    metrics.smaller()?.let { metrics = it }
                }
            }
        }

        CompositionLocalProvider(LocalFormMetrics provides metrics) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Figma (CA-UI-07-06): con el diálogo sin conexión, el fondo se desenfoca. En Android 11 o
                    // anterior blur no tiene efecto y queda la atenuación propia del Dialog.
                    .blur(if (isOffline) OFFLINE_BLUR else 0.dp)
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
                    scrollState = scrollState,
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
    // Como en Figma, el indicador de pasos ocupa el margen superior: el logo queda a la misma altura que en el Login.
    val paddingTop = if (hasTopHeader) 0.dp else metrics.headerPaddingTop

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
    scrollState: ScrollState,
    headerContent: @Composable ColumnScope.() -> Unit,
    formContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val viewport = remember { FormViewport() }

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
                .verticalScroll(scrollState)
                .padding(horizontal = metrics.formHorizontalPadding)
                .padding(bottom = metrics.panelPaddingBottom)
                // El scroll conserva el alto mínimo del viewport: el formulario lo llena y sus FlexibleGap crecen.
                .layout { measurable, constraints ->
                    viewport.minHeightPx = constraints.minHeight
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
        ) {
            CompositionLocalProvider(LocalFormViewport provides viewport) {
                formContent()
            }
        }
    }
}

/**
 * Alto visible del formulario (el viewport del scroll sin el margen inferior), medido en la fase de layout.
 * Lo usan los contenedores que no son hijos directos del formulario, como un `AnimatedContent`, para que sus
 * [FlexibleGap] también repartan el alto sobrante ([fillFormViewport]).
 */
@Stable
class FormViewport internal constructor() {
    internal var minHeightPx by mutableIntStateOf(0)
}

/** Viewport del formulario vigente; lo provee el panel de [AuthScreenScaffold]. */
val LocalFormViewport = staticCompositionLocalOf { FormViewport() }

/**
 * Hace que el elemento mida al menos el alto visible del formulario. Sirve para un único hijo que envuelve todo el
 * formulario (p. ej. el `AnimatedContent` de los pasos de recuperación). Si el contenido es más alto, crece y el
 * formulario hace scroll como siempre.
 *
 * @param viewport Normalmente `LocalFormViewport.current`.
 */
fun Modifier.fillFormViewport(viewport: FormViewport): Modifier = layout { measurable, constraints ->
    val minHeight = viewport.minHeightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(minHeight = minHeight))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/**
 * Separación del formulario con un mínimo fijo que crece con el alto sobrante, en proporción a [weight]. Así, en
 * pantallas altas los bloques se reparten como en Figma y en pantallas bajas (o con el teclado abierto) quedan a
 * [minHeight] sin desbordar (CA-UI-07-03).
 *
 * @param minHeight Separación mínima, normalmente una medida de [LocalFormMetrics].
 * @param weight Parte del alto sobrante que toma esta separación frente a las demás.
 */
@Composable
fun ColumnScope.FlexibleGap(minHeight: Dp, weight: Float = 1f) {
    Spacer(Modifier.height(minHeight))
    Spacer(Modifier.weight(weight))
}

/** Tiempo que debe durar un desborde para bajar de perfil; cubre el fundido entre pasos de la recuperación. */
private const val OVERFLOW_SETTLE_MS = 400L

/** Desenfoque del fondo mientras se muestra el diálogo sin conexión. */
private val OFFLINE_BLUR = 8.dp
