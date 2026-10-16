/**
 * Nombre: PhotoSourceSheet.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Hoja "Agregar foto" del editor (spec 006, CA-INV-05-07, T055): elige entre la galería del teléfono y
 * la cámara de la app. El Figma no tiene este frame; sigue el estilo de la hoja de categoría (`476:4113`), con la
 * cámara del Figma y el ícono de imágenes de Material para la galería (spec §8).
 *
 * @param onGallery Abre el selector de fotos del teléfono.
 * @param onCamera Abre el visor de la cámara.
 * @param onDismiss Cierra la hoja sin elegir.
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceSheet(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val metrics = LocalFormMetrics.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = spacing.panelRadius, topEnd = spacing.panelRadius)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = metrics.formHorizontalPadding)
                .padding(bottom = metrics.sectionSpacing)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(id = R.string.editor_photo_source_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            SourceRow(
                icon = rememberVectorPainter(Icons.Outlined.PhotoLibrary),
                text = stringResource(id = R.string.editor_photo_source_gallery),
                onClick = onGallery
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SourceRow(
                icon = painterResource(id = R.drawable.ic_figma_camera),
                text = stringResource(id = R.string.editor_photo_source_camera),
                onClick = onCamera
            )
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(spacing.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.small)
                    .height(metrics.inputHeight)
            ) {
                Text(text = stringResource(id = R.string.editor_sheet_cancel), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Una opción de la hoja: ícono en un cuadro verde claro (como las opciones del Perfil) y su texto. */
@Composable
private fun SourceRow(icon: Painter, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.small)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(IconBox)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(IconRadius))
        ) {
            Icon(painter = icon, contentDescription = null, tint = GreenCheck, modifier = Modifier.size(IconSize))
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = MaterialTheme.spacing.medium)
        )
    }
}

// Mismas medidas que las opciones del Perfil (Figma 306:369): cuadro de 35 px con radio 7 e ícono de 23.
private val IconBox = 35.dp
private val IconRadius = 7.dp
private val IconSize = 23.dp
