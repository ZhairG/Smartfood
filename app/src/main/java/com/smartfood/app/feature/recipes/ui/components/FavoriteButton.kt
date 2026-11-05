package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenCheck

/**
 * Botón ♡ que marca o quita una receta como favorita (spec 010, CA-SAV-01-01 a 01-03, T005).
 *
 * @param isFavorite `true` si la receta ya está marcada como favorita.
 * @param onClick Se invoca cuando la persona toca el ♡.
 * @param modifier Modificador opcional.
 * @param showPill `true` para dibujar el ♡ dentro de una píldora blanca (tarjeta de lista).
 *   `false` para mostrar solo el corazón (barra superior del detalle).
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPill: Boolean = false,
) {
    val heartColor = if (isFavorite) GreenCheck else MaterialTheme.colorScheme.onSurfaceVariant
    val drawable = if (isFavorite) R.drawable.ic_figma_heart_filled else R.drawable.ic_figma_heart

    Box(
        modifier = modifier
            .size(HeartSize)
            .clickable(onClick = onClick)
    ) {
        if (showPill) {
            Box(
                modifier = Modifier
                    .size(HeartPillSize)
                    .clip(RoundedCornerShape(HeartPillRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.Center)
            )
        }
        Icon(
            painter = painterResource(id = drawable),
            contentDescription = if (isFavorite) {
                stringResource(id = R.string.cd_recipes_remove_favorite)
            } else {
                stringResource(id = R.string.cd_recipes_add_favorite)
            },
            tint = heartColor,
            modifier = Modifier.size(HeartIconSize).align(Alignment.Center)
        )
    }
}

// Medidas del Figma 633:2723 y 557:2856. No son de formulario, así que no van en FormMetrics.
private val HeartSize = 24.dp
private val HeartPillSize = 22.dp
private val HeartPillRadius = 10.dp
private val HeartIconSize = 18.dp
