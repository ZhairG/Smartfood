package com.smartfood.app.navigation

import kotlinx.serialization.Serializable

/**
  * Definición Type-Safe de las rutas de la aplicación utilizando Kotlin Serialization.
 */
@Serializable
sealed interface Route {

    // --- Grafos Anidados ---
    @Serializable
    data object AuthGraph : Route

    @Serializable
    data object MainGraph : Route


    // --- Pantallas Individuales ---
    @Serializable
    data object Splash : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object OfflineConnection : Route

    @Serializable
    data object Register : Route

    @Serializable
    data object PasswordRecovery : Route

    @Serializable
    data object Home : Route
}
