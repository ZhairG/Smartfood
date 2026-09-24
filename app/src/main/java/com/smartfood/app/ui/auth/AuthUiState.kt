package com.smartfood.app.ui.auth

import androidx.compose.runtime.Immutable

/**
 * Nombre: AuthUiState.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Estado completo y UDF (Unidirectional Data Flow) para la pantalla de Autenticación.
 * 
 * La anotación @Immutable asegura a Jetpack Compose que este objeto y sus propiedades
 * nunca cambiarán después de ser instanciados, optimizando dramáticamente y evitando
 * recomposiciones innecesarias de la UI.
 */
@Immutable
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val rememberMe: Boolean = true, // Elevación de estado: Sacado de la UI al ViewModel
    
    // Estado de carga y resultados
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val errorType: AuthErrorType? = null, // Usando Enums para tipado fuerte
    val isOffline: Boolean = false,

    // Estado de error por campo (borde rojo en los campos de texto)
    val emailHasError: Boolean = false,
    val passwordHasError: Boolean = false,
    
    // Validaciones de contraseña en tiempo real
    val hasMinimumLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasSpecialCharacter: Boolean = false,
    val passwordsMatch: Boolean = false
)
