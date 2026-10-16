/**
 * Nombre: SearchResultRow.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Fila de un resultado de la búsqueda (spec 006, CA-INV-06-01, Figma `476:3190`): punto del color del
 * estado, nombre, "Vence: dd/mm/aaaa" y la etiqueta de categoría, con un separador. A diferencia de la lista, no
 * lleva miniatura ni lápiz: toda la fila abre el editor (CA-INV-06-08).
 *
 * @param result Producto encontrado con su estado.
 * @param onClick Abre "Editar Producto" del producto.
 * @param modifier Modificador opcional.
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.ui.components.CategoryLabel
import com.smartfood.app.feature.inventory.ui.components.color
import com.smartfood.app.feature.inventory.ui.components.titleRes
import java.time.format.DateTimeFormatter

@Composable
internal fun SearchResultRow(
    result: SearchResultUi,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = result.item
    val statusLabel = stringResource(id = result.status.titleRes)
    val spacing = MaterialTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onClick(item.id) }
                .padding(vertical = spacing.medium)
        ) {
            // El punto no es la única señal del estado: se anuncia con su nombre (RNF-INV-04).
            Box(
                modifier = Modifier
                    .size(StatusDotSize)
                    .background(color = result.status.color, shape = CircleShape)
                    .semantics { contentDescription = statusLabel }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(id = R.string.inventory_expires_on, item.expiresOn.format(DateFormatter)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CategoryLabel(item = item)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Medida del Figma (476:3190): punto de estado de 12 px. No es de formulario, así que no va en FormMetrics.
private val StatusDotSize = 12.dp
