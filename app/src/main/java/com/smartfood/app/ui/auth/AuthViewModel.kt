package com.smartfood.app.ui.auth

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
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.email, errorMessage = null) }
            }
            is AuthEvent.PasswordChanged -> {
                val isValid = validatePassword(event.password)
                val errorMsg = if (isValid) null else "La contraseña debe tener al menos 6 caracteres"
                _uiState.update {
                    it.copy(
                        password = event.password,
                        isPasswordValid = isValid,
                        passwordError = errorMsg,
                        errorMessage = null
                    )
                }
            }
            AuthEvent.LoginClicked -> login()
            AuthEvent.RegisterClicked -> register()
            AuthEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }

    private fun login() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || !currentState.isPasswordValid) {
            _uiState.update { it.copy(errorMessage = "Verifica los datos ingresados") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.login(currentState.email, currentState.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Error al iniciar sesión") 
                    }
                }
        }
    }

    private fun register() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || !currentState.isPasswordValid) {
            _uiState.update { it.copy(errorMessage = "Verifica los datos ingresados") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.register(currentState.email, currentState.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Error al registrarse") 
                    }
                }
        }
    }
}
