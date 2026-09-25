package com.smartfood.app.navigation

import kotlinx.serialization.Serializable

/**
 * Nombre: Routes.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Definición de rutas type-safe para la navegación con Kotlin Serialization.
 */
@Serializable
sealed interface Route {

    // --- Grafos Anidados ---
    /** Grafo de navegación para las pantallas de autenticación. */
    @Serializable
    data object AuthGraph : Route

    /** Grafo de navegación para las pantallas principales de la aplicación. */
    @Serializable
    data object MainGraph : Route


    // --- Pantallas Individuales ---
    /** Pantalla inicial de Splash. */
    @Serializable
    data object Splash : Route

    /** Pantalla de inicio de sesión. */
    @Serializable
    data object Login : Route

    /** Pantalla de aviso de falta de conexión. */
    @Serializable
    data object OfflineConnection : Route

    /** Pantalla de registro de usuarios. */
    @Serializable
    data object Register : Route

    /** Pantalla de confirmación de registro exitoso. */
    @Serializable
    data object RegisterSuccess : Route

    /** Pantalla de recuperación de contraseña. */
    @Serializable
    data object PasswordRecovery : Route

    /** Pantalla principal (Home) de la aplicación. */
    @Serializable
    data object Home : Route
}
