/**
 * Nombre: InventorySkeleton.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Esqueleto de "Mi Inventario" mientras se cargan los productos (Skeleton Screen, spec 006, #45): imita un
 * encabezado de sección y cinco filas con las medidas de `InventoryItemRow` (miniatura de 73 × 60, fecha, nombre y
 * etiqueta), en bloques del tema con un brillo que se mueve. El Figma no tiene este estado (spec §8). TalkBack lo
 * anuncia como "Cargando inventario".
 *
 * @param modifier Modificador opcional.
 */
package com.smartfood.app.feature.inventory.ui.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing

@Composable
internal fun InventorySkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(id = R.string.inventory_loading)
    val brush = shimmerBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = MaterialTheme.spacing.extraLarge,
                end = MaterialTheme.spacing.extraLarge,
                top = MaterialTheme.spacing.large
            )
            // Una sola descripción para todo el esqueleto: los bloques no son contenido.
            .clearAndSetSemantics { contentDescription = description }
    ) {
        // Encabezado de sección: el punto y el título (ExpirySectionHeader).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = HeaderVerticalPadding)
        ) {
            Bone(brush, CircleShape, Modifier.size(HeaderDotSize))
            Spacer(Modifier.width(HeaderDotGap))
            Bone(brush, LineShape, Modifier.size(width = 150.dp, height = 18.dp))
        }
        repeat(RowCount) { SkeletonRow(brush) }
    }
}

/** Una fila con las medidas de InventoryItemRow (Figma 476:3116). */
@Composable
private fun SkeletonRow(brush: Brush) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(RowHeight)
        ) {
            Bone(brush, ThumbnailShape, Modifier.size(width = ThumbnailWidth, height = RowHeight))
            Spacer(Modifier.width(ThumbnailToTextGap))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Bone(brush, LineShape, Modifier.size(width = 110.dp, height = 12.dp))
                    Spacer(Modifier.weight(1f))
                    Bone(brush, LineShape, Modifier.size(width = 56.dp, height = 14.dp))
                }
                Bone(brush, LineShape, Modifier.size(width = 130.dp, height = 18.dp))
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = RowToDividerGap, bottom = DividerToNextRowGap)
        )
    }
}

/** Un bloque del esqueleto con el brillo compartido. */
@Composable
private fun Bone(brush: Brush, shape: Shape, modifier: Modifier) {
    Box(modifier = modifier.background(brush, shape))
}

/** Brillo que recorre los bloques de izquierda a derecha; usa colores del tema para el modo oscuro. */
@Composable
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant
    val progress by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ShimmerMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "skeleton_shimmer"
    )
    val start = -ShimmerWidth + (ShimmerTravel + ShimmerWidth) * progress
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(start, 0f),
        end = Offset(start + ShimmerWidth, 0f)
    )
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun InventorySkeletonPreview() {
    SmartfoodTheme { InventorySkeleton() }
}

// Medidas de InventoryItemRow y ExpirySectionHeader (Figma 476:3116 y 476:3095); el esqueleto no es de formulario.
private const val RowCount = 5
private val RowHeight: Dp = 60.dp
private val ThumbnailWidth: Dp = 73.dp
private val ThumbnailToTextGap: Dp = 14.dp
private val RowToDividerGap: Dp = 10.dp
private val DividerToNextRowGap: Dp = 8.dp
private val HeaderDotSize: Dp = 25.dp
private val HeaderDotGap: Dp = 12.dp
private val HeaderVerticalPadding: Dp = 8.dp
private val ThumbnailShape = RoundedCornerShape(15.dp)
private val LineShape = RoundedCornerShape(6.dp)
private const val ShimmerMillis = 1_200
private const val ShimmerWidth = 400f
private const val ShimmerTravel = 1_200f
