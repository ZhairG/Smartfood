package com.smartfood.app.feature.recipes.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.GreyDivider
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredientGroup
import com.smartfood.app.feature.recipes.domain.model.RecipeIngredientLine
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipePhoto
import com.smartfood.app.feature.recipes.ui.components.formatCop
import com.smartfood.app.feature.recipes.ui.components.formatQuantity

/**
 * Nombre: RecipeDetailScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Detalle de receta (spec 009, HU-RIA-04, CA-RIA-04-01 y 04-02, T008), fiel al Figma `557:2856`: foto,
 * preparación, cocción, reposo y dificultad, selector de porciones, "Precios de referencia" (desviación aprobada por la
 * integración con supermercados) e ingredientes agrupados con la cantidad y el costo de lo que falta; los que alcanzan
 * se marcan "En tu inventario". Es stateless: recibe el estado y avisa los eventos.
 *
 * @param state Estado calculado por el ViewModel.
 * @param onEvent Acciones de la persona.
 * @param onBack Vuelve a Descubrir.
 * @param modifier Modificador opcional.
 * @param onStartCooking Abre "Antes de empezar"; `null` hasta que exista la cocina guiada (#115) y el botón queda
 *   deshabilitado.
 */
@Composable
fun RecipeDetailScreen(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onStartCooking: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = state.name.ifEmpty { stringResource(id = R.string.recipes_title) },
            onBack = onBack
        )
        when {
            state.isLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.hasError -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(ContentPadding)
            ) {
                Text(
                    text = stringResource(id = R.string.recipes_detail_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    textAlign = TextAlign.Center
                )
            }

            // "Empezar a cocinar" flota abajo a la derecha sobre la lista, como en el Figma (557:2884).
            else -> Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = ListBottomPadding)
                ) {
                    item(key = "foto") {
                        RecipePhoto(
                            photo = state.photo,
                            modifier = Modifier
                                .padding(horizontal = ContentPadding, vertical = PhotoVerticalPadding)
                                .fillMaxWidth()
                                .aspectRatio(PhotoAspectRatio)
                                .clip(RoundedCornerShape(PhotoRadius))
                        )
                    }
                    item(key = "tiempos") {
                        TimeMetrics(
                            prepMinutes = state.prepMinutes,
                            cookMinutes = state.cookMinutes,
                            restMinutes = state.restMinutes,
                            difficulty = state.difficulty
                        )
                    }
                    item(key = "ingredientes") {
                        IngredientsHeader(
                            servings = state.servings,
                            onDecrease = { onEvent(RecipeDetailEvent.DecreaseServings) },
                            onIncrease = { onEvent(RecipeDetailEvent.IncreaseServings) }
                        )
                    }
                    item(key = "precios") {
                        ReferencePrices()
                    }
                    item(key = "grupos") {
                        IngredientGroups(groups = state.groups)
                    }
                }
                RecipeActionButton(
                    text = stringResource(id = R.string.recipes_start_cooking),
                    onClick = { onStartCooking?.invoke() },
                    enabled = onStartCooking != null,
                    trailingIcon = R.drawable.ic_figma_play,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = CtaEndPadding, bottom = CtaBottomPadding)
                        .width(CtaWidth)
                )
            }
        }
    }
}

/** Las cuatro tarjetas del Figma (`557:2861`): preparación, cocción, reposo y dificultad. */
@Composable
private fun TimeMetrics(
    prepMinutes: Int,
    cookMinutes: Int,
    restMinutes: Int,
    difficulty: String,
) {
    val maxMinutes = maxOf(prepMinutes, cookMinutes, restMinutes).coerceAtLeast(1)
    Row(
        horizontalArrangement = Arrangement.spacedBy(MetricGap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPadding)
    ) {
        TimeCard(
            value = stringResource(id = R.string.recipes_minutes, prepMinutes),
            label = stringResource(id = R.string.recipes_prep),
            progress = prepMinutes.toFloat() / maxMinutes,
            modifier = Modifier.weight(1f)
        )
        TimeCard(
            value = stringResource(id = R.string.recipes_minutes, cookMinutes),
            label = stringResource(id = R.string.recipes_cook),
            progress = cookMinutes.toFloat() / maxMinutes,
            modifier = Modifier.weight(1f)
        )
        TimeCard(
            value = stringResource(id = R.string.recipes_minutes, restMinutes),
            label = stringResource(id = R.string.recipes_rest),
            progress = restMinutes.toFloat() / maxMinutes,
            modifier = Modifier.weight(1f)
        )
        TimeCard(
            value = difficulty,
            label = stringResource(id = R.string.recipes_difficulty),
            progress = null,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * @param progress Fracción del anillo naranja (0 a 1), o `null` para la dificultad (sin anillo).
 */
@Composable
private fun TimeCard(
    value: String,
    label: String,
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val trackColor = GreyDivider
    val progressColor = MaterialTheme.colorScheme.secondary
    Card(
        shape = RoundedCornerShape(MetricRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = MetricElevation),
        modifier = modifier.height(MetricHeight)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MetricInnerPadding)
        ) {
            if (progress != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(MetricRingSize)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = MetricRingStroke.toPx()
                        drawCircle(color = trackColor, style = Stroke(width = strokeWidth))
                        if (progress > 0f) {
                            drawArc(
                                color = progressColor,
                                startAngle = StartAngle,
                                sweepAngle = FullCircle * progress.coerceIn(0f, 1f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text = value,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(MetricRingSize)) {
                    Text(
                        text = value,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(MetricTextGap))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, lineHeight = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** "Ingredientes" con el selector de porciones del Figma (`557:2880`). */
@Composable
private fun IngredientsHeader(
    servings: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = ContentPadding,
                end = ContentPadding,
                top = IngredientsTopPadding,
                bottom = IngredientsBottomPadding
            )
    ) {
        Text(
            text = stringResource(id = R.string.recipes_ingredients),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PortionGap)
        ) {
            PortionButton(
                text = "−",
                description = stringResource(id = R.string.recipes_remove_portion),
                filled = false,
                enabled = servings > RecipeDetailViewModel.MinServings,
                onClick = onDecrease
            )
            Text(
                text = pluralStringResource(R.plurals.recipes_servings, servings, servings),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            PortionButton(
                text = "+",
                description = stringResource(id = R.string.recipes_add_portion),
                filled = true,
                enabled = servings < RecipeDetailViewModel.MaxServings,
                onClick = onIncrease
            )
        }
    }
}

/** Círculo de "−" o "+" del selector; deshabilitado en los extremos. */
@Composable
private fun PortionButton(
    text: String,
    description: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .alpha(if (enabled) 1f else DisabledAlpha)
            .size(PortionButtonSize)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (filled) MaterialTheme.colorScheme.secondary else GreyDivider,
                shape = CircleShape
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                if (!enabled) disabled()
            }
    ) {
        Text(
            text = text,
            color = if (filled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold)
        )
    }
}

/** Desviación aprobada (spec §9): el Figma dice "Carulla - Optimizado para tu zona". */
@Composable
private fun ReferencePrices() {
    Box(
        modifier = Modifier
            .padding(horizontal = ContentPadding, vertical = ReferenceVerticalPadding)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = ReferenceSidePadding, vertical = ReferenceBarPadding)
    ) {
        Text(
            text = stringResource(id = R.string.recipes_reference_prices),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

/** Secciones de ingredientes en el orden del Figma. */
@Composable
private fun IngredientGroups(groups: List<RecipeIngredientGroup>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(IngredientRowGap),
        modifier = Modifier.padding(horizontal = ContentPadding)
    ) {
        groups.forEach { group ->
            Text(
                text = group.section,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(top = SectionTopPadding)
                    .semantics { heading() }
            )
            group.lines.forEach { line -> IngredientRow(line = line) }
        }
    }
}

/** Fila blanca del ingrediente con su cantidad y "En tu inventario" o el costo de lo que falta. */
@Composable
private fun IngredientRow(line: RecipeIngredientLine) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(IngredientRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = IngredientSidePadding, vertical = IngredientVerticalPadding)
    ) {
        Text(
            text = "${line.name} ${quantityText(line)}",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (line.inInventory) {
            Icon(
                painter = painterResource(id = R.drawable.ic_requirement_met),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(InInventoryIconSize)
            )
            Spacer(modifier = Modifier.size(InInventoryIconGap))
            Text(
                text = stringResource(id = R.string.recipes_in_inventory),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        } else {
            Text(
                text = stringResource(id = R.string.recipes_cost, formatCop(line.costCop)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
                maxLines = 1
            )
        }
    }
}

/** Cantidad con la unidad base del catálogo ("300 g", "3 und", "250 ml"). */
@Composable
private fun quantityText(line: RecipeIngredientLine): String {
    val amount = formatQuantity(line.quantity)
    return when (line.unit) {
        IngredientUnit.GRAM -> stringResource(id = R.string.recipes_unit_grams, amount)
        IngredientUnit.MILLILITER -> stringResource(id = R.string.recipes_unit_milliliters, amount)
        IngredientUnit.UNIT -> stringResource(id = R.string.recipes_unit_units, amount)
    }
}

// Medidas del Figma 557:2856. No son de formulario, así que no van en FormMetrics.
private val ContentPadding = 16.dp
private val PhotoVerticalPadding = 12.dp
private const val PhotoAspectRatio = 380f / 215f
private val PhotoRadius = 16.dp
private val MetricGap = 8.dp
private val MetricRadius = 12.dp
private val MetricElevation = 1.dp
private val MetricHeight = 84.dp
private val MetricInnerPadding = 4.dp
private val MetricRingSize = 48.dp
private val MetricRingStroke = 4.dp
private val MetricTextGap = 3.dp
private const val StartAngle = -90f
private const val FullCircle = 360f
private val IngredientsTopPadding = 16.dp
private val IngredientsBottomPadding = 4.dp
private val PortionGap = 8.dp
private val PortionButtonSize = 26.dp
private const val DisabledAlpha = 0.4f
private val ReferenceVerticalPadding = 10.dp
private val ReferenceSidePadding = 16.dp
private val ReferenceBarPadding = 8.dp
private val SectionTopPadding = 12.dp
private val IngredientRowGap = 6.dp
private val IngredientRadius = 10.dp
private val IngredientSidePadding = 14.dp
private val IngredientVerticalPadding = 10.dp
private val InInventoryIconSize = 14.dp
private val InInventoryIconGap = 3.dp
// El botón flotante (Figma 557:2884: 181 × 48, a 12 del borde y 38 sobre la barra inferior); la lista deja ese espacio
// al final para que la última fila no quede debajo.
private val CtaWidth = 181.dp
private val CtaEndPadding = 12.dp
private val CtaBottomPadding = 38.dp
private val ListBottomPadding = 96.dp

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun RecipeDetailScreenPreview() {
    SmartfoodTheme {
        RecipeDetailScreen(
            state = RecipeDetailUiState(
                isLoading = false,
                name = "Pollo guisado con arroz",
                photo = "receta_pollo_guisado",
                prepMinutes = 15,
                cookMinutes = 25,
                restMinutes = 0,
                difficulty = "Media",
                servings = 4,
                groups = listOf(
                    RecipeIngredientGroup(
                        section = "Verduras",
                        lines = listOf(
                            RecipeIngredientLine("tomate", "Tomate", "Verduras", 200.0, IngredientUnit.GRAM, false, 500),
                            RecipeIngredientLine("cebolla", "Cebolla", "Verduras", 100.0, IngredientUnit.GRAM, false, 280),
                        )
                    ),
                    RecipeIngredientGroup(
                        section = "Cereales",
                        lines = listOf(
                            RecipeIngredientLine("arroz", "Arroz", "Cereales", 300.0, IngredientUnit.GRAM, true, 0),
                        )
                    ),
                    RecipeIngredientGroup(
                        section = "Carnes",
                        lines = listOf(
                            RecipeIngredientLine("pollo", "Pollo", "Carnes", 600.0, IngredientUnit.GRAM, true, 0),
                        )
                    ),
                )
            ),
            onEvent = {},
            onBack = {}
        )
    }
}
