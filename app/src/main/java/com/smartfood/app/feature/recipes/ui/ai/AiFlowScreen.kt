package com.smartfood.app.feature.recipes.ui.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.GreenDark
import com.smartfood.app.core.designsystem.theme.GreenSoft
import com.smartfood.app.core.designsystem.theme.GreyDivider
import com.smartfood.app.core.designsystem.theme.GreyLight
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.YellowSoft
import com.smartfood.app.core.designsystem.util.figmaShadow
import com.smartfood.app.feature.inventory.ui.components.OptionSheet
import com.smartfood.app.feature.profile.ui.preferences.titleRes
import com.smartfood.app.feature.recipes.domain.model.PlannedDay
import com.smartfood.app.feature.recipes.ui.components.OutlineActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipePhoto
import com.smartfood.app.feature.recipes.ui.components.formatCop
import com.smartfood.app.feature.recipes.ui.components.titleRes
import kotlin.math.roundToInt

/**
 * Nombre: AiFlowScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Flujo de la IA sin estado (spec 009, HU-RIA-02, T015 a T017), fiel al Figma con las desviaciones de la
 * spec §9. Según [AiUiState.step] muestra:
 * - **Contexto** (`633:2948`): inventario detectado con el aviso del que vence antes, "Según tus preferencias",
 *   presupuesto con su tienda, porciones, picante y tiempo, y "Generar recomendaciones" (CA-RIA-02-01).
 * - **Analizando opciones** (`666:3546`): el arco de progreso, el presupuesto y "Cancelar" (CA-RIA-02-02).
 * - **Tu plan de 3 días** (`557:2903`): una tarjeta por día y "¿Por qué te conviene?" (CA-RIA-02-03, 02-06, 02-08).
 * - **Sin coincidencias** (`557:2515`): las sugerencias del agente, "Ajustar contexto" y "Ver recetas económicas"
 *   (CA-RIA-02-07).
 *
 * @param state Estado del flujo.
 * @param onEvent Acciones de la persona.
 * @param onBack Flecha atrás: vuelve a Descubrir (todas las reacciones del prototipo).
 * @param modifier Modificador opcional.
 */
@Composable
fun AiFlowScreen(
    state: AiUiState,
    onEvent: (AiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Atrás mientras analiza cancela el cálculo, como "Cancelar".
    BackHandler(enabled = state.step == AiStep.Loading) { onEvent(AiEvent.CancelClicked) }
    AiSheets(state = state, onEvent = onEvent)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(
                id = when (state.step) {
                    AiStep.Context -> R.string.recipes_ai_title
                    AiStep.Loading -> R.string.recipes_ai_loading_title
                    AiStep.Plan -> R.string.recipes_ai_plan_title
                    AiStep.NoPlan -> R.string.recipes_ai_no_plan_title
                }
            ),
            onBack = onBack,
            action = when (state.step) {
                AiStep.Context -> ({ BarAction(stringResource(id = R.string.recipes_ai_help)) { onEvent(AiEvent.HelpClicked) } })
                AiStep.Plan -> ({ BarAction(stringResource(id = R.string.recipes_ai_adjust)) { onEvent(AiEvent.AdjustClicked) } })
                else -> null
            }
        )
        when (state.step) {
            AiStep.Context -> ScrollContent { ContextContent(state, onEvent) }
            AiStep.Loading -> LoadingContent(state, onEvent, Modifier.weight(1f))
            AiStep.Plan -> ScrollContent { PlanContent(state, onEvent) }
            AiStep.NoPlan -> ScrollContent { NoPlanContent(state, onEvent) }
        }
    }
}

/** "Ayuda" o "Ajustar" a la derecha de la barra (Figma `557:2981` y `557:2908`). */
@Composable
private fun BarAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onPrimary,
        style = CardTitleStyle(),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
    )
}

@Composable
private fun ColumnScope.ScrollContent(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ContentGap),
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(ContentPadding),
        content = content
    )
}

// ---------- Contexto (633:2948) ----------

@Composable
private fun ContextContent(state: AiUiState, onEvent: (AiEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HeadingGap)) {
        Text(
            text = stringResource(id = R.string.recipes_ai_heading),
            color = MaterialTheme.colorScheme.onSurface,
            style = TitleStyle(),
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(id = R.string.recipes_ai_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = BodyStyle()
        )
    }
    WhiteCard {
        Text(
            text = stringResource(id = R.string.recipes_ai_detected),
            color = MaterialTheme.colorScheme.onSurface,
            style = CardTitleStyle()
        )
        if (state.detected.isEmpty()) {
            Text(
                text = stringResource(id = R.string.recipes_ai_detected_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = BodyStyle()
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ChipGap),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                state.detected.forEach { item ->
                    Pill(text = item.name, highlighted = item.id in state.expiringIds)
                }
            }
        }
        state.firstExpiring?.let { item ->
            Text(
                text = when (state.firstExpiringDays) {
                    0 -> stringResource(id = R.string.recipes_ai_expires_today, item.name)
                    1 -> stringResource(id = R.string.recipes_ai_expires_tomorrow, item.name)
                    else -> stringResource(id = R.string.recipes_ai_expires_in_days, item.name, state.firstExpiringDays)
                },
                color = MaterialTheme.colorScheme.secondary,
                style = BodyStyle()
            )
        }
        if (state.restrictions.isNotEmpty()) {
            val names = state.restrictions.map { stringResource(id = it.titleRes) }.joinToString(", ")
            Text(
                text = stringResource(id = R.string.recipes_ai_preferences, names),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = BodyStyle()
            )
        }
    }
    WhiteCard {
        Text(
            text = stringResource(id = R.string.recipes_ai_budget),
            color = MaterialTheme.colorScheme.onSurface,
            style = CardTitleStyle()
        )
        Text(
            text = stringResource(id = R.string.recipes_ai_budget_value, formatCop(state.budgetCop)),
            color = if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.primary else GreenDark,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold)
        )
        Column {
            Text(
                text = stringResource(
                    id = R.string.recipes_ai_budget_store,
                    formatCop(state.budgetCop),
                    stringResource(id = state.store.titleRes)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = BodyStyle()
            )
            Slider(
                value = state.budgetCop.toFloat(),
                onValueChange = { onEvent(AiEvent.BudgetChanged(it.roundToInt().toLong())) },
                valueRange = AiUiState.MIN_BUDGET_COP.toFloat()..AiUiState.MAX_BUDGET_COP.toFloat(),
                steps = BudgetSteps,
                colors = SliderDefaults.colors(
                    thumbColor = GreenCheck,
                    activeTrackColor = GreenCheck,
                    inactiveTrackColor = GreyLight,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                )
            )
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        Pill(
            text = pluralStringResource(R.plurals.recipes_servings, state.servings, state.servings),
            selected = true,
            onClick = { onEvent(AiEvent.ServingsClicked) }
        )
        Pill(
            text = stringResource(id = R.string.recipes_ai_no_spicy),
            selected = state.noSpicy,
            toggle = true,
            onClick = { onEvent(AiEvent.SpicyToggled) }
        )
        Pill(
            text = timeLabel(state.maxMinutes),
            onClick = { onEvent(AiEvent.TimeClicked) }
        )
    }
    RecipeActionButton(
        text = stringResource(id = R.string.recipes_ai_generate),
        onClick = { onEvent(AiEvent.GenerateClicked) },
        enabled = !state.isLoadingContext,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Hojas de porciones y de tiempo (desviación aprobada, spec §9). */
@Composable
private fun AiSheets(state: AiUiState, onEvent: (AiEvent) -> Unit) {
    when (state.sheet) {
        AiSheet.Servings -> OptionSheet(
            title = stringResource(id = R.string.recipes_ai_servings_sheet),
            options = AiUiState.SERVINGS_OPTIONS,
            selected = state.servings,
            label = { pluralStringResource(R.plurals.recipes_servings, it, it) },
            onConfirm = { onEvent(AiEvent.ServingsPicked(it)) },
            onDismiss = { onEvent(AiEvent.DismissSheet) }
        )
        AiSheet.Time -> OptionSheet(
            title = stringResource(id = R.string.recipes_ai_time_sheet),
            options = AiUiState.TIME_OPTIONS,
            selected = state.maxMinutes,
            label = { timeLabel(it) },
            onConfirm = { onEvent(AiEvent.TimePicked(it)) },
            onDismiss = { onEvent(AiEvent.DismissSheet) }
        )
        null -> Unit
    }
}

@Composable
private fun timeLabel(minutes: Int?): String =
    if (minutes == null) stringResource(id = R.string.recipes_ai_no_time_limit) else stringResource(id = R.string.recipes_ai_minutes, minutes)

/**
 * Chip del Figma: blanco con borde gris; verde oscuro si está elegido (porciones, "Sin picante" activo) y amarillo si es
 * un producto que vence en 0 a 3 días. Sin [onClick] solo muestra.
 */
@Composable
private fun Pill(
    text: String,
    selected: Boolean = false,
    highlighted: Boolean = false,
    toggle: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primary
        highlighted && !LocalIsDarkTheme.current -> YellowSoft
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        highlighted && !LocalIsDarkTheme.current -> GreenDark
        else -> MaterialTheme.colorScheme.onSurface
    }
    val base = Modifier
        .clip(CircleShape)
        .background(container)
        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else GreyDivider, CircleShape)
    val interactive = when {
        onClick == null -> base
        toggle -> base.toggleable(value = selected, role = Role.Switch, onValueChange = { onClick() })
        else -> base.clickable(role = Role.Button, onClick = onClick)
    }
    Box(modifier = interactive.padding(horizontal = PillSidePadding, vertical = PillVerticalPadding)) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

// ---------- Analizando opciones (666:3546) ----------

@Composable
private fun LoadingContent(state: AiUiState, onEvent: (AiEvent) -> Unit, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ContentGap, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxWidth()
            .padding(ContentPadding)
    ) {
        // Arco del componente "Progreso" (281:272): verde, girando mientras el agente calcula.
        CircularProgressIndicator(
            color = GreenCheck,
            trackColor = Color.Transparent,
            strokeWidth = ArcStroke,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(ArcSize)
        )
        Text(
            text = stringResource(id = R.string.recipes_ai_loading_heading),
            color = MaterialTheme.colorScheme.onSurface,
            style = TitleStyle(),
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(id = R.string.recipes_ai_loading_body, formatCop(state.budgetCop)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = BodyStyle()
        )
        OutlineActionButton(
            text = stringResource(id = R.string.recipes_ai_cancel),
            onClick = { onEvent(AiEvent.CancelClicked) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------- Tu plan de 3 días (557:2903) ----------

@Composable
private fun PlanContent(state: AiUiState, onEvent: (AiEvent) -> Unit) {
    val plan = state.plan ?: return
    Column(
        verticalArrangement = Arrangement.spacedBy(CardInnerGap),
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(RoundedCornerShape(CardRadius))
            .background(if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.surfaceVariant else GreenSoft)
            .padding(CardPadding)
    ) {
        Text(
            text = stringResource(id = R.string.recipes_ai_ready),
            color = MaterialTheme.colorScheme.onSurface,
            style = CardTitleStyle()
        )
        Text(
            text = stringResource(id = R.string.recipes_ai_ready_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = BodyStyle()
        )
    }
    plan.days.forEach { day ->
        DayCard(day = day, onClick = { onEvent(AiEvent.RecipeClicked(day.recipe.id)) })
    }
    WhiteCard {
        Text(
            text = stringResource(id = R.string.recipes_ai_why),
            color = MaterialTheme.colorScheme.onSurface,
            style = CardTitleStyle()
        )
        // Una sola línea con "•" entre cada razón, como el Figma (557:2903).
        val reasons = state.reasons.map { it.text(state.ingredients, state.detected) }
        Text(
            text = reasons.joinToString(separator = " ") { "• $it" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = BodyStyle()
        )
    }
}

/**
 * Tarjeta de un día con el estilo de las de Descubrir (`557:3028`): foto, "Día N" donde el Figma dice "Mejor opción",
 * nombre y "92 % del inventario · COP $7.800 · 35 min" (CA-RIA-02-03). Sin ♡ (spec §9).
 */
@Composable
private fun DayCard(day: PlannedDay, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CardRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = DayCardElevation, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        RecipePhoto(
            photo = day.recipe.photo,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(DayPhotoRatio)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(DayTextGap),
            modifier = Modifier.padding(horizontal = DayTextSidePadding, vertical = DayTextVerticalPadding)
        ) {
            Text(
                text = stringResource(id = R.string.recipes_ai_day, day.day),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = day.recipe.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    id = R.string.recipes_ai_card_meta,
                    (day.pantryShare * 100).roundToInt(),
                    formatCop(day.costCop),
                    day.recipe.totalMinutes
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp)
            )
        }
    }
}

// ---------- Sin coincidencias (557:2515) ----------

@Composable
private fun ColumnScope.NoPlanContent(state: AiUiState, onEvent: (AiEvent) -> Unit) {
    Icon(
        painter = painterResource(id = R.drawable.ic_figma_no_results),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = NoPlanIconTop)
    )
    Text(
        text = stringResource(id = R.string.recipes_ai_no_plan_heading),
        color = MaterialTheme.colorScheme.onSurface,
        style = TitleStyle(),
        modifier = Modifier.semantics { heading() }
    )
    Text(
        text = stringResource(id = R.string.recipes_ai_no_plan_body, formatCop(state.budgetCop)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = BodyStyle()
    )
    if (state.suggestions.isNotEmpty()) {
        WhiteCard {
            Text(
                text = stringResource(id = R.string.recipes_ai_try),
                color = MaterialTheme.colorScheme.onSurface,
                style = CardTitleStyle()
            )
            val suggestions = state.suggestions.map { it.text() }
            Text(
                text = suggestions.joinToString(separator = " ") { "• $it" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = BodyStyle()
            )
        }
    }
    RecipeActionButton(
        text = stringResource(id = R.string.recipes_ai_adjust_context),
        onClick = { onEvent(AiEvent.AdjustClicked) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlineActionButton(
        text = stringResource(id = R.string.recipes_ai_economic),
        onClick = { onEvent(AiEvent.EconomicRecipesClicked) },
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------- Estilo compartido del Figma ----------

/** Tarjeta blanca de radio 20 con la sombra suave del Figma. */
@Composable
internal fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CardInnerGap),
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(RoundedCornerShape(CardRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(CardPadding),
        content = content
    )
}

/** Sombra suave de las tarjetas del Figma: `0 3 6` verde oscuro al 9 %. */
internal fun Modifier.cardShadow(): Modifier =
    figmaShadow(color = GreenDark.copy(alpha = CardShadowAlpha), borderRadius = CardRadius, blurRadius = 6.dp, offsetY = 3.dp)

/** Poppins Bold de 20 sp. */
@Composable
internal fun TitleStyle() = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)

/** Poppins SemiBold de 13 sp (títulos de tarjeta y acciones de la barra). */
@Composable
internal fun CardTitleStyle() = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 19.5.sp, fontWeight = FontWeight.SemiBold)

/** Poppins Regular de 13 sp con interlineado 1,45. */
@Composable
internal fun BodyStyle() = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 18.85.sp)

// Medidas del Figma 633:2948, 666:3546, 557:2903 y 557:2515. No son de formulario, así que no van en FormMetrics.
internal val ContentPadding = 20.dp
internal val ContentGap = 16.dp
internal val CardRadius = 20.dp
internal val CardPadding = 16.dp
internal val CardInnerGap = 12.dp
private const val CardShadowAlpha = 0.09f
private val HeadingGap = 8.dp
private val ChipGap = 8.dp
private val PillSidePadding = 12.dp
private val PillVerticalPadding = 7.dp
// Pasos de $1.000 entre $2.000 y $100.000: 97 valores intermedios.
private const val BudgetSteps = 97
private val ArcSize = 81.dp
private val ArcStroke = 10.dp
private val DayCardElevation = 3.dp
private const val DayPhotoRatio = 372f / 150f
private val DayTextSidePadding = 10.dp
private val DayTextVerticalPadding = 8.dp
private val DayTextGap = 5.dp
private val NoPlanIconTop = 32.dp

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AiFlowScreenPreview() {
    SmartfoodTheme {
        AiFlowScreen(state = AiUiState(isLoadingContext = false), onEvent = {}, onBack = {})
    }
}
