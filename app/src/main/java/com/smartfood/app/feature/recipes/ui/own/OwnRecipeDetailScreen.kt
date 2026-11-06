package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.GreenSoft
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe
import com.smartfood.app.feature.recipes.ui.components.FavoriteButton
import com.smartfood.app.feature.recipes.ui.components.OutlineActionButton
import com.smartfood.app.feature.recipes.ui.components.recipeCardShadow

/**
 * Nombre: OwnRecipeDetailScreen.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Detalle de solo lectura de una receta propia (spec 010, CA-SAV-04-05, T014). El Figma no lo define
 * (desviación §9), así que usa el lenguaje del detalle del catálogo, sin "Cocinar" ni precios.
 *
 * @param state Estado del detalle.
 * @param onFavoriteToggled Se llama al tocar el ♡.
 * @param onEdit Abre "Editar receta".
 * @param onBack Vuelve atrás.
 */
@Composable
fun OwnRecipeDetailScreen(
    state: OwnRecipeDetailUiState,
    onFavoriteToggled: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val recipe = state.recipe
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = recipe?.name ?: stringResource(id = R.string.own_detail_title),
            onBack = onBack,
            action = recipe?.let { { FavoriteButton(isFavorite = state.isFavorite, onClick = onFavoriteToggled, showPill = true) } }
        )
        when {
            state.isLoading -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            recipe == null -> Text(
                text = stringResource(id = R.string.own_error_not_found),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(SidePadding)
            )
            else -> DetailContent(recipe, state, onEdit)
        }
    }
}

@Composable
private fun DetailContent(recipe: OwnRecipe, state: OwnRecipeDetailUiState, onEdit: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ContentGap),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SidePadding)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HeaderAspectRatio)
                .clip(RoundedCornerShape(CardRadius))
                .background(GreenSoft)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_book),
                contentDescription = null,
                tint = GreenCheck,
                modifier = Modifier.size(HeaderIconSize)
            )
        }
        Text(
            text = recipe.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = stringResource(
                id = R.string.own_detail_meta,
                stringResource(id = recipe.category.titleRes),
                OwnRecipe.SERVINGS,
                recipe.cookMinutes,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)
        )
        DetailCard(title = stringResource(id = R.string.own_ingredients_label)) {
            if (recipe.ingredients.isEmpty()) {
                BodyText(stringResource(id = R.string.own_detail_no_ingredients))
            }
            recipe.ingredients.forEach { line ->
                val ingredient = state.ingredients[line.ingredientId]
                IngredientRow(
                    name = ingredient?.name ?: line.ingredientId,
                    quantity = ingredientQuantityText(line.quantity, ingredient),
                    onClick = null
                )
            }
        }
        DetailCard(title = stringResource(id = R.string.own_detail_preparation)) {
            BodyText(recipe.preparation)
        }
        OutlineActionButton(
            text = stringResource(id = R.string.own_detail_edit),
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(CardRadius)
    Column(
        verticalArrangement = Arrangement.spacedBy(CardGap),
        modifier = Modifier
            .fillMaxWidth()
            .recipeCardShadow()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(CardPadding)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        )
        content()
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 19.sp)
    )
}

// Medidas del detalle del catálogo (Figma 557:2856). No son de formulario, así que no van en FormMetrics.
private val SidePadding = 20.dp
private val ContentGap = 12.dp
private val CardRadius = 20.dp
private val CardPadding = 16.dp
private val CardGap = 10.dp
private val HeaderIconSize = 74.dp
private const val HeaderAspectRatio = 2f

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun OwnRecipeDetailScreenPreview() {
    SmartfoodTheme {
        OwnRecipeDetailScreen(
            state = OwnRecipeDetailUiState(isLoading = false),
            onFavoriteToggled = {},
            onEdit = {},
            onBack = {}
        )
    }
}
