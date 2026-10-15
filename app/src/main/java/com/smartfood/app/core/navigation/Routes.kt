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

    /** Pestaña "Mi Inventario": inicio de la sesión (spec 006, CA-INV-09-01). */
    @Serializable
    data object Inventory : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.INVENTORY
    }

    /** Búsqueda en el inventario (spec 006, HU-INV-06). */
    @Serializable
    data object InventorySearch : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.INVENTORY_SEARCH
    }

    /**
     * Editor de producto (spec 006, HU-INV-02 y 03).
     *
     * @property itemId Producto a editar, o `null` para añadir uno nuevo.
     */
    @Serializable
    data class ProductEditor(val itemId: String? = null) : Route {
        override val screenRoute: ScreenRoute get() = ScreenRoute.PRODUCT_EDITOR
    }

    /** Cámara para la foto del producto (spec 006, HU-INV-05). */
    @Serializable
    data object ProductCamera : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.PRODUCT_CAMERA
    }

    /** Pestaña Perfil: saludo y cierre de sesión (antes Home; spec 006, CA-INV-09-03). */
    @Serializable
    data object Profile : Route {
        override val screenRoute: ScreenRoute = ScreenRoute.PROFILE
    }
}
