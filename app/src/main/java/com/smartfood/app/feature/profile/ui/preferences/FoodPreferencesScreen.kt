package com.smartfood.app.feature.profile.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodLoadingDialog
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.components.SmartFoodSaveChangesDialog
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.formMetricsFor
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.auth.ui.messageRes
import com.smartfood.app.feature.profile.domain.model.Cuisine
import com.smartfood.app.feature.profile.domain.model.DietaryRestriction
import com.smartfood.app.feature.profile.domain.model.FoodPreferences
import com.smartfood.app.feature.profile.domain.model.MealBudget
import com.smartfood.app.feature.profile.domain.model.SpiceLevel
import com.smartfood.app.feature.profile.domain.model.Supermarket

/**
 * Nombre: FoodPreferencesScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla sin estado de "Preferencias alimentarias" (spec 008, HU-PER-03; Figma `376:1260`, `593:1976` y
 * `593:1898`). Barra verde con la flecha, los cuatro grupos de chips —restricciones y cocinas permiten varios; picante
 * y presupuesto, uno— y "Guardar Preferencias", que se deshabilita sin cambios (CA-PER-03-01 a 03-03). La barra
 * inferior la pone `MainTabScaffold`.
 *
 * @param state Estado de la pantalla.
 * @param onEvent Recibe lo que hace el usuario.
 */
@Composable
fun FoodPreferencesScreen(
    state: FoodPreferencesUiState,
    onEvent: (FoodPreferencesEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = R.string.preferences_title),
            onBack = { onEvent(FoodPreferencesEvent.BackClicked) }
        )
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalFormMetrics provides formMetricsFor(maxHeight)) {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    FoodPreferencesContent(state = state, onEvent = onEvent)
                }
            }
        }
    }

    FoodPreferencesDialogs(state = state, onEvent = onEvent)
}

/** Grupos de chips y botón, con el error debajo si el guardado falló. */
@Composable
private fun FoodPreferencesContent(
    state: FoodPreferencesUiState,
    onEvent: (FoodPreferencesEvent) -> Unit,
) {
    val metrics = LocalFormMetrics.current
    val spacing = MaterialTheme.spacing

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = metrics.formHorizontalPadding, vertical = metrics.sectionSpacing)
        ) {
            PreferenceGroup(title = stringResource(id = R.string.preferences_restrictions)) {
                DietaryRestriction.entries.forEach { restriction ->
                    PreferenceChip(
                        text = stringResource(id = restriction.titleRes),
                        selected = restriction in state.draft.restrictions,
                        onClick = { onEvent(FoodPreferencesEvent.RestrictionToggled(restriction)) }
                    )
                }
            }
            PreferenceGroup(title = stringResource(id = R.string.preferences_cuisines)) {
                Cuisine.entries.forEach { cuisine ->
                    PreferenceChip(
                        text = stringResource(id = cuisine.titleRes),
                        selected = cuisine in state.draft.cuisines,
                        onClick = { onEvent(FoodPreferencesEvent.CuisineToggled(cuisine)) }
                    )
                }
            }
            PreferenceGroup(title = stringResource(id = R.string.preferences_spice)) {
                SpiceLevel.entries.forEach { level ->
                    PreferenceChip(
                        text = stringResource(id = level.titleRes),
                        selected = state.draft.spice == level,
                        onClick = { onEvent(FoodPreferencesEvent.SpiceSelected(level)) }
                    )
                }
            }
            PreferenceGroup(title = stringResource(id = R.string.preferences_budget)) {
                MealBudget.entries.forEach { budget ->
                    PreferenceChip(
                        text = stringResource(id = budget.titleRes),
                        selected = state.draft.budget == budget,
                        onClick = { onEvent(FoodPreferencesEvent.BudgetSelected(budget)) }
                    )
                }
            }
            // No está en el Figma (desviación aprobada, spec 008 §9): precios de las recetas por tienda (CA-PER-03-05).
            PreferenceGroup(title = stringResource(id = R.string.preferences_store)) {
                Supermarket.entries.forEach { store ->
                    PreferenceChip(
                        text = stringResource(id = store.titleRes),
                        selected = state.draft.storeOrDefault == store,
                        onClick = { onEvent(FoodPreferencesEvent.StoreSelected(store)) }
                    )
                }
            }
        }

        state.error?.let { error ->
            Text(
                text = stringResource(id = error.messageRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = metrics.formHorizontalPadding)
            )
        }
        SmartFoodPrimaryButton(
            text = stringResource(id = R.string.preferences_save),
            onClick = { onEvent(FoodPreferencesEvent.SaveClicked) },
            enabled = state.isSaveEnabled,
            green = true,
            modifier = Modifier.padding(
                start = metrics.formHorizontalPadding,
                end = metrics.formHorizontalPadding,
                top = spacing.small,
                bottom = spacing.medium
            )
        )
    }
}

/** Título del grupo y sus chips, que pasan a la línea siguiente si no caben (spec 002, adaptabilidad). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        // Figma 376:1260: título en el verde oscuro de la app y Bold.
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            content()
        }
    }
}

/** Carga, pregunta de guardado y "Guardado" que se cierra solo (CA-PER-03-03). */
@Composable
private fun FoodPreferencesDialogs(
    state: FoodPreferencesUiState,
    onEvent: (FoodPreferencesEvent) -> Unit,
) {
    if (state.isSaving) {
        SmartFoodLoadingDialog(text = stringResource(id = R.string.preferences_saving))
    }
    when (state.dialog) {
        FoodPreferencesDialog.ConfirmSave -> SmartFoodSaveChangesDialog(
            question = stringResource(id = R.string.preferences_confirm_save),
            confirmText = stringResource(id = R.string.edit_profile_save),
            dismissText = stringResource(id = R.string.edit_profile_cancel),
            onConfirm = { onEvent(FoodPreferencesEvent.SaveConfirmed) },
            onDismiss = { onEvent(FoodPreferencesEvent.DialogDismissed) }
        )
        // Figma 593:1898: círculo verde claro con el chulo verde y "Preferencias se actualizó" en naranja.
        FoodPreferencesDialog.Saved -> SmartFoodResultDialog(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * GlyphFraction)
                )
            },
            title = stringResource(id = R.string.preferences_saved),
            body = stringResource(id = R.string.preferences_saved_body)
        )
        null -> Unit
    }
}

// Figma 593:1898: el glifo ocupa el 40 % del círculo, como en "Editar información".
private const val GlyphFraction = 0.4f

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun FoodPreferencesScreenPreview() {
    SmartfoodTheme {
        FoodPreferencesScreen(
            state = FoodPreferencesUiState(
                isLoading = false,
                saved = FoodPreferences(),
                draft = FoodPreferences(
                    restrictions = setOf(DietaryRestriction.VEGETARIAN, DietaryRestriction.GLUTEN_FREE),
                    cuisines = setOf(Cuisine.COLOMBIAN, Cuisine.ITALIAN),
                    spice = SpiceLevel.MEDIUM,
                    budget = MealBudget.MODERATE
                )
            ),
            onEvent = {}
        )
    }
}
