package com.smartfood.app.core.navigation

import kotlinx.serialization.Serializable

/**
 * Nombre: Routes.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Definición de rutas type-safe respaldadas por [ScreenRoute] para la navegación con Kotlin Serialization.
 */
@Serializable
sealed interface Route {

    /** Devuelve el identificador de enumeración [ScreenRoute] asociado a esta ruta. */
    val screenRoute: ScreenRoute

    // --- Grafos Anidados ---
    /** Grafo de navegación para las pantallas de autenticación. */
    @Serializable
    data object AuthGraph : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.AUTH_GRAPH
    }

    /** Grafo de navegación para las pantallas principales de la aplicación. */
    @Serializable
    data object MainGraph : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.MAIN_GRAPH
    }

    // --- Pantallas Individuales ---
    /** Pantalla inicial de Splash. */
    @Serializable
    data object Splash : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.SPLASH
    }

    /** Pantalla de inicio de sesión. */
    @Serializable
    data object Login : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.LOGIN
    }

    /** Pantalla de registro de usuarios. */
    @Serializable
    data object Register : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.REGISTER
    }

    /** Pantalla de confirmación de registro exitoso. */
    @Serializable
    data object RegisterSuccess : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.REGISTER_SUCCESS
    }

    /** Pantalla de recuperación de contraseña. */
    @Serializable
    data object PasswordRecovery : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.PASSWORD_RECOVERY
    }

    /** Pantalla de confirmación de restablecimiento exitoso de contraseña. */
    @Serializable
    data object PasswordRecoverySuccess : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.PASSWORD_RECOVERY_SUCCESS
    }

    /** Términos y condiciones (spec 005). */
    @Serializable
    data object TermsOfService : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.TERMS_OF_SERVICE
    }

    /** Política de privacidad (spec 005). */
    @Serializable
    data object PrivacyPolicy : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.PRIVACY_POLICY
    }

    /** Pestaña Perfil: saludo y cierre de sesión (antes Home; spec 006, CA-INV-09-03). */
    @Serializable
    data object Profile : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.PROFILE
    }
}
