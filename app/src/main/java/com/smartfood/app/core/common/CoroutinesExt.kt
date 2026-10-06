package com.smartfood.app.core.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * <build>
 * <b>Nombre:</b> CoroutinesExt.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 19/09/2026<br>
 * <b>Descripción:</b> Funciones de extensión utilitarias para el manejo seguro de corrutinas en Kotlin.<br>
 * </build>
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
