package com.smartfood.app.feature.profile.ui.preferences

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartfood.app.core.navigation.ObserveEffects

/**
 * Nombre: FoodPreferencesRoute.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Parte con estado de "Preferencias alimentarias" (spec 008, T012). Conecta el ViewModel con la pantalla e
 * intercepta "atrás" para volver al Perfil, donde los cambios sin guardar se descartan.
 *
 * @param onBack Vuelve al Perfil.
 * @param viewModel ViewModel de la pantalla.
 */
@Composable
fun FoodPreferencesRoute(
    onBack: () -> Unit,
    viewModel: FoodPreferencesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler { viewModel.onEvent(FoodPreferencesEvent.BackClicked) }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            FoodPreferencesEffect.NavigateBack -> onBack()
        }
    }

    FoodPreferencesScreen(state = state, onEvent = viewModel::onEvent)
}
