package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.inventory.ui.components.OptionSheet
import com.smartfood.app.feature.inventory.ui.editor.EditorLineSelector
import com.smartfood.app.feature.inventory.ui.editor.EditorLineTextField
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeField
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory
import com.smartfood.app.feature.recipes.ui.components.OutlineActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton
import com.smartfood.app.feature.recipes.ui.components.recipeCardShadow

/**
 * Nombre: OwnRecipeEditorScreen.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: "Nueva receta" y "Editar receta" (spec 010, HU-SAV-03 y 04, T013), fieles al Figma `557:2693`, `666:3647`,
 * `557:2654` y `666:3753`. Es stateless: recibe el estado y avisa los eventos.
 *
 * @param state Estado del editor.
 * @param onEvent Acciones de la persona.
 */
@Composable
fun OwnRecipeEditorScreen(state: OwnRecipeEditorUiState, onEvent: (OwnRecipeEditorEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = if (state.isNew) R.string.saved_new_recipe_title else R.string.own_edit_title),
            onBack = { onEvent(OwnRecipeEditorEvent.DiscardClicked) },
            action = {
                if (state.isNew) {
                    BarTextAction(stringResource(id = R.string.own_action_save)) { onEvent(OwnRecipeEditorEvent.SaveClicked) }
                } else {
                    BarTextAction(stringResource(id = R.string.own_action_delete)) { onEvent(OwnRecipeEditorEvent.DeleteClicked) }
                }
            }
        )
        if (state.isLoading) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            EditorForm(state, onEvent)
        }
    }
    if (state.isCategorySheetOpen) {
        OptionSheet(
            title = stringResource(id = R.string.own_category_label),
            options = RecipeCategory.entries,
            selected = state.draft.category,
            label = { stringResource(id = it.titleRes) },
            onConfirm = { onEvent(OwnRecipeEditorEvent.CategoryPicked(it)) },
            onDismiss = { onEvent(OwnRecipeEditorEvent.CategoryDismissed) }
        )
    }
    state.ingredientSheet?.let { sheet -> IngredientSheet(sheet, state.catalog, onEvent) }
}

@Composable
private fun EditorForm(state: OwnRecipeEditorUiState, onEvent: (OwnRecipeEditorEvent) -> Unit) {
    val metrics = LocalFormMetrics.current
    val field = state.errorField
    Column(
        verticalArrangement = Arrangement.spacedBy(FormGap),
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SidePadding, vertical = VerticalPadding)
    ) {
        state.error?.let { error ->
            ErrorCard(ownRecipeErrorReason(error, state.draft.cookMinutes))
        }

        FieldLabel(stringResource(id = if (state.isNew) R.string.own_name_label_required else R.string.own_name_label))
        EditorLineTextField(
            value = state.draft.name,
            onValueChange = { onEvent(OwnRecipeEditorEvent.NameChanged(it)) },
            placeholder = stringResource(id = R.string.own_name_placeholder),
            accessibilityLabel = stringResource(id = R.string.own_name_label_required),
            icon = { FieldImage(R.drawable.ic_figma_recipe_book) },
            isError = field == OwnRecipeField.NAME
        )
        FieldError(field, OwnRecipeField.NAME)

        FieldLabel(stringResource(id = if (state.isNew) R.string.own_category_label_required else R.string.own_category_label))
        EditorLineSelector(
            value = state.draft.category?.let { stringResource(id = it.titleRes) }.orEmpty(),
            placeholder = stringResource(id = R.string.own_category_placeholder),
            onClick = { onEvent(OwnRecipeEditorEvent.CategoryClicked) },
            icon = { FieldImage(R.drawable.ic_figma_tag) },
            trailing = { FieldImage(R.drawable.ic_figma_chevron_right, ChevronSize) },
            isError = field == OwnRecipeField.CATEGORY,
            accessibilityLabel = stringResource(id = R.string.own_category_label_required)
        )
        FieldError(field, OwnRecipeField.CATEGORY)

        IngredientsCard(state, onEvent)
        FieldError(field, OwnRecipeField.INGREDIENTS)

        FieldLabel(stringResource(id = R.string.own_time_label))
        BoxField(
            value = state.draft.cookMinutes,
            onValueChange = { onEvent(OwnRecipeEditorEvent.CookMinutesChanged(it)) },
            label = stringResource(id = R.string.own_time_label),
            isError = field == OwnRecipeField.COOK_MINUTES,
            minHeight = TimeFieldHeight,
            singleLine = true,
            suffix = stringResource(id = R.string.own_time_suffix),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        FieldError(field, OwnRecipeField.COOK_MINUTES)

        FieldLabel(stringResource(id = R.string.own_preparation_label))
        BoxField(
            value = state.draft.preparation,
            onValueChange = { onEvent(OwnRecipeEditorEvent.PreparationChanged(it)) },
            label = stringResource(id = R.string.own_preparation_label),
            isError = field == OwnRecipeField.PREPARATION,
            minHeight = PreparationHeight,
            singleLine = false,
            suffix = null,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)
        )
        FieldError(field, OwnRecipeField.PREPARATION)

        if (state.isNew) {
            Text(
                text = stringResource(id = R.string.own_required_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 19.sp)
            )
        }
        RecipeActionButton(
            text = stringResource(id = R.string.own_save_button),
            onClick = { onEvent(OwnRecipeEditorEvent.SaveClicked) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )
        if (!state.isNew) {
            OutlineActionButton(
                text = stringResource(id = R.string.own_discard_button),
                onClick = { onEvent(OwnRecipeEditorEvent.DiscardClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        // Reserva el alto del formulario mínimo cuando el campo enfocado queda sobre el teclado.
        Box(modifier = Modifier.size(width = 0.dp, height = metrics.sectionSpacing))
    }
}

/** Tarjeta "Ingredientes" del Figma `557:2704`. */
@Composable
private fun IngredientsCard(state: OwnRecipeEditorUiState, onEvent: (OwnRecipeEditorEvent) -> Unit) {
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
        FieldLabel(stringResource(id = R.string.own_ingredients_label))
        state.draft.ingredients.forEach { line ->
            val ingredient = state.catalog.firstOrNull { it.id == line.ingredientId }
            IngredientRow(
                name = ingredient?.name ?: line.ingredientId,
                quantity = ingredientQuantityText(line.quantity, ingredient),
                onClick = { onEvent(OwnRecipeEditorEvent.IngredientClicked(line.ingredientId)) }
            )
        }
        OutlineActionButton(
            text = stringResource(id = R.string.own_add_ingredient),
            onClick = { onEvent(OwnRecipeEditorEvent.AddIngredientClicked) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Aviso rojo del Figma `557:2661`. */
@Composable
private fun ErrorCard(reason: String) {
    val error = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(CardRadius)
    Column(
        verticalArrangement = Arrangement.spacedBy(ErrorCardGap),
        modifier = Modifier
            .fillMaxWidth()
            .recipeCardShadow()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .background(error.copy(alpha = ErrorCardAlpha))
            .padding(CardPadding)
    ) {
        FieldLabel(stringResource(id = R.string.own_error_title))
        Text(text = reason, color = error, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 19.sp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun FieldImage(resource: Int, size: Dp = LocalFormMetrics.current.fieldIconSize) {
    Image(painter = painterResource(id = resource), contentDescription = null, modifier = Modifier.size(size))
}

/** Mensaje rojo bajo el campo [own], si es el inválido (Figma `557:2671`). */
@Composable
private fun FieldError(current: OwnRecipeField?, own: OwnRecipeField) {
    if (current == own) {
        Text(
            text = stringResource(id = own.messageRes),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 19.sp)
        )
    }
}

/** Campo blanco con borde del Figma (`666:3857` y `557:2713`). */
@Composable
private fun BoxField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    minHeight: Dp,
    singleLine: Boolean,
    suffix: String?,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val shape = RoundedCornerShape(FieldRadius)
    val color: Color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val style = textStyle.copy(color = color)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = suffix?.let { SuffixTransformation(" $it") } ?: VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape)
                    .border(BorderStroke(FieldBorder, borderColor), shape)
                    .padding(FieldPadding)
            ) {
                innerTextField()
            }
        }
    )
}

/** Muestra [suffix] después del valor, como "30 min", sin que forme parte del texto editable. */
private class SuffixTransformation(private val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val offsets = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = minOf(offset, text.length)
        }
        return TransformedText(AnnotatedString(text.text + suffix), offsets)
    }
}

// Medidas del Figma 557:2693. No son de formulario, así que no van en FormMetrics.
private val FormGap = 10.dp
private val SidePadding = 20.dp
private val VerticalPadding = 20.dp
private val CardRadius = 20.dp
private val CardPadding = 16.dp
private val CardGap = 12.dp
private val ErrorCardGap = 12.dp
private const val ErrorCardAlpha = 0.14f
private val ChevronSize = 15.dp
private val TimeFieldHeight = 45.dp
private val PreparationHeight = 100.dp
private val FieldRadius = 12.dp
private val FieldBorder = 1.dp
private val FieldPadding = 12.dp

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun OwnRecipeEditorScreenPreview() {
    SmartfoodTheme {
        OwnRecipeEditorScreen(state = OwnRecipeEditorUiState(), onEvent = {})
    }
}
