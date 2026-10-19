package com.smartfood.app.feature.profile.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.usecase.LogoutUseCase
import com.smartfood.app.feature.inventory.domain.usecase.HasPendingChangesUseCase
import com.smartfood.app.feature.inventory.domain.usecase.ObserveInventoryUseCase
import com.smartfood.app.feature.profile.domain.usecase.ObserveProfileUseCase
import com.smartfood.app.feature.profile.domain.usecase.SyncProfileEmailUseCase
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
 * del usuario en vivo (nombre, correo, foto o iniciales; spec 008 CA-PER-01-01), lo iguala al correo de la cuenta
 * (CA-PER-02-12), cuenta los productos del inventario, abre "Editar información" y gestiona el cierre de sesión.
 *
 * @param observeProfile Perfil del usuario con sesión, en vivo: refleja lo guardado en "Editar información".
 * @param syncProfileEmail Iguala el correo del perfil al de la cuenta tras confirmar un correo nuevo.
 * @param logout Caso de uso de cierre de sesión.
 * @param observeInventory Inventario del usuario, para la tarjeta "Ingredientes en inventario".
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeProfile: ObserveProfileUseCase,
    private val syncProfileEmail: SyncProfileEmailUseCase,
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
            var synced = false
            observeProfile()
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { profile ->
                    val fullName = profile.fullName.trim().ifBlank { null }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            firstName = fullName?.substringBefore(' '),
                            fullName = fullName,
                            email = profile.email,
                            initials = fullName?.let(::initialsOf),
                            photoUrl = profile.photoUrl
                        )
                    }
                    // Una vez por sesión: si se confirmó un correo nuevo, el perfil lo toma (CA-PER-02-12).
                    if (!synced) {
                        synced = true
                        syncProfileEmail(profile)
                    }
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
            // Siempre se pide confirmar (Figma 604:2668, CA-05-03); con cambios sin subir, el aviso dice que podrían
            // perderse (CA-INV-10-07).
            ProfileEvent.LogoutClicked -> viewModelScope.launch {
                _uiState.update { it.copy(dialog = ProfileDialog.ConfirmLogout(hasPendingChanges = hasPendingChanges())) }
            }
            ProfileEvent.LogoutConfirmed -> {
                _uiState.update { it.copy(dialog = null) }
                viewModelScope.launch { logoutAndLeave() }
            }
            ProfileEvent.DialogDismissed -> _uiState.update { it.copy(dialog = null) }
            ProfileEvent.EditProfileClicked -> viewModelScope.launch { _effects.send(ProfileEffect.NavigateToEditProfile) }
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
 * @property photoUrl Foto de perfil, o `null` para mostrar las iniciales (CA-PER-01-01).
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
    val photoUrl: String? = null,
    val inventoryCount: Int = 0,
    val dialog: ProfileDialog? = null,
)

/** Aviso abierto sobre el Perfil. */
sealed interface ProfileDialog {
    /**
     * "¿Cerrar sesión?" (Figma 604:2668, CA-05-03).
     *
     * @property hasPendingChanges `true` si hay cambios sin subir: el aviso dice que podrían perderse (CA-INV-10-07).
     */
    data class ConfirmLogout(val hasPendingChanges: Boolean) : ProfileDialog
}

sealed interface ProfileEvent {
    data object LogoutClicked : ProfileEvent
    data object LogoutConfirmed : ProfileEvent
    data object DialogDismissed : ProfileEvent
    data object EditProfileClicked : ProfileEvent
}

sealed interface ProfileEffect {
    data object NavigateToLogin : ProfileEffect
    data object NavigateToEditProfile : ProfileEffect
}
