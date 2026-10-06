package com.smartfood.app.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

/**
 * Nombre: NavTransitions.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Transiciones de empuje (push) de 600 ms con interpolación Ease in and out (RNF-06).
 * Se declaran una sola vez en el `NavHost` en lugar de repetirse en cada destino.
 */
object NavTransitions {

    /** Duración de la animación de empuje en milisegundos. */
    const val PUSH_DURATION_MS = 600

    private val pushSpec = tween<IntOffset>(durationMillis = PUSH_DURATION_MS, easing = FastOutSlowInEasing)

    /** La pantalla nueva entra desde la derecha. */
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(towards = SlideDirection.Left, animationSpec = pushSpec)
    }

    /** La pantalla actual sale hacia la izquierda. */
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(towards = SlideDirection.Left, animationSpec = pushSpec)
    }

    /** Al volver, la pantalla anterior entra desde la izquierda. */
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(towards = SlideDirection.Right, animationSpec = pushSpec)
    }

    /** Al volver, la pantalla actual sale hacia la derecha. */
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(towards = SlideDirection.Right, animationSpec = pushSpec)
    }
}
