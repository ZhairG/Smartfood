package com.smartfood.app.core.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Nombre: CoroutinesExt.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Funciones de extensión utilitarias para el manejo seguro de corrutinas en Kotlin.
 */

/**
 * Extensión de `runCatching` adaptada para Corrutinas.
 * Re-lanza [CancellationException] para respetar la cancelación estructurada del hilo/job de corrutina.
 *
 * @param T Tipo de dato de retorno del bloque.
 * @param block Bloque de código a ejecutar.
 * @return [Result] con el valor resultante o la excepción capturada.
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
