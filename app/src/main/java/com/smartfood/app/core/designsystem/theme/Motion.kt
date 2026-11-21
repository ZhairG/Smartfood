package com.smartfood.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * Nombre: Motion.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Tokens de movimiento de la app (spec 007, RF-ANI-01). Las duraciones y curvas salen del prototipo de
 * Figma: disolvencia de pestañas de 100 ms, Smart Animate de 150 ms y empuje, hojas, diálogos y listas de 300 ms.
 * Ninguna pantalla declara una duración o una curva sueltas (CA-ANI-01-01). Compose ya multiplica las duraciones por la
 * escala de animación del sistema, así que con "Quitar animaciones" todo termina en el siguiente cuadro (CA-ANI-01-02).
 */
object SmartFoodMotion {

    /** Pestañas de la barra inferior (Figma: disolvencia de 100 ms). */
    const val SHORT = 100

    /** Foco de los campos, ojo, checklist y errores en línea (Figma: Smart Animate de 150 ms). */
    const val FAST = 150

    /** Navegación, hojas, diálogos, avisos, listas, chips y botones (Figma: 300 ms). */
    const val MEDIUM = 300

    /** "Ease in and out" de Figma, la curva CSS `ease-in-out`. */
    val EaseInOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

    /** "Ease out" de Figma, la curva CSS `ease-out`. */
    val EaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

    /** "Ease in" de Figma, la curva CSS `ease-in`. */
    val EaseIn: Easing = CubicBezierEasing(0.42f, 0f, 1f, 1f)

    /**
     * "Ease in and out back" de Figma: se pasa levemente del destino y regresa. Solo para la escala de los diálogos
     * (spec 007 §8), nunca para el velo ni para colores.
     */
    val EaseInOutBack: Easing = CubicBezierEasing(0.68f, -0.6f, 0.32f, 1.6f)

    /** Empuje entre pantallas (CA-ANI-02-01). */
    fun <T> navigation(): FiniteAnimationSpec<T> = tween(MEDIUM, easing = EaseInOut)

    /** Disolvencia entre pestañas (CA-ANI-02-03). */
    fun <T> tab(): FiniteAnimationSpec<T> = tween(SHORT, easing = EaseOut)

    /** Foco, ojo, checklist y errores en línea (HU-ANI-03, CA-ANI-04-03). */
    fun <T> focus(): FiniteAnimationSpec<T> = tween(FAST, easing = EaseOut)

    /** Hojas, diálogos, avisos, listas y chips (HU-ANI-04 a HU-ANI-06). */
    fun <T> standard(): FiniteAnimationSpec<T> = tween(MEDIUM, easing = EaseInOut)

    /** Cambio de un botón a "cargando" y de vuelta (Figma "BotonCargando", CA-ANI-05-04). */
    fun <T> loading(): FiniteAnimationSpec<T> = tween(MEDIUM, easing = EaseOut)

    /** Escala de un diálogo al aparecer (CA-ANI-04-02). */
    fun <T> emphasizedEnter(): FiniteAnimationSpec<T> = tween(MEDIUM, easing = EaseInOutBack)
}
