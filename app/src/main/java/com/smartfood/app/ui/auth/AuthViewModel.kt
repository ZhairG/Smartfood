package com.smartfood.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfood.app.domain.usecase.auth.LoginUseCase
import com.smartfood.app.domain.usecase.auth.RegisterUseCase
import com.smartfood.app.domain.usecase.auth.ResetPasswordUseCase
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
 * Descripción: ViewModel central para el flujo de autenticación (Login, Registro y Recuperación de Contraseña).
 * Intercepta los eventos de usuario [AuthEvent], ejecuta la lógica mediante Casos de Uso
 * de la capa de Dominio y expone un [AuthUiState] inmutable a través de [StateFlow].
 *
 * @param loginUseCase Caso de uso para inicio de sesión.
 * @param registerUseCase Caso de uso para registro de usuarios.
 * @param resetPasswordUseCase Caso de uso para recuperación y restablecimiento de contraseña.
 * @param validatePasswordUseCase Caso de uso para validación de formato de contraseña.
 * @param validateEmailUseCase Caso de uso para validación de formato de correo electrónico.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())

    /** Estado reactivo inmutable expuesto a la vista. */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Procesador centralizado de eventos provistos por la interfaz de usuario.
     *
     * @param event Evento emitido por la UI.
     */
    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.FullNameChanged -> {
                _uiState.update { it.copy(fullName = event.fullName).withoutErrors() }
            }
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
            is AuthEvent.AcceptedTermsChanged -> {
                _uiState.update { it.copy(acceptedTerms = event.isChecked) }
            }
            AuthEvent.LoginClicked -> login()
            AuthEvent.RegisterClicked -> register()
            AuthEvent.RetryConnectionClicked -> retryConnection()
            AuthEvent.DismissError -> _uiState.update { it.copy(errorMessage = null, errorType = null) }

            // --- Eventos de Recuperación de Contraseña ---
            is AuthEvent.OtpCodeChanged -> {
                _uiState.update {
                    it.copy(
                        otpCode = event.code,
                        otpCodeHasError = false,
                        errorType = null,
                        errorMessage = null
                    )
                }
            }
            AuthEvent.SendRecoveryCodeClicked -> sendRecoveryCode()
            AuthEvent.VerifyOtpCodeClicked -> verifyOtpCode()
            AuthEvent.ResendOtpCodeClicked -> sendRecoveryCode()
            AuthEvent.ChangeEmailClicked -> {
                _uiState.update {
                    it.copy(
                        recoveryStep = RecoveryStep.EMAIL_REQUEST,
                        otpCode = "",
                        otpCodeHasError = false,
                        errorType = null,
                        errorMessage = null
                    )
                }
            }
            AuthEvent.ResetPasswordClicked -> resetPassword()
            AuthEvent.ResetRecoveryFlow -> {
                _uiState.update {
                    it.copy(
                        recoveryStep = RecoveryStep.EMAIL_REQUEST,
                        otpCode = "",
                        password = "",
                        confirmPassword = "",
                        otpCodeHasError = false,
                        isPasswordResetSuccess = false,
                        errorType = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    /**
     * Envía la solicitud de código de recuperación para el primer paso vía Firebase Auth.
     */
    private fun sendRecoveryCode() {
        val currentState = _uiState.value
        val email = currentState.email.trim()

        if (email.isBlank()) {
            _uiState.update {
                it.copy(
                    errorType = AuthErrorType.EMPTY_EMAIL,
                    emailHasError = true
                )
            }
            return
        }

        if (!validateEmailUseCase.execute(email)) {
            _uiState.update {
                it.copy(
                    errorType = AuthErrorType.INVALID_EMAIL,
                    emailHasError = true
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, emailHasError = false, errorType = null) }

        viewModelScope.launch {
            resetPasswordUseCase.sendResetCode(email)
                .onSuccess { generatedOtp ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generatedOtpCode = generatedOtp,
                            recoveryStep = RecoveryStep.OTP_VERIFICATION,
                            emailHasError = false,
                            errorType = null
                        )
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: ""
                    val isNotFound = message.contains("not-found", ignoreCase = true) ||
                            message.contains("user-not-found", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            emailHasError = true,
                            errorType = if (isNotFound) AuthErrorType.ACCOUNT_NOT_FOUND_RECOVERY else AuthErrorType.INVALID_EMAIL
                        )
                    }
                }
        }
    }

    /**
     * Verifica el código de 4 dígitos ingresado en el segundo paso.
     */
    private fun verifyOtpCode() {
        val currentState = _uiState.value
        val code = currentState.otpCode

        resetPasswordUseCase.verifyOtpCode(code, currentState.generatedOtpCode)
            .onSuccess {
                _uiState.update {
                    it.copy(
                        recoveryStep = RecoveryStep.NEW_PASSWORD,
                        otpCodeHasError = false,
                        errorType = null
                    )
                }
            }
            .onFailure { error ->
                val errorType = when (error.message) {
                    "EMPTY_OTP_CODE" -> AuthErrorType.EMPTY_OTP_CODE
                    else -> AuthErrorType.INVALID_OTP_CODE
                }
                _uiState.update {
                    it.copy(
                        otpCodeHasError = true,
                        errorType = errorType
                    )
                }
            }
    }

    /**
     * Actualiza la contraseña en Firebase Auth en el tercer paso.
     */
    private fun resetPassword() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val pass = currentState.password
        val confirmPass = currentState.confirmPassword

        val validation = validatePasswordUseCase.execute(pass)
        if (!validation.isValid || pass != confirmPass) {
            _uiState.update {
                it.copy(
                    passwordHasError = !validation.isValid,
                    errorType = AuthErrorType.VALIDATION_ERROR
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorType = null) }

        viewModelScope.launch {
            resetPasswordUseCase.resetPassword(
                email = currentState.email,
                code = currentState.otpCode,
                newPass = pass,
                confirmPass = confirmPass
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPasswordResetSuccess = true
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordHasError = true,
                            errorType = AuthErrorType.VALIDATION_ERROR
                        )
                    }
                }
        }
    }

    /**
     * Inicia el proceso de autenticación validando precondiciones y delegando a [LoginUseCase].
     */
    private fun login() {
        val currentState = _uiState.value

        if (currentState.isLoading) return

        val email = currentState.email.trim()
        val password = currentState.password

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
     * Inicia el proceso de registro de un nuevo usuario delegando a [RegisterUseCase].
     */
    private fun register() {
        val currentState = _uiState.value

        if (currentState.isLoading) return

        val fullName = currentState.fullName.trim()
        val email = currentState.email.trim()
        val password = currentState.password

        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    errorType = AuthErrorType.EMPTY_FIELDS,
                    fullNameHasError = fullName.isBlank(),
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

        _uiState.update { it.copy(isLoading = true, errorMessage = null, errorType = null) }

        viewModelScope.launch {
            registerUseCase.execute(
                email,
                password,
                password // En el formulario de Figma registro usa un único campo de contraseña
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
     * Restablece el estado de desconexión y reintenta la acción correspondiente según los campos ingresados.
     */
    private fun retryConnection() {
        _uiState.update { it.copy(isOffline = false, errorMessage = null, errorType = null) }
        
        val currentState = _uiState.value
        if (currentState.email.isNotBlank() && currentState.password.isNotBlank()) {
            if (currentState.fullName.isNotBlank()) {
                register()
            } else {
                login()
            }
        }
    }

    /**
     * Procesa las excepciones lanzadas por la capa de datos para actualizar el estado UI.
     * Intercepta fallos de cuenta no encontrada, cuenta existente y errores de configuración de Firebase,
     * activando los bordes rojos para los campos de texto en la interfaz.
     *
     * @param error Excepción capturada.
     * @param defaultType Tipo de error predeterminado si no es un fallo de conectividad o cuenta.
     */
    private fun handleError(error: Throwable, defaultType: AuthErrorType) {
        val message = error.message ?: ""
        val isNetworkError = message.contains("network", ignoreCase = true) ||
                             message.contains("host", ignoreCase = true) ||
                             message.contains("timeout", ignoreCase = true)
        
        val isUserNotFound = message.contains("user-not-found", ignoreCase = true) ||
                              message.contains("no user record", ignoreCase = true)

        val isEmailAlreadyInUse = message.contains("email-already-in-use", ignoreCase = true) ||
                                   message.contains("already exists", ignoreCase = true)

        val isConfigNotFound = message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)

        if (isNetworkError) {
            _uiState.update { it.copy(isLoading = false, isOffline = true, errorType = AuthErrorType.NETWORK_ERROR) }
        } else {
            val resolvedType = when {
                isUserNotFound -> AuthErrorType.ACCOUNT_NOT_FOUND
                isEmailAlreadyInUse -> AuthErrorType.ACCOUNT_ALREADY_EXISTS
                else -> defaultType
            }

            val resolvedErrorMessage = when {
                isConfigNotFound -> "El servicio de autenticación por correo no está habilitado en Firebase Console"
                else -> error.localizedMessage ?: "Ha ocurrido un error inesperado"
            }

            _uiState.update {
                it.copy(
                    isLoading = false, 
                    errorMessage = resolvedErrorMessage,
                    errorType = resolvedType,
                    fullNameHasError = defaultType == AuthErrorType.VALIDATION_ERROR,
                    emailHasError = true,
                    passwordHasError = true
                )
            }
        }
    }

    /**
     * Limpia los errores y banderas de resaltado en el estado de la interfaz.
     */
    private fun AuthUiState.withoutErrors(): AuthUiState = copy(
        errorMessage = null,
        errorType = null,
        fullNameHasError = false,
        emailHasError = false,
        passwordHasError = false
    )
}
