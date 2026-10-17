/**
 * Nombre: PermissionRationale.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Aviso de permiso denegado que se dibuja encima de una pantalla, con el acceso a los ajustes del
 * teléfono (spec 006, CA-INV-07-06). Lo usa la búsqueda por voz; guarda la forma del aviso de la cámara
 * (`ProductCameraScreen`, CA-INV-05-03).
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing

/**
 * @param icon Ícono que encabeza el aviso.
 * @param title Título del aviso.
 * @param message Explicación de para qué se usa el permiso.
 * @param onOpenSettings Se llama al tocar "Abrir ajustes".
 * @param onDismiss Se llama al cerrar el aviso sin conceder el permiso.
 * @param modifier Modificador opcional.
 */
@Composable
fun PermissionRationale(
    icon: ImageVector,
    title: String,
    message: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrimColor),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large),
            shape = RoundedCornerShape(MaterialTheme.spacing.inputRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(RationaleIconSize)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                SmartFoodPrimaryButton(
                    text = stringResource(id = R.string.search_voice_open_settings),
                    onClick = onOpenSettings
                )
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.cd_navigate_back))
                }
            }
        }
    }
}

/** Velo oscuro que separa el aviso de permiso del contenido de la pantalla. */
private val ScrimColor = Color.Black.copy(alpha = 0.6f)

/** Tamaño del ícono del aviso; guarda la proporción del aviso de la cámara (`476:5959`). */
private val RationaleIconSize = 50.dp

@Preview(showBackground = true)
@Composable
private fun PermissionRationalePreview() {
    SmartfoodTheme {
        PermissionRationale(
            icon = Icons.Outlined.Mic,
            title = "Permiso de micrófono",
            message = "Lo usamos para buscar tus productos por voz.",
            onOpenSettings = {},
            onDismiss = {}
        )
    }
}
