/**
 * Nombre: InventorySearchViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de la búsqueda del inventario (spec 006, HU-INV-06 y HU-INV-07). Observa el inventario
 * agrupado por vencimiento, delega el filtrado en [SearchInventoryUseCase] y orquesta la escucha de voz con
 * [SpeechToText] y [ParseSpokenIngredientsUseCase], sin repetir reglas del dominio.
 */
package com.smartfood.app.feature.inventory.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.SpeechError
import com.smartfood.app.feature.inventory.domain.model.SpeechEvent
import com.smartfood.app.feature.inventory.domain.repository.SpeechToText
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ParseSpokenIngredientsUseCase
import com.smartfood.app.feature.inventory.domain.usecase.SearchInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @param observeInventoryUseCase Inventario del usuario agrupado por estado de vencimiento (HU-INV-01).
 * @param searchInventoryUseCase Filtrado por texto y categoría (RF-INV-05).
 * @param speechToText Reconocimiento de voz del sistema (CA-INV-07-01).
 * @param parseSpokenIngredientsUseCase Convierte el dictado en ingredientes (CA-INV-07-03).
 */
@HiltViewModel
class InventorySearchViewModel @Inject constructor(
    observeInventoryUseCase: ObserveInventoryUseCase,
    private val searchInventoryUseCase: SearchInventoryUseCase,
    private val speechToText: SpeechToText,
    private val parseSpokenIngredientsUseCase: ParseSpokenIngredientsUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<FoodCategory?>(null)
    private val voice = MutableStateFlow<VoiceUiState>(VoiceUiState.Hidden)

    private val _effects = Channel<InventorySearchEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso (navegación y permiso), recolectados con `ObserveEffects` desde el grafo. */
    val effects = _effects.receiveAsFlow()

    /** Escucha activa; se cancela al cerrar el aviso para liberar el reconocedor (CA-INV-07-07). */
    private var voiceJob: Job? = null

    /** Se resuelve una sola vez: si el teléfono no tiene servicio, la tarjeta no se muestra (CA-INV-07-08). */
    private val isVoiceAvailable: Boolean = speechToText.isAvailable()

    /**
     * Estado de la pantalla (CA-INV-06-01). El estado de vencimiento de cada resultado sale de las mismas secciones
     * que alimentan la lista, así que no se vuelve a clasificar por producto.
     */
    val uiState: StateFlow<InventorySearchUiState> = combine(
        // Si el inventario no se puede leer (por ejemplo, la sesión venció), la búsqueda queda sin resultados en
        // lugar de cerrar la app: el error no debe llegar sin atrapar hasta la UI.
        observeInventoryUseCase().catch { emit(emptyList()) },
        query,
        selectedCategory,
        voice
    ) { sections, text, category, voiceState ->
        val statusById = buildMap {
            sections.forEach { section ->
                section.items.forEach { put(it.id, section.status) }
            }
        }
        val results = searchInventoryUseCase(sections.flatMap { it.items }, text, category)
            .map { SearchResultUi(it, statusById.getValue(it.id)) }
        InventorySearchUiState(
            query = text,
            selectedCategory = category,
            results = results,
            isVoiceAvailable = isVoiceAvailable,
            voice = voiceState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StopTimeoutMs),
        initialValue = InventorySearchUiState(isLoading = true)
    )

    fun onEvent(event: InventorySearchEvent) {
        when (event) {
            is InventorySearchEvent.QueryChanged -> query.value = event.query
            InventorySearchEvent.ClearQuery -> query.value = ""
            is InventorySearchEvent.CategorySelected -> selectedCategory.value = event.category
            InventorySearchEvent.VoiceClicked -> send(InventorySearchEffect.RequestMicPermission)
            is InventorySearchEvent.VoicePermissionResult ->
                if (event.granted) startListening() else send(InventorySearchEffect.ShowPermissionRationale)

            InventorySearchEvent.VoiceRetry -> startListening()
            InventorySearchEvent.VoiceDismissed -> dismissVoice()
            is InventorySearchEvent.ResultClicked -> send(InventorySearchEffect.NavigateToEditor(event.id))
            InventorySearchEvent.BackClicked -> send(InventorySearchEffect.NavigateBack)
        }
    }

    /** Empieza (o reinicia) la escucha y muestra "Habla ahora" (CA-INV-07-01). */
    private fun startListening() {
        voiceJob?.cancel()
        voice.value = VoiceUiState.Listening
        voiceJob = viewModelScope.launch {
            speechToText.listen().collect { event ->
                when (event) {
                    SpeechEvent.Ready -> voice.value = VoiceUiState.Listening
                    SpeechEvent.Processing -> voice.value = VoiceUiState.Processing
                    is SpeechEvent.Result -> applySpokenIngredients(event.alternatives)
                    is SpeechEvent.Failed -> voice.value = VoiceUiState.Failed(event.error)
                }
            }
        }
    }

    /**
     * Con el dictado, llena la barra con los ingredientes y cierra el aviso (CA-INV-07-03). Si no se separó ningún
     * ingrediente, se trata como que no se entendió nada (CA-INV-07-05).
     */
    private suspend fun applySpokenIngredients(alternatives: List<String>) {
        val ingredients = parseSpokenIngredientsUseCase(alternatives)
        if (ingredients.isEmpty()) {
            voice.value = VoiceUiState.Failed(SpeechError.NO_MATCH)
        } else {
            // Como el Figma, el aviso muestra un momento lo que entendió antes de cerrarse (CA-INV-07-03, T055).
            voice.value = VoiceUiState.Recognized(
                ingredients.joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
            )
            delay(RecognizedVisibleMillis)
            query.value = ingredients.joinToString(IngredientSeparator)
            voice.value = VoiceUiState.Hidden
        }
    }

    /** Cancela la escucha y cierra el aviso sin cambiar la barra (CA-INV-07-07). */
    private fun dismissVoice() {
        voiceJob?.cancel()
        voiceJob = null
        voice.value = VoiceUiState.Hidden
    }

    override fun onCleared() {
        voiceJob?.cancel()
        super.onCleared()
    }

    private fun send(effect: InventorySearchEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        /** Tiempo que el estado sobrevive sin suscriptores, igual que en el resto de la app. */
        const val StopTimeoutMs = 5_000L

        /** Separador con el que los ingredientes dictados llenan la barra: "carne, pollo, leche" (CA-INV-07-03). */
        const val IngredientSeparator = ", "
    }
}

/** Tiempo que el aviso de voz muestra lo que entendió antes de cerrarse (CA-INV-07-03). */
internal const val RecognizedVisibleMillis = 1_500L
