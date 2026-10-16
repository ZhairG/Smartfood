/**
 * Nombre: InventoryItemRow.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Fila de un producto de la lista: miniatura (foto o bloque del color del estado), fecha de vencimiento,
 * nombre, etiqueta de la categoría con la cantidad opcional y lápiz de edición (spec 006, CA-INV-01-02, CA-INV-01-03
 * y CA-INV-01-08). Medidas del frame "Inventario" de Figma (`476:3116`).
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.GreenMain20
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.model.ExpiryStatus
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import com.smartfood.app.feature.inventory.domain.model.Quantity
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Texto de cada [FoodCategory] (CA-INV-01-02).
 */
val FoodCategory.titleRes: Int
    get() = when (this) {
        FoodCategory.FRUITS -> R.string.category_fruits
        FoodCategory.DAIRY -> R.string.category_dairy
        FoodCategory.PROTEINS -> R.string.category_proteins
        FoodCategory.VEGETABLES -> R.string.category_vegetables
        FoodCategory.GRAINS -> R.string.category_grains
        FoodCategory.OTHER -> R.string.category_other
    }

/**
 * Texto de cada [QuantityUnit] (CA-INV-01-08).
 */
val QuantityUnit.titleRes: Int
    get() = when (this) {
        QuantityUnit.UNITS -> R.string.unit_units
        QuantityUnit.GRAMS -> R.string.unit_grams
        QuantityUnit.KILOGRAMS -> R.string.unit_kilograms
        QuantityUnit.POUNDS -> R.string.unit_pounds
        QuantityUnit.MILLILITERS -> R.string.unit_milliliters
        QuantityUnit.LITERS -> R.string.unit_liters
    }

/**
 * @param item Producto que muestra la fila.
 * @param status Estado de vencimiento de la sección a la que pertenece.
 * @param onEditClicked Acción al tocar la fila o su lápiz, con el identificador del producto.
 * @param modifier Modificador opcional.
 */
@Composable
fun InventoryItemRow(
    item: InventoryItem,
    status: ExpiryStatus,
    onEditClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .clickable { onEditClicked(item.id) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemThumbnail(item = item, status = status)
            Spacer(modifier = Modifier.width(ThumbnailToTextGap))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.expiresOn.format(DateFormatter),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = DateFontSize),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CategoryLabel(item = item)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = NameFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(
                        onClick = { onEditClicked(item.id) },
                        modifier = Modifier.size(EditButtonSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.cd_edit_button, item.name),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(EditIconSize)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/**
 * Miniatura del producto: la foto si la tiene y, si no, un bloque del color de su estado (CA-INV-01-03). El estado
 * se anuncia en la descripción para que el color no sea la única señal (RNF-INV-04).
 */
@Composable
private fun ItemThumbnail(
    item: InventoryItem,
    status: ExpiryStatus,
) {
    val statusLabel = stringResource(id = status.titleRes)
    Row(
        modifier = Modifier
            .width(ThumbnailWidth)
            .height(ThumbnailHeight)
            .semantics { contentDescription = statusLabel }
    ) {
        // Barra del estado: se mantiene aunque el producto tenga foto.
        Box(
            modifier = Modifier
                .width(StatusBarWidth)
                .fillMaxHeight()
                .background(color = status.color, shape = LeadingThumbnailShape)
        )
        val photo = item.photo
        if (photo == null) {
            Box(
                modifier = Modifier
                    .size(ThumbnailHeight)
                    .background(color = status.color, shape = TrailingThumbnailShape)
            )
        } else {
            AsyncImage(
                model = photo.url,
                contentDescription = stringResource(id = R.string.cd_item_photo, item.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ThumbnailHeight)
                    .clip(TrailingThumbnailShape)
            )
        }
    }
}

/**
 * Etiqueta verde con la categoría del producto y, si tiene, su cantidad (CA-INV-01-02 y CA-INV-01-08).
 */
@Composable
private fun CategoryLabel(item: InventoryItem) {
    val category = stringResource(id = item.category.titleRes)
    val quantityText = item.quantity?.let { quantity ->
        stringResource(
            id = R.string.inventory_quantity_format,
            quantity.formattedAmount(),
            stringResource(id = quantity.unit.titleRes)
        )
    }
    val label = if (quantityText == null) {
        category
    } else {
        stringResource(id = R.string.inventory_category_with_quantity, category, quantityText)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = CategoryFontSize,
            fontWeight = FontWeight.Bold
        ),
        color = GreenCheck,
        maxLines = 1,
        modifier = Modifier
            .background(color = GreenMain20, shape = CategoryLabelShape)
            .padding(
                horizontal = MaterialTheme.spacing.extraSmall,
                vertical = MaterialTheme.spacing.extraSmall
            )
    )
}

/**
 * La app está en español (spec 002), así que la cantidad va con la convención de Colombia: coma decimal y sin ceros
 * sobrantes, como el "1,5 kg" de CA-INV-01-08.
 */
private fun Quantity.formattedAmount(): String =
    String.format(QuantityLocale, "%.2f", amount)
        .trimEnd('0')
        .trimEnd(',', '.')

private val QuantityLocale: Locale = Locale.forLanguageTag("es-CO")

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Medidas del Figma (`476:3116`): fila de 60 px, miniatura de 73 px (barra de 13 + imagen de 60) con radio 15,
// separación de 14 px y textos de 13, 20 y 10. No son de formulario, así que no van en FormMetrics.
private val RowHeight = 60.dp
private val ThumbnailHeight = 60.dp
private val StatusBarWidth = 13.dp
private val ThumbnailWidth = 73.dp
private val ThumbnailRadius = 15.dp
private val ThumbnailToTextGap = 14.dp
private val EditButtonSize = 32.dp
private val EditIconSize = 20.dp
private val DateFontSize = 13.sp
private val NameFontSize = 20.sp
private val CategoryFontSize = 10.sp
private val CategoryLabelShape = RoundedCornerShape(5.dp)
private val LeadingThumbnailShape = RoundedCornerShape(
    topStart = ThumbnailRadius,
    bottomStart = ThumbnailRadius
)
private val TrailingThumbnailShape = RoundedCornerShape(
    topEnd = ThumbnailRadius,
    bottomEnd = ThumbnailRadius
)
