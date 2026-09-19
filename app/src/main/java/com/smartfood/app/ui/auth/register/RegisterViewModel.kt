package com.smartfood.app.ui.auth.register

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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.email, errorMessage = null) }
            is RegisterUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password, errorMessage = null) }
            is RegisterUiEvent.ConfirmPasswordChanged -> _uiState.update { it.copy(confirmPassword = event.confirmPass, errorMessage = null) }
            RegisterUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            RegisterUiEvent.SubmitRegister -> register()
        }
    }

    private fun register() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank() || currentState.confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor completa todos los campos") }
            return
        }
        if (currentState.password != currentState.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.register(currentState.email, currentState.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Error al registrar") }
                }
        }
    }
}
