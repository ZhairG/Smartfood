package com.smartfood.app.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.auth.domain.usecase.LoginUseCase
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
 * Nombre: LoginViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: ViewModel de la pantalla de inicio de sesión (HU-02 y HU-06). Las reglas de validación
 * viven en [LoginUseCase]; este ViewModel solo traduce eventos a estado.
 *
 * @param login Caso de uso de inicio de sesión.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, como la navegación. */
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    /** Procesa un evento de la UI. */
    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> _uiState.update { it.copy(email = event.email, error = null) }
            is LoginEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password, error = null) }
            is LoginEvent.RememberMeChanged -> _uiState.update { it.copy(rememberMe = event.isChecked) }
            LoginEvent.LoginClicked, LoginEvent.RetryConnectionClicked -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        // Si se reintenta tras un error de red, el diálogo sigue visible mostrando "Conectando" (CA-06-02).
        _uiState.update { it.copy(isLoading = true, error = it.error.takeIf { e -> e == AuthError.Network }) }

        viewModelScope.launch {
            login(state.email, state.password, state.rememberMe)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    _effects.send(LoginEffect.NavigateToHome)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.toAuthError()) }
                }
        }
    }
}
