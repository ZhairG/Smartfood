package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.feature.recipes.domain.model.SavedRecipeCard

/**
 * Nombre: SavedRecipeCard.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Tarjeta de Guardadas (spec 010, CA-SAV-02-02, T007, Figma `633:2723`): tarjeta blanca con foto, ♡,
 * nombre y "N porciones · M min".
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recipeCardShadow(),
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.clickable(role = Role.Button, onClick = onClick)) {
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
                        .offset(FavoriteButtonCardOffset, FavoriteButtonCardOffset),
                    showPill = true,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(CardTextGap),
                modifier = Modifier.padding(horizontal = CardTextSidePadding, vertical = CardTextVerticalPadding)
            ) {
                // El Figma deja la línea del aviso vacía, como en las tarjetas de Recetas.
                Spacer(modifier = Modifier.height(BadgeLineHeight))
                Text(
                    text = card.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(id = R.string.saved_meta, card.servings, card.minutes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Medidas del Figma 633:2723. No son de formulario, así que no van en FormMetrics.
private val CardRadius = 20.dp
private val PhotoAspectRatio = 173f / 111f
private val CardTextSidePadding = 10.dp
private val CardTextVerticalPadding = 8.dp
private val CardTextGap = 5.dp
private val BadgeLineHeight = 12.dp
