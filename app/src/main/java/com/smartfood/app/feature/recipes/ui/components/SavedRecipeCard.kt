package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.SavedRecipeCard

/**
 * Tarjeta de Guardadas con ♡ para marcar favorita (spec 010, T007).
 *
 * @param card Datos de la tarjeta.
 * @param onClick Abre el detalle o el editor.
 * @param onFavoriteToggled Se invoca al tocar el ♡.
 */
@Composable
fun SavedRecipeCard(
    card: SavedRecipeCard,
    onClick: () -> Unit,
    onFavoriteToggled: () -> Unit,
) {
    val shape = RoundedCornerShape(CardRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                RecipePhoto(
                    photo = card.photo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(PhotoAspectRatio)
                )
                FavoriteButton(
                    isFavorite = card.isFavorite,
                    onClick = onFavoriteToggled,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(HeartPadding),
                    showPill = true,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(CardTextGap),
            modifier = Modifier.padding(horizontal = CardTextSidePadding, vertical = CardTextVerticalPadding)
        ) {
            Text(
                text = card.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (card.isOwn) {
                    stringResource(id = R.string.saved_meta_servings, 2)
                } else {
                    stringResource(id = R.string.saved_meta, card.servings, card.minutes)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Medidas del Figma 633:2723. No son de formulario, así que no van en FormMetrics.
private val HeartPadding = 8.dp
private val CardRadius = 20.dp
private val CardElevation = 3.dp
private val PhotoAspectRatio = 173f / 111f
private val CardTextSidePadding = 10.dp
private val CardTextVerticalPadding = 8.dp
private val CardTextGap = 5.dp
