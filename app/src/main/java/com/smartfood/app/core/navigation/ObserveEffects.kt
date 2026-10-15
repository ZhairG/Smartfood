package com.smartfood.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Nombre: ObserveEffects.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Recolecta los efectos de un solo uso de un ViewModel (por ejemplo, la navegación) solo mientras
 * la pantalla está visible, para no navegar desde segundo plano.
 *
 * @param effects Flujo de efectos expuesto por el ViewModel.
 * @param onEffect Acción a ejecutar por cada efecto recibido.
 */
@Composable
fun <T> ObserveEffects(effects: Flow<T>, onEffect: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { currentOnEffect(it) }
        }
    }
}
