package com.smartfood.app.feature.recipes.domain.agent

import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Nombre: SearchDeadline.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Límite de la búsqueda de [MealPlanner] (spec 009, enmienda del 2026-11-20, T040, CA-RIA-02-12 y 02-13).
 * [MealPlanner] lo consulta en cada nodo; cuando [expired] devuelve `true`, deja de buscar y se queda con el mejor plan
 * encontrado hasta ese momento. Es Kotlin puro: la app lo arma con un reloj y las pruebas con un contador o un
 * `TestTimeSource`.
 */
fun interface SearchDeadline {

    /** `true` si ya no se debe seguir buscando. */
    fun expired(): Boolean

    companion object {
        /** Sin límite: la búsqueda revisa todo el árbol y el plan es el óptimo (CA-RIA-02-04). */
        val NONE = SearchDeadline { false }

        /** Vence cuando pasa [limit] desde ahora, medido con [timeSource]. */
        fun after(limit: Duration, timeSource: TimeSource = TimeSource.Monotonic): SearchDeadline {
            val end = timeSource.markNow() + limit
            return SearchDeadline { end.hasPassedNow() }
        }
    }
}
