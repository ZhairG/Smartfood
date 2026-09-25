package com.smartfood.app.ui.auth

import androidx.compose.runtime.Immutable

/**
 * Nombre: AuthUiState.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Estado inmutable completo (Unidirectional Data Flow) de las pantallas de Autenticación.
 *
 * La anotación `@Immutable` optimiza las recomposiciones en Jetpack Compose al garantizar
 * la inmutabilidad de sus propiedades.
 *
 * @property fullName Nombre completo ingresado por el usuario en el registro.
 * @property email Correo electrónico ingresado por el usuario.
 * @property password Contraseña ingresada por el usuario.
 * @property confirmPassword Confirmación de contraseña ingresada por el usuario.
 * @property rememberMe Estado de la casilla "Recordarme".
 * @property acceptedTerms Estado de aceptación de los Términos y Condiciones.
 * @property isLoading Indica si hay una operación asíncrona en curso.
 * @property isSuccess Indica si la operación de autenticación finalizó con éxito.
 * @property errorMessage Mensaje de error formateado o reportado.
 * @property errorType Clasificación en enum del error ocurrido.
 * @property isOffline Indica si la aplicación se encuentra sin conexión a la red.
 * @property fullNameHasError Indica si el campo de nombre completo debe mostrar borde de error.
 * @property emailHasError Indica si el campo de correo debe mostrar borde de error.
 * @property passwordHasError Indica si el campo de contraseña debe mostrar borde de error.
 * @property hasMinimumLength Cumplimiento de longitud mínima en la contraseña.
 * @property hasUppercase Cumplimiento de al menos una letra mayúscula en la contraseña.
 * @property hasSpecialCharacter Cumplimiento de al menos un carácter especial en la contraseña.
 * @property passwordsMatch Indica si las contraseñas coinciden en el registro.
 */
@Immutable
data class AuthUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val rememberMe: Boolean = true,
    val acceptedTerms: Boolean = true,
    
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val errorType: AuthErrorType? = null,
    val isOffline: Boolean = false,

    val fullNameHasError: Boolean = false,
    val emailHasError: Boolean = false,
    val passwordHasError: Boolean = false,
    
    val hasMinimumLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasSpecialCharacter: Boolean = false,
    val passwordsMatch: Boolean = false
)
