package com.smartfood.app.feature.profile

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
 * Nombre: ProfileViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de la pestaña Perfil (spec 001 HU-05; spec 006 CA-INV-09-03). Lee el perfil del usuario
 * para saludarlo y gestiona el cierre de sesión.
 *
 * @param getCurrentUser Caso de uso que obtiene el perfil del usuario actual.
 * @param logout Caso de uso de cierre de sesión.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val logout: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, como la navegación. */
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Si el perfil no se puede leer, se muestra un saludo genérico (CA-05-02).
            val firstName = getCurrentUser().getOrNull()?.fullName?.trim()?.substringBefore(' ')
            _uiState.update { it.copy(isLoading = false, firstName = firstName?.ifBlank { null }) }
        }
    }

    /** Procesa un evento de la UI. */
    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.LogoutClicked -> {
                logout()
                viewModelScope.launch { _effects.send(ProfileEffect.NavigateToLogin) }
            }
        }
    }
}

/**
 * Estado inmutable de [ProfileScreen].
 *
 * @property isLoading Se está leyendo el perfil.
 * @property firstName Primer nombre del usuario, o `null` si no se pudo leer.
 */
@Immutable
data class ProfileUiState(
    val isLoading: Boolean = true,
    val firstName: String? = null
)

/** Acciones del usuario en [ProfileScreen]. */
sealed interface ProfileEvent {
    data object LogoutClicked : ProfileEvent
}

/** Efectos de un solo uso emitidos por [ProfileViewModel]. */
sealed interface ProfileEffect {
    data object NavigateToLogin : ProfileEffect
}
