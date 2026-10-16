/**
 * Nombre: InventoryViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de la lista de inventario (spec 006, HU-INV-01, plan §2). Observa el inventario ya agrupado
 * por el dominio y traduce sus fallas a un estado con error tipado. Los efectos de un solo uso (navegación) salen
 * por un `Channel` que las pantallas recolectan con `ObserveEffects`.
 */
package com.smartfood.app.feature.inventory.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.inventory.domain.model.toInventoryError
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @param observeInventoryUseCase Casos de uso que observa el inventario del usuario.
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val observeInventoryUseCase: ObserveInventoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _effects = Channel<InventoryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Observación viva del inventario; al reintentar se cancela la anterior para no dejar dos colecciones. */
    private var observeJob: Job? = null

    init {
        observeInventory()
    }

    /**
     * Atiende lo que el usuario hace en la pantalla.
     *
     * @param event Evento de la UI.
     */
    fun onEvent(event: InventoryEvent) {
        when (event) {
            InventoryEvent.AddClicked -> sendEffect(InventoryEffect.NavigateToEditor())
            InventoryEvent.SearchClicked -> sendEffect(InventoryEffect.NavigateToSearch)
            is InventoryEvent.EditClicked -> sendEffect(InventoryEffect.NavigateToEditor(event.itemId))
            InventoryEvent.RetryClicked -> observeInventory()
        }
    }

    /**
     * Empieza a observar el inventario: cada cambio del repositorio se refleja en el estado sin recargar la pantalla
     * (CA-INV-01-07). Si la observación falla, deja el error tipado en el estado y termina; el usuario puede
     * reintentar con [InventoryEvent.RetryClicked].
     */
    private fun observeInventory() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            observeInventoryUseCase()
                .catch { cause ->
                    _uiState.update { it.copy(isLoading = false, error = cause.toInventoryError()) }
                }
                .collect { sections ->
                    _uiState.update { it.copy(isLoading = false, sections = sections, error = null) }
                }
        }
    }

    private fun sendEffect(effect: InventoryEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
