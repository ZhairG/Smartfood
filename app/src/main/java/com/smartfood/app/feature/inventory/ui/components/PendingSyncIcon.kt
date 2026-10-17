/**
 * Nombre: PendingSyncIcon.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Ícono de "pendiente de sincronizar" de un producto con cambios o una foto que todavía no se suben
 * (spec 006 §13, CA-INV-10-02 y 10-06, T049). El Figma no lo tiene (spec §8): es la nube de Material en gris, del
 * tamaño del lápiz, y TalkBack lo anuncia como "Pendiente de sincronizar".
 *
 * @param modifier Modificador opcional.
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartfood.app.R

@Composable
internal fun PendingSyncIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Outlined.CloudUpload,
        contentDescription = stringResource(id = R.string.cd_pending_sync),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(PendingSyncIconSize)
    )
}

// Del tamaño del lápiz de la fila (EditIconSize en InventoryItemRow).
private val PendingSyncIconSize = 20.dp
