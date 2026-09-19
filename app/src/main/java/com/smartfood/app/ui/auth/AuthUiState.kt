package com.smartfood.app.ui.auth

/**
 * Estado completo de la pantalla de Autenticación.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    
    // Estado de carga y resultados
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    
    // Validaciones de contraseña en tiempo real
    val hasMinimumLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasSpecialCharacter: Boolean = false,
    val passwordsMatch: Boolean = false
)
