/**
 * Nombre: InventorySearchViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de la búsqueda del inventario (spec 006, HU-INV-06). Observa el inventario agrupado por
 * vencimiento y delega el filtrado en [SearchInventoryUseCase], sin repetir la normalización del dominio.
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @param observeInventoryUseCase Inventario del usuario agrupado por estado de vencimiento (HU-INV-01).
 * @param searchInventoryUseCase Filtrado por texto y categoría (RF-INV-05).
 */
@HiltViewModel
class InventorySearchViewModel @Inject constructor(
    observeInventoryUseCase: ObserveInventoryUseCase,
    private val searchInventoryUseCase: SearchInventoryUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<FoodCategory?>(null)

    private val _effects = Channel<InventorySearchEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso (navegación), recolectados con `ObserveEffects` desde el grafo. */
    val effects = _effects.receiveAsFlow()

    /**
     * Estado de la pantalla (CA-INV-06-01). El estado de vencimiento de cada resultado sale de las mismas secciones
     * que alimentan la lista, así que no se vuelve a clasificar por producto.
     */
    val uiState: StateFlow<InventorySearchUiState> = combine(
        observeInventoryUseCase(),
        query,
        selectedCategory
    ) { sections, text, category ->
        val statusById = buildMap {
            sections.forEach { section ->
                section.items.forEach { put(it.id, section.status) }
            }
        }
        val results = searchInventoryUseCase(sections.flatMap { it.items }, text, category)
            .map { SearchResultUi(it, statusById.getValue(it.id)) }
        InventorySearchUiState(query = text, selectedCategory = category, results = results)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMs),
        initialValue = InventorySearchUiState(isLoading = true)
    )

    fun onEvent(event: InventorySearchEvent) {
        when (event) {
            is InventorySearchEvent.QueryChanged -> query.value = event.query
            InventorySearchEvent.ClearQueryClicked -> query.value = ""
            is InventorySearchEvent.CategorySelected -> selectedCategory.value = event.category
            // La tarjeta "Usa tu voz" todavía no escucha: la voz es del #43 (T035).
            InventorySearchEvent.VoiceClicked -> Unit
            is InventorySearchEvent.ResultClicked -> send(InventorySearchEffect.NavigateToEditor(event.itemId))
            InventorySearchEvent.BackClicked -> send(InventorySearchEffect.NavigateBack)
        }
    }

    private fun send(effect: InventorySearchEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        /** Tiempo que el estado sobrevive sin suscriptores, igual que en el resto de la app. */
        const val StopTimeoutMs = 5_000L
    }
}
