/**
 * Nombre: PhotoSlot.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Zona de foto del editor de producto (spec 006, HU-INV-05, Figma `476:3672`): recuadro punteado con la
 * foto, la papelera arriba a la izquierda y la cámara abajo a la derecha.
 *
 * @param photo Foto que se muestra.
 * @param onCameraClick Abre el visor de la cámara (CA-INV-05-01).
 * @param onRemoveClick Pide quitar la foto (CA-INV-05-04); la papelera solo aparece si hay foto.
 * @param modifier Modificador opcional.
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreyDark
import com.smartfood.app.feature.inventory.ui.editor.PhotoUiState
import java.io.File

@Composable
fun PhotoSlot(
    photo: PhotoUiState,
    onCameraClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SlotRadius)
    val borderColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(SlotAspectRatio)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawBehind {
                val stroke = SlotBorder.toPx()
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(SlotRadius.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx()))
                    )
                )
            }
    ) {
        val model: Any? = when (photo) {
            is PhotoUiState.Local -> File(photo.path)
            is PhotoUiState.Remote -> photo.url
            PhotoUiState.None -> null
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = stringResource(id = R.string.cd_editor_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onRemoveClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = GreyDark),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(ButtonInset)
                    .size(SlotButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.cd_editor_remove_photo),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        IconButton(
            onClick = onCameraClick,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(ButtonInset)
                .size(SlotButtonSize)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = stringResource(id = R.string.cd_editor_take_photo),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// Medidas del Figma (476:3672: 366 × 190, botones de 50 a 10 dp del borde). No son de formulario, así que no van en
// FormMetrics; el recuadro conserva la proporción y se adapta al ancho.
private const val SlotAspectRatio = 366f / 190f
private val SlotRadius = 20.dp
private val SlotBorder = 1.dp
private val DashOn = 6.dp
private val DashOff = 4.dp
private val SlotButtonSize = 50.dp
private val ButtonInset = 10.dp
