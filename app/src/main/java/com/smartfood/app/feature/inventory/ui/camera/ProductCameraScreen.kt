package com.smartfood.app.feature.inventory.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing

/**
 * Nombre: ProductCameraScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Visor de la cámara para la foto del producto (spec 006, HU-INV-05, T033), según "Foto_Producto" del
 * Figma (`476:5959`): barra superior, visor redondeado con el recuadro de encuadre y botón circular de captura. Es
 * stateless: la vista previa de CameraX llega por [preview] y el permiso por [permission], así que se prueba sin
 * cámara. Con el permiso negado explica para qué se usa y ofrece abrir los ajustes (CA-INV-05-03).
 */

/** Estado del permiso de cámara. */
enum class CameraPermission {
    /** Concedido: se muestra el visor. */
    GRANTED,

    /** Negado: se muestra la explicación con el acceso a los ajustes (CA-INV-05-03). */
    DENIED,

    /** Todavía no se sabe (se está pidiendo). */
    UNKNOWN,
}

/**
 * @param permission Estado del permiso de cámara.
 * @param preview Vista previa de la cámara (en la app, un `PreviewView` de CameraX).
 * @param isCapturing Si hay una captura en curso: el botón queda deshabilitado.
 * @param captureFailed Si la última captura falló: se muestra el aviso para reintentar.
 * @param onCapture Acción del botón de captura.
 * @param onBack Vuelve al formulario sin foto.
 * @param onOpenSettings Abre los ajustes de la app para conceder el permiso.
 * @param modifier Modificador opcional.
 */
@Composable
fun ProductCameraScreen(
    permission: CameraPermission,
    preview: @Composable () -> Unit,
    isCapturing: Boolean,
    captureFailed: Boolean,
    onCapture: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(title = stringResource(id = R.string.camera_title), onBack = onBack)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(MaterialTheme.spacing.medium)
                .clip(ViewfinderShape)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = ViewfinderEmptyAlpha))
        ) {
            when (permission) {
                CameraPermission.GRANTED -> {
                    preview()
                    ViewfinderFrame(modifier = Modifier.fillMaxSize())
                    CaptureControls(
                        isCapturing = isCapturing,
                        captureFailed = captureFailed,
                        onCapture = onCapture,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
                CameraPermission.DENIED -> CameraPermissionRationale(
                    onOpenSettings = onOpenSettings,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
                CameraPermission.UNKNOWN -> Unit
            }
        }
    }
}

/** Recuadro de encuadre: 4 esquinas centradas en la parte alta del visor, como en Figma. */
@Composable
private fun ViewfinderFrame(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier) {
        val side = size.width * FrameWidthFraction
        val left = (size.width - side) / 2
        val top = size.height * FrameTopFraction
        val corner = FrameCornerLength.toPx()
        val stroke = FrameStroke.toPx()
        val right = left + side
        val bottom = top + side
        fun line(from: Offset, to: Offset) = drawLine(color, from, to, strokeWidth = stroke)
        // Superior izquierda, superior derecha, inferior izquierda e inferior derecha.
        line(Offset(left, top), Offset(left + corner, top)); line(Offset(left, top), Offset(left, top + corner))
        line(Offset(right, top), Offset(right - corner, top)); line(Offset(right, top), Offset(right, top + corner))
        line(Offset(left, bottom), Offset(left + corner, bottom)); line(Offset(left, bottom), Offset(left, bottom - corner))
        line(Offset(right, bottom), Offset(right - corner, bottom)); line(Offset(right, bottom), Offset(right, bottom - corner))
    }
}

@Composable
private fun CaptureControls(
    isCapturing: Boolean,
    captureFailed: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (captureFailed) {
            Text(
                text = stringResource(id = R.string.camera_capture_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(MaterialTheme.spacing.inputRadius))
                    .padding(MaterialTheme.spacing.small)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))
        }
        FilledIconButton(
            onClick = onCapture,
            enabled = !isCapturing,
            modifier = Modifier.size(CaptureButtonSize),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = stringResource(id = R.string.camera_take_photo)
            )
        }
    }
}

/** Explicación cuando se niega el permiso, con el acceso a los ajustes (CA-INV-05-03). */
@Composable
private fun CameraPermissionRationale(onOpenSettings: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        Icon(
            imageVector = Icons.Outlined.NoPhotography,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(CaptureButtonSize)
        )
        Text(
            text = stringResource(id = R.string.camera_permission_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.camera_permission_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        SmartFoodPrimaryButton(text = stringResource(id = R.string.camera_open_settings), onClick = onOpenSettings)
        TextButton(onClick = onBack) { Text(text = stringResource(id = R.string.cd_navigate_back)) }
    }
}

// Medidas del Figma (476:5959). No son de formulario, así que no van en FormMetrics.
private val ViewfinderShape = RoundedCornerShape(27.dp)
private const val ViewfinderEmptyAlpha = 0.42f
private val CaptureButtonSize = 50.dp
private val FrameCornerLength = 58.dp
private val FrameStroke = 1.5.dp
private const val FrameWidthFraction = 0.69f
private const val FrameTopFraction = 0.17f

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductCameraScreenPreview() {
    SmartfoodTheme {
        ProductCameraScreen(
            permission = CameraPermission.GRANTED,
            preview = { Box(Modifier.fillMaxSize().background(Color.DarkGray)) },
            isCapturing = false,
            captureFailed = false,
            onCapture = {},
            onBack = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductCameraScreenDeniedPreview() {
    SmartfoodTheme {
        ProductCameraScreen(
            permission = CameraPermission.DENIED,
            preview = {},
            isCapturing = false,
            captureFailed = false,
            onCapture = {},
            onBack = {},
            onOpenSettings = {}
        )
    }
}
