package com.smartfood.app.core.navigation

import androidx.annotation.Keep

/**
 * Nombre: ScreenRoute.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Enumeración independiente que identifica unívocamente cada pantalla y grafo del sistema de navegación.
 *
 * @property routeName Identificador en cadena de texto de la ruta.
 */
@Keep // Lo referencian las rutas @Serializable; evita que R8 lo ofusque en release.
enum class ScreenRoute(val routeName: String) {
    AUTH_GRAPH("auth_graph"),
    MAIN_GRAPH("main_graph"),
    SPLASH("splash"),
    LOGIN("login"),
    REGISTER("register"),
    REGISTER_SUCCESS("register_success"),
    PASSWORD_RECOVERY("password_recovery"),
    PASSWORD_RECOVERY_SUCCESS("password_recovery_success"),
    TERMS_OF_SERVICE("terms_of_service"),
    PRIVACY_POLICY("privacy_policy"),
    HOME("home")
}
