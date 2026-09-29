package com.smartfood.app.navigation

/**
 * Nombre: ScreenRoute.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Enumeración independiente que identifica unívocamente cada pantalla y grafo del sistema de navegación.
 *
 * @property routeName Identificador en cadena de texto de la ruta.
 */
enum class ScreenRoute(val routeName: String) {
    AUTH_GRAPH("auth_graph"),
    MAIN_GRAPH("main_graph"),
    SPLASH("splash"),
    LOGIN("login"),
    REGISTER("register"),
    REGISTER_SUCCESS("register_success"),
    PASSWORD_RECOVERY("password_recovery"),
    OFFLINE_CONNECTION("offline_connection"),
    HOME("home")
}
