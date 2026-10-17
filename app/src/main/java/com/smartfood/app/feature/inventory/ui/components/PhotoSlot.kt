/**
 * Nombre: PhotoSlot.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Zona de foto del editor de producto (spec 006, HU-INV-05, Figma `476:3672`): recuadro blanco con borde
 * punteado gris, la papelera arriba a la izquierda y la cámara abajo a la derecha. La papelera se ve siempre, como en
 * el Figma: gris y deshabilitada sin foto, y roja cuando hay una foto que borrar.
 *
 * @param photo Foto que se muestra.
 * @param onAddPhotoClick Abre la hoja "Agregar foto" con la galería y la cámara (CA-INV-05-07). La llaman todo el
 * recuadro y su botón de cámara.
 * @param onRemoveClick Pide quitar la foto (CA-INV-05-04).
 * @param modifier Modificador opcional.
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreyDark
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.inventory.ui.editor.PhotoUiState
import java.io.File

@Composable
fun PhotoSlot(
    photo: PhotoUiState,
    onAddPhotoClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SlotRadius)
    val addPhotoLabel = stringResource(id = R.string.cd_editor_add_photo)
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val model: Any? = when (photo) {
        is PhotoUiState.Local -> File(photo.path)
        // La foto que espera subirse se ve desde el teléfono (CA-INV-10-04).
        is PhotoUiState.Pending -> File(photo.path)
        is PhotoUiState.Remote -> photo.url
        PhotoUiState.None -> null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(SlotAspectRatio)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // Todo el recuadro abre las opciones de la foto (CA-INV-05-07).
            .clickable(onClickLabel = addPhotoLabel, role = Role.Button, onClick = onAddPhotoClick)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(SlotRadius.toPx()),
                    style = Stroke(
                        width = SlotBorder.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx()))
                    )
                )
            }
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = stringResource(id = R.string.cd_editor_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        SlotButton(
            icon = R.drawable.ic_figma_delete,
            contentDescription = stringResource(id = R.string.cd_editor_remove_photo),
            // Figma: gris sin foto (deshabilitada) y roja cuando hay una foto que borrar.
            containerColor = MaterialTheme.colorScheme.error,
            disabledContainerColor = GreyDark,
            iconSize = TrashIconSize,
            onClick = onRemoveClick,
            enabled = model != null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = TrashInsetStart, top = TrashInsetTop)
        )
        SlotButton(
            icon = R.drawable.ic_figma_camera,
            contentDescription = stringResource(id = R.string.cd_editor_take_photo),
            containerColor = MaterialTheme.colorScheme.primary,
            iconSize = CameraIconSize,
            onClick = onAddPhotoClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = CameraInsetEnd, bottom = CameraInsetBottom)
        )
    }
}

/** Botón redondo con sombra de la zona de foto (Figma: círculo de 50 px con sombra de 4 px). */
@Composable
private fun SlotButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    containerColor: Color,
    iconSize: Dp,
    disabledContainerColor: Color = containerColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .size(SlotButtonSize)
            .shadow(elevation = ButtonShadow, shape = CircleShape)
    ) {
        Icon(painter = painterResource(id = icon), contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
}

// Medidas del Figma (476:3672: 366 × 190, radio 27, borde punteado de 1 px; papelera de 50 px a 10 y 8 px del borde
// con ícono de 32; cámara de 50 px a 15 y 13 px del borde con ícono de 25). No son de formulario, así que no van en
// FormMetrics; el recuadro conserva la proporción y se adapta al ancho.
private const val SlotAspectRatio = 366f / 190f
private val SlotRadius = 27.dp
private val SlotBorder = 1.dp
private val DashOn = 6.dp
private val DashOff = 4.dp
private val SlotButtonSize = 50.dp
private val TrashInsetStart = 10.dp
private val TrashInsetTop = 8.dp
private val TrashIconSize = 32.dp
private val CameraInsetEnd = 15.dp
private val CameraInsetBottom = 13.dp
private val CameraIconSize = 25.dp
private val ButtonShadow = 4.dp

@Preview(showBackground = true)
@Composable
private fun PhotoSlotEmptyPreview() {
    SmartfoodTheme { PhotoSlot(photo = PhotoUiState.None, onAddPhotoClick = {}, onRemoveClick = {}) }
}
