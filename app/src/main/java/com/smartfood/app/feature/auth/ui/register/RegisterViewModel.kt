package com.smartfood.app.feature.auth.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.auth.domain.usecase.RegisterUseCase
import com.smartfood.app.feature.auth.domain.usecase.ValidatePasswordUseCase
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
 * <b>Nombre:</b> RegisterViewModel.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> ViewModel de la pantalla de registro (HU-01 y HU-06). La orquestación (cuenta, perfil,<br>
 * verificación y rollback) vive en [RegisterUseCase].<br>
 * </build>
 *
 * @param register Caso de uso de registro.
 * @param validatePassword Validador usado para la lista de requisitos en tiempo real.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val register: RegisterUseCase,
    private val validatePassword: ValidatePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())

    /** Estado observable de la pantalla. */
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)

    /** Efectos de un solo uso, como la navegación. */
    val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

    /** Procesa un evento de la UI. */
    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.FullNameChanged -> _uiState.update { it.copy(fullName = event.fullName, error = null) }
            is RegisterEvent.EmailChanged -> _uiState.update { it.copy(email = event.email, error = null) }
            is RegisterEvent.PasswordChanged -> _uiState.update {
                it.copy(
                    password = event.password,
                    passwordValidation = validatePassword.execute(event.password),
                    error = null
                )
            }
            is RegisterEvent.AcceptedTermsChanged -> _uiState.update { it.copy(acceptedTerms = event.isChecked) }
            RegisterEvent.RegisterClicked, RegisterEvent.RetryConnectionClicked -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = it.error.takeIf { e -> e == AuthError.Network }) }

        viewModelScope.launch {
            register(state.fullName, state.email, state.password, state.acceptedTerms)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    _effects.send(RegisterEffect.NavigateToSuccess)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.toAuthError()) }
                }
        }
    }
}
