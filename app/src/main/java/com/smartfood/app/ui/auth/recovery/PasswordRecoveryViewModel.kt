package com.smartfood.app.ui.auth.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    fun onEvent(event: PasswordRecoveryUiEvent) {
        when (event) {
            is PasswordRecoveryUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.email, errorMessage = null) }
            PasswordRecoveryUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            PasswordRecoveryUiEvent.SubmitRecovery -> recoverPassword()
        }
    }

    private fun recoverPassword() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa tu correo electrónico") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.sendPasswordReset(email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Error al enviar correo") }
                }
        }
    }
}
