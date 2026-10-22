package com.smartfood.app.feature.recipes.ui.ai

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.feature.recipes.domain.model.PlanSuggestion
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton
import com.smartfood.app.feature.recipes.ui.components.formatCop

/** Nombre: AiFlowScreen.kt. Autor: SmartFood Team. Fecha: Octubre 2026. Descripción: Flujo stateless de sugerencias IA. */
@Composable
fun AiFlowScreen(state: AiUiState, onEvent: (AiEvent) -> Unit, onBack: () -> Unit) {
    BackHandler(enabled = state.step == AiStep.Loading) { onEvent(AiEvent.CancelClicked) }
    Column(Modifier.fillMaxSize()) {
        SmartFoodTopBar(title = if (state.step == AiStep.Loading) "Analizando opciones" else if (state.step == AiStep.Plan) "Tu plan de 3 días" else "Sugerencia con IA", onBack = onBack, action = if (state.step == AiStep.Context) ({ Text("Ayuda", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(8.dp).clickable { onEvent(AiEvent.HelpClicked) }) }) else null)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state.step) {
                AiStep.Context -> ContextContent(state, onEvent)
                AiStep.Loading -> LoadingContent(state, onEvent)
                AiStep.Plan -> PlanContent(state, onEvent)
                AiStep.NoPlan -> NoPlanContent(state, onEvent)
            }
        }
    }
}

@Composable private fun ContextContent(state: AiUiState, onEvent: (AiEvent) -> Unit) {
    Text("Cocina con lo que ya tienes", style = MaterialTheme.typography.headlineSmall)
    Text("Revisa el contexto antes de generar. Puedes cambiar cualquier dato.")
    Text("Inventario detectado", style = MaterialTheme.typography.titleMedium)
    if (state.detected.isEmpty()) Text("No hay productos de tu inventario en las recetas.") else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.detected.take(8).forEach { FilterChip(selected = false, onClick = {}, label = { Text(it.name) }) } }
    state.firstExpiring?.let { Text("⚠ El ${it.name} vence pronto.", color = MaterialTheme.colorScheme.secondary) }
    if (state.restrictions.isNotEmpty()) Text("Según tus preferencias: ${state.restrictions.joinToString()}")
    Text("Presupuesto máximo: COP $${formatCop(state.context.budgetCop)}")
    Slider(value = state.context.budgetCop.toFloat(), onValueChange = { onEvent(AiEvent.BudgetChanged((it / 1000).toLong() * 1000) ) }, valueRange = 2_000f..100_000f, steps = 97)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = true, onClick = {}, label = { Text("${state.context.servings} porciones") }); FilterChip(selected = state.context.noSpicy, onClick = { onEvent(AiEvent.NoSpicyChanged(!state.context.noSpicy)) }, label = { Text("Sin picante") }); FilterChip(selected = state.context.maxMinutes != null, onClick = { onEvent(AiEvent.MaxMinutesChanged(if (state.context.maxMinutes == null) 35 else null)) }, label = { Text(state.context.maxMinutes?.let { "$it min" } ?: "Sin límite") }) }
    RecipeActionButton(text = "Generar recomendaciones", onClick = { onEvent(AiEvent.GenerateClicked) }, modifier = Modifier.fillMaxWidth())
}

@Composable private fun LoadingContent(state: AiUiState, onEvent: (AiEvent) -> Unit) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Buscando el mejor aprovechamiento…", style = MaterialTheme.typography.headlineSmall); Text("Comparamos vencimientos, ingredientes faltantes y tu presupuesto."); OutlinedButton(onClick = { onEvent(AiEvent.CancelClicked) }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") } }

@Composable private fun PlanContent(state: AiUiState, onEvent: (AiEvent) -> Unit) { Text("✓ Recomendaciones listas", style = MaterialTheme.typography.titleLarge); state.plan?.days?.forEach { day -> Button(onClick = { onEvent(AiEvent.RecipeClicked(day.recipe.id)) }, modifier = Modifier.fillMaxWidth()) { Text("Día ${day.day}: ${day.recipe.name}\nCOP $${formatCop(day.costCop)} · ${day.recipe.totalMinutes} min") } }; OutlinedButton(onClick = { onEvent(AiEvent.AdjustClicked) }, modifier = Modifier.fillMaxWidth()) { Text("Ajustar") } }

@Composable private fun NoPlanContent(state: AiUiState, onEvent: (AiEvent) -> Unit) { Text("No encontramos una opción segura", style = MaterialTheme.typography.headlineSmall); Text("Con COP $${formatCop(state.context.budgetCop)} y los ingredientes actuales, ninguna receta cumple tus preferencias."); state.suggestions.forEach { Text("• ${suggestionText(it)}") }; RecipeActionButton(text = "Ajustar contexto", onClick = { onEvent(AiEvent.AdjustClicked) }, modifier = Modifier.fillMaxWidth()); OutlinedButton(onClick = { onEvent(AiEvent.EconomicRecipesClicked) }, modifier = Modifier.fillMaxWidth()) { Text("Ver recetas económicas") } }
private fun suggestionText(value: PlanSuggestion): String = when (value) { is PlanSuggestion.RaiseBudget -> "Sube el presupuesto a COP $${formatCop(value.minimumBudgetCop)}"; PlanSuggestion.AllowSpicy -> "Permite recetas picantes"; is PlanSuggestion.AllowMoreTime -> "Sube el tiempo a ${value.minutes} min"; PlanSuggestion.NotEnoughRecipes -> "Agrega más productos a tu inventario" }

@Composable fun AiHelpScreen(onBack: () -> Unit) { val context = LocalContext.current; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) { SmartFoodTopBar("Ayuda", onBack = onBack); Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("¿Cómo decide la IA?", style = MaterialTheme.typography.headlineSmall); Text("Prioriza urgencia de vencimiento, uso del inventario y costo de compra."); Text("Controles y seguridad", style = MaterialTheme.typography.titleLarge); Text("Tus preferencias alimentarias se respetan como restricciones estrictas."); Text("La IA puede equivocarse", color = MaterialTheme.colorScheme.secondary); OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:smartfood.equipo@gmail.com")).apply { putExtra(Intent.EXTRA_SUBJECT, "SmartFood: ayuda con la IA") }) } }, modifier = Modifier.fillMaxWidth()) { Text("Contactar soporte") } } } }
