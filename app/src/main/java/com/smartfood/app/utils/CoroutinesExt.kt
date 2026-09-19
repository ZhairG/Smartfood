package com.smartfood.app.utils

import kotlin.coroutines.cancellation.CancellationException

/**
 * Extensión de runCatching segura para Corrutinas.
 * A diferencia del runCatching estándar, esta función relanza la [CancellationException]
 * para no romper la cancelación estructurada de las corrutinas de Kotlin.
 */
inline fun <T> safeRunCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
