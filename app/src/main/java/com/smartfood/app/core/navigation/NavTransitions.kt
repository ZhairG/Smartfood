package com.smartfood.app.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.smartfood.app.core.designsystem.theme.SmartFoodMotion

/**
 * Nombre: NavTransitions.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026 (300 ms y pestañas en noviembre de 2026, spec 007)
 * Descripción: Transiciones de navegación del prototipo (spec 007, HU-ANI-02): empuje de 300 ms con curva ease-in-out
 * entre pantallas y disolvencia de 100 ms, ease-out, entre las pestañas de la barra inferior. Se declaran una sola vez
 * en el `NavHost` en lugar de repetirse en cada destino, y sus duraciones salen de [SmartFoodMotion].
 */
object NavTransitions {

    /** Duración de la animación de empuje en milisegundos (Figma: 300 ms; spec 001 RNF-06 enmendado). */
    const val PUSH_DURATION_MS = SmartFoodMotion.MEDIUM

    /** Duración de la disolvencia entre pestañas en milisegundos (Figma: 100 ms, CA-ANI-02-03). */
    const val TAB_DURATION_MS = SmartFoodMotion.SHORT

    /** La pantalla nueva entra desde la derecha. */
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(towards = SlideDirection.Left, animationSpec = SmartFoodMotion.navigation())
    }

    /** La pantalla actual sale hacia la izquierda. */
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(towards = SlideDirection.Left, animationSpec = SmartFoodMotion.navigation())
    }

    /** Al volver, la pantalla anterior entra desde la izquierda. */
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(towards = SlideDirection.Right, animationSpec = SmartFoodMotion.navigation())
    }

    /** Al volver, la pantalla actual sale hacia la derecha. */
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(towards = SlideDirection.Right, animationSpec = SmartFoodMotion.navigation())
    }

    // Entre pestañas, disolvencia corta sin empuje (CA-ANI-02-03); hacia o desde una pantalla interna se usa el empuje.
    private val AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs: Boolean
        get() = initialState.destination.isTab() && targetState.destination.isTab()

    /** Entrada de una pestaña. */
    val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (betweenTabs) fadeIn(SmartFoodMotion.tab()) else enter()
    }

    /** Salida de una pestaña. */
    val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (betweenTabs) fadeOut(SmartFoodMotion.tab()) else exit()
    }

    /** Entrada de una pestaña al volver. */
    val tabPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (betweenTabs) fadeIn(SmartFoodMotion.tab()) else popEnter()
    }

    /** Salida de una pestaña al volver. */
    val tabPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (betweenTabs) fadeOut(SmartFoodMotion.tab()) else popExit()
    }
}

/** Si el destino es una de las cuatro pestañas de la barra inferior (Guardadas incluida, CA-ANI-02-03). */
private fun NavDestination.isTab(): Boolean =
    hasRoute<Route.Inventory>() || hasRoute<Route.Recipes>() || hasRoute<Route.Saved>() || hasRoute<Route.Profile>()
