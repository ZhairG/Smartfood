package com.smartfood.app.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.smartfood.app.feature.auth.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * <build>
 * <b>Nombre:</b> HomeViewModel.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> ViewModel de la pantalla de inicio (HU-05). Lee el perfil del usuario para saludarlo y<br>
 * gestiona el cierre de sesión. Es el punto de entrada de las próximas features (inventario y recetas).<br>
 * </build>
 *
 * @param getCurrentUser Caso de uso que obtiene el perfil del usuario actual.
 * @param logout Caso de uso de cierre de sesión.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val logout: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, como la navegación. */
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Si el perfil no se puede leer, se muestra un saludo genérico (CA-05-02).
            val firstName = getCurrentUser().getOrNull()?.fullName?.trim()?.substringBefore(' ')
            _uiState.update { it.copy(isLoading = false, firstName = firstName?.ifBlank { null }) }
        }
    }

    /** Procesa un evento de la UI. */
    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LogoutClicked -> {
                logout()
                viewModelScope.launch { _effects.send(HomeEffect.NavigateToLogin) }
            }
        }
    }
}

/**
 * Estado inmutable de [HomeScreen].
 *
 * @property isLoading Se está leyendo el perfil.
 * @property firstName Primer nombre del usuario, o `null` si no se pudo leer.
 */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val firstName: String? = null
)

/** Acciones del usuario en [HomeScreen]. */
sealed interface HomeEvent {
    data object LogoutClicked : HomeEvent
}

/** Efectos de un solo uso emitidos por [HomeViewModel]. */
sealed interface HomeEffect {
    data object NavigateToLogin : HomeEffect
}
