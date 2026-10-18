/**
 * Nombre: PantallaEnFlujo.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Ayudante de las pruebas instrumentadas por flujo (#105). Monta una pantalla sin estado una sola vez y
 * deja cambiar su `UiState` entre comprobaciones, registrando los eventos que emite.
 *
 * @param rule regla de Compose de la prueba.
 * @param inicial estado con el que arranca la pantalla.
 * @param contenido dibuja la pantalla con el estado actual y la función que recibe sus eventos.
 */
package com.smartfood.app.testutil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.ComposeContentTestRule

class PantallaEnFlujo<S, E>(
    private val rule: ComposeContentTestRule,
    inicial: S,
    private val contenido: @Composable (state: S, onEvent: (E) -> Unit) -> Unit
) {
    private val estado = mutableStateOf(inicial)

    /** Eventos emitidos desde el último [paso]. */
    val eventos = mutableListOf<E>()

    /** Monta la pantalla con [inicial]; se llama una vez por prueba. */
    fun mostrar(inicial: S = estado.value) {
        estado.value = inicial
        rule.setContent { contenido(estado.value) { eventos += it } }
    }

    /**
     * Cambia el estado a [nuevo] y espera a que se dibuje. Con `autoAdvance` desactivado (animaciones infinitas, como
     * las ondas del aviso de voz) avanza un solo cuadro en lugar de esperar a que la pantalla quede quieta.
     */
    fun paso(nuevo: S) {
        eventos.clear()
        estado.value = nuevo
        if (rule.mainClock.autoAdvance) rule.waitForIdle() else rule.mainClock.advanceTimeByFrame()
    }
}
