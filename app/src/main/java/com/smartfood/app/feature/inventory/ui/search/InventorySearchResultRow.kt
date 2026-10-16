/**
 * Nombre: InventorySearchResultRow.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Fila de un resultado de la búsqueda (spec 006, CA-INV-06-01): punto de estado, nombre, fecha de
 * vencimiento y categoría. El estado nunca se comunica solo con color: lleva su nombre visible (RNF-INV-04).
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.ErrorRed
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.OrangeMain
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Tamaño del punto de estado, el mismo del indicador de las secciones de la lista (RNF-INV-04).
 */
private val StatusDotSize = 12.dp

/**
 * Formato de fecha de vencimiento de los resultados: `dd/MM/yyyy` (CA-INV-06-01).
 */
private fun formatExpiryDate(expiresOn: LocalDate): String =
    expiresOn.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

/**
 * Color semántico de cada estado de vencimiento, igual que en la lista.
 */
private val ExpiryStatus.statusColor: Color
    get() = when (this) {
        ExpiryStatus.EXPIRED -> ErrorRed
        ExpiryStatus.EXPIRING_SOON -> OrangeMain
        ExpiryStatus.FRESH -> GreenCheck
    }

/**
 * Nombre visible de cada estado, que evita comunicarlo solo con color (RNF-INV-04).
 */
@get:StringRes
private val ExpiryStatus.statusLabelRes: Int
    get() = when (this) {
        ExpiryStatus.EXPIRED -> R.string.inventory_section_expired
        ExpiryStatus.EXPIRING_SOON -> R.string.inventory_section_expiring_soon
        ExpiryStatus.FRESH -> R.string.inventory_section_fresh
    }

/**
 * Muestra un resultado de la búsqueda y avisa al tocar la fila o su lápiz (CA-INV-06-08).
 *
 * @param result Producto encontrado con su estado de vencimiento.
 * @param onClick Se llama al tocar la fila para abrir "Editar Producto".
 */
@Composable
fun InventorySearchResultRow(
    result: SearchResultUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MaterialTheme.spacing.inputRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            // Punto de estado: decorativo, porque el nombre del estado ya va como texto (RNF-INV-04).
            Column(
                modifier = Modifier
                    .size(StatusDotSize)
                    .background(color = result.status.statusColor, shape = CircleShape)
            ) {}

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result.item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(id = result.status.statusLabelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = result.status.statusColor
                    )
                }
                Text(
                    text = stringResource(
                        id = R.string.inventory_expires_on,
                        formatExpiryDate(result.item.expiresOn)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = result.item.category.titleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.cd_edit_button, result.item.name)
                )
            }
        }
    }
}
