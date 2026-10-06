package com.smartfood.app.core.navigation

import androidx.annotation.Keep

/**
 * <build>
 * <b>Nombre:</b> ScreenRoute.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 28/09/2026<br>
 * <b>Descripción:</b> Enumeración independiente que identifica unívocamente cada pantalla y grafo del sistema de<br>
 * navegación.<br>
 * </build>
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
    HOME("home")
}
