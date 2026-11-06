package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import com.smartfood.app.feature.inventory.ui.editor.EditorLineTextField
import com.smartfood.app.feature.recipes.domain.model.Ingredient
import com.smartfood.app.feature.recipes.domain.model.IngredientUnit
import com.smartfood.app.feature.recipes.ui.components.OutlineActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeSearchField

/**
 * Nombre: IngredientSheet.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Hoja "Añadir ingrediente" (spec 010, CA-SAV-03-03, T013): busca en el catálogo y pide la cantidad en su
 * unidad. El Figma no la define (desviación §9), así que usa el estilo de `OptionSheet`.
 *
 * @param sheet Estado de la hoja.
 * @param catalog Ingredientes del catálogo.
 * @param onEvent Acciones de la persona.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IngredientSheet(
    sheet: IngredientSheetState,
    catalog: List<Ingredient>,
    onEvent: (OwnRecipeEditorEvent) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val metrics = LocalFormMetrics.current
    val selected = catalog.firstOrNull { it.id == sheet.selectedId }
    ModalBottomSheet(
        onDismissRequest = { onEvent(OwnRecipeEditorEvent.IngredientSheetDismissed) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = spacing.panelRadius, topEnd = spacing.panelRadius)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = metrics.formHorizontalPadding)
                .padding(bottom = metrics.sectionSpacing)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = stringResource(id = R.string.own_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            if (selected == null) {
                SearchList(sheet, catalog, onEvent)
            } else {
                QuantityForm(sheet, selected, onEvent)
            }
        }
    }
}

@Composable
private fun SearchList(
    sheet: IngredientSheetState,
    catalog: List<Ingredient>,
    onEvent: (OwnRecipeEditorEvent) -> Unit,
) {
    RecipeSearchField(
        query = sheet.query,
        placeholder = stringResource(id = R.string.own_sheet_search),
        onQueryChanged = { onEvent(OwnRecipeEditorEvent.IngredientQueryChanged(it)) },
        onClearQuery = { onEvent(OwnRecipeEditorEvent.IngredientQueryChanged("")) }
    )
    val query = SearchInventoryUseCase.normalize(sheet.query)
    val found = catalog.filter { query.isEmpty() || SearchInventoryUseCase.normalize(it.name).contains(query) }
    if (found.isEmpty()) {
        Text(
            text = stringResource(id = R.string.own_sheet_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)
        )
    }
    LazyColumn(modifier = Modifier.heightIn(max = ListMaxHeight)) {
        items(items = found, key = { it.id }) { ingredient ->
            Text(
                text = ingredient.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onEvent(OwnRecipeEditorEvent.IngredientPicked(ingredient.id)) }
                    .padding(vertical = RowPadding)
            )
        }
    }
}

@Composable
private fun QuantityForm(
    sheet: IngredientSheetState,
    ingredient: Ingredient,
    onEvent: (OwnRecipeEditorEvent) -> Unit,
) {
    val unit = when (ingredient.unit) {
        IngredientUnit.GRAM -> "g"
        IngredientUnit.MILLILITER -> "ml"
        IngredientUnit.UNIT -> "und"
    }
    Text(
        text = ingredient.name,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    )
    EditorLineTextField(
        value = sheet.quantity,
        onValueChange = { onEvent(OwnRecipeEditorEvent.IngredientQuantityChanged(it)) },
        placeholder = "${stringResource(id = R.string.own_sheet_quantity)} ($unit)",
        icon = {},
        isError = sheet.isQuantityInvalid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
    if (sheet.isQuantityInvalid) {
        Text(
            text = stringResource(id = R.string.own_sheet_quantity_error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGap), modifier = Modifier.fillMaxWidth()) {
        RecipeActionButton(
            text = stringResource(id = if (sheet.editingId == null) R.string.own_sheet_confirm else R.string.own_sheet_update),
            onClick = { onEvent(OwnRecipeEditorEvent.IngredientConfirmed) },
            modifier = Modifier.weight(1f)
        )
        if (sheet.editingId != null) {
            OutlineActionButton(
                text = stringResource(id = R.string.own_sheet_remove),
                onClick = { onEvent(OwnRecipeEditorEvent.IngredientRemoved) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private val ListMaxHeight = 280.dp
private val RowPadding = 12.dp
private val ButtonGap = 12.dp
