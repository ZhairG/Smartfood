package com.smartfood.app.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.smartfood.app.feature.auth.domain.usecase.LogoutUseCase
import com.smartfood.app.feature.inventory.domain.usecase.HasPendingChangesUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Nombre: ProfileViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de la pestaña Perfil (spec 001 HU-05; spec 006 CA-INV-09-03; Figma `306:369`). Lee el perfil
 * del usuario (nombre, correo e iniciales), cuenta los productos del inventario y gestiona el cierre de sesión, que
 * es la única acción que funciona por ahora.
 *
 * @param getCurrentUser Caso de uso que obtiene el perfil del usuario actual.
 * @param logout Caso de uso de cierre de sesión.
 * @param observeInventory Inventario del usuario, para la tarjeta "Ingredientes en inventario".
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val logout: LogoutUseCase,
    private val observeInventory: ObserveInventoryUseCase,
    private val hasPendingChanges: HasPendingChangesUseCase,
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
            val user = getCurrentUser().getOrNull()
            val fullName = user?.fullName?.trim()?.ifBlank { null }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    firstName = fullName?.substringBefore(' '),
                    fullName = fullName,
                    email = user?.email,
                    initials = fullName?.let(::initialsOf)
                )
            }
        }
        viewModelScope.launch {
            // Si el inventario no se puede leer, la tarjeta queda en 0 sin cerrar la pantalla.
            observeInventory()
                .catch { emit(emptyList()) }
                .collect { sections -> _uiState.update { it.copy(inventoryCount = sections.sumOf { s -> s.items.size }) } }
        }
    }

    /** Procesa un evento de la UI. */
    fun onEvent(event: ProfileEvent) {
        when (event) {
            // Con cambios sin subir, primero se avisa que podrían perderse (CA-INV-10-07, T051).
            ProfileEvent.LogoutClicked -> viewModelScope.launch {
                if (hasPendingChanges()) {
                    _uiState.update { it.copy(dialog = ProfileDialog.ConfirmLogoutWithPending) }
                } else {
                    logoutAndLeave()
                }
            }
            ProfileEvent.LogoutConfirmed -> {
                _uiState.update { it.copy(dialog = null) }
                viewModelScope.launch { logoutAndLeave() }
            }
            ProfileEvent.DialogDismissed -> _uiState.update { it.copy(dialog = null) }
        }
    }

    /** Cierra la sesión (descarta las fotos pendientes, CA-INV-10-07) y vuelve al inicio de sesión (CA-05-03). */
    private suspend fun logoutAndLeave() {
        logout()
        _effects.send(ProfileEffect.NavigateToLogin)
    }

    internal companion object {
        /** Iniciales del avatar: la primera letra de las dos primeras palabras ("Pepito Perez" → "PP"). */
        fun initialsOf(fullName: String): String =
            fullName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    }
}

/**
 * Estado del Perfil.
 *
 * @property isLoading `true` hasta leer el perfil.
 * @property firstName Primer nombre, para el saludo (CA-05-01).
 * @property fullName Nombre completo del encabezado.
 * @property email Correo del encabezado.
 * @property initials Iniciales del avatar.
 * @property inventoryCount Productos del inventario, para la tarjeta "Ingredientes en inventario".
 * @property dialog Aviso abierto sobre el Perfil, o `null`.
 */
@Immutable
data class ProfileUiState(
    val isLoading: Boolean = true,
    val firstName: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val initials: String? = null,
    val inventoryCount: Int = 0,
    val dialog: ProfileDialog? = null,
)

/** Aviso abierto sobre el Perfil. */
sealed interface ProfileDialog {
    /** "Tienes cambios sin subir. Si cierras sesión ahora, podrían perderse" (CA-INV-10-07). */
    data object ConfirmLogoutWithPending : ProfileDialog
}

sealed interface ProfileEvent {
    data object LogoutClicked : ProfileEvent
    data object LogoutConfirmed : ProfileEvent
    data object DialogDismissed : ProfileEvent
}

sealed interface ProfileEffect {
    data object NavigateToLogin : ProfileEffect
}
