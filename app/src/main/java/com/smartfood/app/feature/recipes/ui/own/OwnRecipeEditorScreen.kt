package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfood.app.feature.recipes.domain.model.RecipeCategory

/** Editor stateless de receta propia; las hojas avanzadas se completan sobre este contrato (T013). */
@Composable
fun OwnRecipeEditorScreen(state: OwnRecipeEditorUiState, onEvent: (OwnRecipeEditorEvent) -> Unit) {
    var categoryOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Nueva receta")
        OutlinedTextField(state.draft.name, { onEvent(OwnRecipeEditorEvent.NameChanged(it)) }, label = { Text("Nombre de la receta *") })
        Button(onClick = { categoryOpen = true }) { Text(state.draft.category?.name ?: "Categoría *") }
        DropdownMenu(categoryOpen, { categoryOpen = false }) {
            RecipeCategory.entries.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { categoryOpen = false; onEvent(OwnRecipeEditorEvent.CategoryPicked(category)) }) }
        }
        OutlinedTextField(state.draft.cookMinutes, { onEvent(OwnRecipeEditorEvent.CookMinutesChanged(it)) }, label = { Text("Tiempo de cocción *") })
        OutlinedTextField(state.draft.preparation, { onEvent(OwnRecipeEditorEvent.PreparationChanged(it)) }, label = { Text("Preparación *") }, minLines = 4)
        state.error?.let { Text("⚠ No pudimos guardar los cambios") }
        Button(enabled = !state.isSaving, onClick = { onEvent(OwnRecipeEditorEvent.SaveClicked) }) { Text("Guardar receta") }
    }
}
