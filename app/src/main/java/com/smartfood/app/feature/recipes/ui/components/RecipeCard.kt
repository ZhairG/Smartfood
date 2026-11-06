package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenDark
import com.smartfood.app.core.designsystem.theme.GreenSoft
import com.smartfood.app.core.designsystem.theme.GreyDivider
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.SurfaceLight
import com.smartfood.app.core.designsystem.util.figmaShadow
import com.smartfood.app.feature.recipes.ui.discover.RecipeCardUi
import com.smartfood.app.feature.recipes.ui.components.formatCop

/**
 * Sombra de las tarjetas de recetas del Figma (`557:3051`, `666:3452`): `0 3 12` verde oscuro al 9,4 %. El desenfoque
 * de [figmaShadow] es la mitad del de Figma.
 */
internal fun Modifier.recipeCardShadow(): Modifier = figmaShadow(
    color = GreenDark.copy(alpha = RecipeCardShadowAlpha),
    borderRadius = RecipeCardShadowRadius,
    blurRadius = RecipeCardShadowBlur,
    offsetY = RecipeCardShadowOffsetY,
)

private const val RecipeCardShadowAlpha = 0.094f
private val RecipeCardShadowRadius = 20.dp
private val RecipeCardShadowBlur = 6.dp
private val RecipeCardShadowOffsetY = 3.dp

/**
 * Tarjeta de receta del Figma (`557:3051`) con ♡ para marcar favorita.
 *
 * @param card Datos de la receta.
 * @param isFavorite `true` si el ♡ debe mostrarse relleno.
 * @param onClick Abre el detalle.
 * @param onFavoriteToggled Se invoca al tocar el ♡.
 */
@Composable
fun RecipeCard(
    card: RecipeCardUi,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggled: () -> Unit,
) {
    val shape = RoundedCornerShape(CardRadius)
    val highlighted = card.expiringIngredient != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .recipeCardShadow()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (highlighted) Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, shape) else Modifier
            )
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PhotoAspectRatio)
        ) {
            RecipePhoto(
                photo = card.photo,
                modifier = Modifier.fillMaxWidth().aspectRatio(PhotoAspectRatio)
            )
            FavoriteButton(
                isFavorite = isFavorite,
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
            if (card.expiringIngredient != null) {
                Text(
                    text = stringResource(id = R.string.recipes_expiring_badge, card.expiringIngredient.lowercase()),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // El Figma deja la línea del aviso vacía para que los nombres queden alineados.
                Spacer(modifier = Modifier.height(BadgeLineHeight))
            }
            Text(
                text = card.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    id = R.string.recipes_card_meta,
                    card.minutes,
                    card.kcal,
                    formatCop(card.costCop)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
                maxLines = 2
            )
        }
    }
}

// Medidas del Figma 557:3028. No son de formulario, así que no van en FormMetrics.
private val CardRadius = 20.dp
// La foto de la tarjeta se ve de 173 × 111 en el Figma (557:3028).
private val PhotoAspectRatio = 173f / 111f
private val CardTextSidePadding = 10.dp
private val CardTextVerticalPadding = 8.dp
private val CardTextGap = 5.dp
private val BadgeLineHeight = 12.dp
