package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfood.app.feature.recipes.domain.model.OwnRecipe

/** Detalle de solo lectura de una receta propia (spec 010, CA-SAV-04-05, T014). */
@Composable
fun OwnRecipeDetailScreen(recipe: OwnRecipe, onEdit: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text(recipe.name)
        Text("${recipe.category.name} · 2 porciones · ${recipe.cookMinutes} min")
        recipe.ingredients.forEach { Text("${it.ingredientId}: ${it.quantity}") }
        Text(recipe.preparation)
        Button(onClick = onEdit) { Text("Editar") }
        Button(onClick = onBack) { Text("Volver") }
    }
}
