package com.smartfood.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.domain.usecase.auth.LoginUseCase
import com.smartfood.app.domain.usecase.auth.RecoverPasswordUseCase
import com.smartfood.app.domain.usecase.auth.RegisterUseCase
import com.smartfood.app.domain.usecase.auth.ValidateEmailUseCase
import com.smartfood.app.domain.usecase.auth.ValidatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Nombre: AuthViewModel.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: ViewModel central para la Autenticación. Intercepta eventos de la UI, 
 * delega la lógica de negocio a los Casos de Uso (Domain Layer) y expone
 * un estado inmutable reactivo (StateFlow).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val recoverPasswordUseCase: RecoverPasswordUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.email).withoutErrors() }
            }
            is AuthEvent.PasswordChanged -> {
                val validation = validatePasswordUseCase.execute(event.password)
                _uiState.update {
                    it.copy(
                        password = event.password,
                        hasMinimumLength = validation.hasMinimumLength,
                        hasUppercase = validation.hasUppercase,
                        hasSpecialCharacter = validation.hasSpecialCharacter,
                        passwordsMatch = event.password == it.confirmPassword
                    ).withoutErrors()
                }
            }
            is AuthEvent.ConfirmPasswordChanged -> {
                _uiState.update {
                    it.copy(
                        confirmPassword = event.confirmPassword,
                        passwordsMatch = it.password == event.confirmPassword,
                        errorMessage = null,
                        errorType = null
                    )
                }
            }
            is AuthEvent.RememberMeChanged -> {
                _uiState.update { it.copy(rememberMe = event.isChecked) }
            }
            AuthEvent.LoginClicked -> login()
            AuthEvent.RegisterClicked -> register()
            AuthEvent.RecoverPasswordClicked -> recoverPassword()
            AuthEvent.RetryConnectionClicked -> retryConnection()
            AuthEvent.DismissError -> _uiState.update { it.copy(errorMessage = null, errorType = null) }
        }
    }

    /**
     * Intenta iniciar sesión usando el caso de uso `LoginUseCase`.
     */
    private fun login() {
        val currentState = _uiState.value

        // Prevención de envíos duplicados (doble toque, o "Listo" del teclado + botón).
        if (currentState.isLoading) return

        val email = currentState.email.trim()
        val password = currentState.password

        // Validaciones locales: evitan una llamada de red innecesaria y marcan el campo con error.
        if (email.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    errorType = AuthErrorType.EMPTY_FIELDS,
                    emailHasError = email.isBlank(),
                    passwordHasError = password.isBlank()
                )
            }
            return
        }
        if (!validateEmailUseCase.execute(email)) {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    errorType = AuthErrorType.INVALID_EMAIL,
                    emailHasError = true,
                    passwordHasError = false
                )
            }
            return
        }

        // Se marca la carga de forma síncrona (antes de lanzar la corrutina) para que un segundo
        // evento inmediato ya encuentre `isLoading = true` y sea descartado por la guarda de arriba.
        _uiState.update { it.copy(email = email, isLoading = true).withoutErrors() }

        viewModelScope.launch {
            loginUseCase.execute(email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    handleError(error, AuthErrorType.CREDENTIALS_ERROR)
                }
        }
    }

    /**
     * Intenta registrar una nueva cuenta usando el caso de uso `RegisterUseCase`.
     */
    private fun register() {
        val currentState = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, errorType = null) }
            registerUseCase.execute(
                currentState.email,
                currentState.password,
                currentState.confirmPassword
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    handleError(error, AuthErrorType.VALIDATION_ERROR)
                }
        }
    }

    /**
     * Intenta enviar un correo de recuperación de contraseña.
     */
    private fun recoverPassword() {
        val currentState = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, errorType = null) }
            recoverPasswordUseCase.execute(currentState.email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    handleError(error, AuthErrorType.UNKNOWN_ERROR)
                }
        }
    }

    /**
     * Limpia los errores e intenta reanudar la acción fallida de acuerdo
     * a los campos previamente llenados.
     */
    private fun retryConnection() {
        _uiState.update { it.copy(isOffline = false, errorMessage = null, errorType = null) }
        
        val currentState = _uiState.value
        if (currentState.email.isNotBlank() && currentState.password.isNotBlank()) {
            if (currentState.hasMinimumLength && currentState.hasUppercase) {
                register() // Caso de Registro
            } else {
                login() // Caso de Login
            }
        }
    }

    /**
     * Analiza una excepción asíncrona y la mapea al estado reactivo correcto.
     * Si detecta fallos de conectividad, enciende la bandera `isOffline`.
     * 
     * @param error Excepción lanzada por la capa de datos.
     * @param defaultType Clasificación semántica del error en caso de que no sea de red.
     */
    private fun handleError(error: Throwable, defaultType: AuthErrorType) {
        val isNetworkError = error.message?.contains("network", ignoreCase = true) == true ||
                             error.message?.contains("host", ignoreCase = true) == true ||
                             error.message?.contains("timeout", ignoreCase = true) == true
        
        if (isNetworkError) {
            _uiState.update { it.copy(isLoading = false, isOffline = true, errorType = AuthErrorType.NETWORK_ERROR) }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false, 
                    errorMessage = error.localizedMessage ?: "Ha ocurrido un error",
                    errorType = defaultType,
                    // Un error de credenciales no indica cuál de los dos datos falló: se marcan ambos (Figma).
                    emailHasError = defaultType == AuthErrorType.CREDENTIALS_ERROR,
                    passwordHasError = defaultType == AuthErrorType.CREDENTIALS_ERROR
                )
            }
        }
    }

    /**
     * Limpia el mensaje, el tipo de error y el borde rojo de los campos. Se usa cuando el usuario
     * vuelve a editar un campo o cuando arranca una nueva petición.
     */
    private fun AuthUiState.withoutErrors(): AuthUiState = copy(
        errorMessage = null,
        errorType = null,
        emailHasError = false,
        passwordHasError = false
    )
}
