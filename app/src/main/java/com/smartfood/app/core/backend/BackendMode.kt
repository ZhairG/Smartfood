package com.smartfood.app.core.backend

import javax.inject.Inject

/**
 * Nombre: BackendMode.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Con qué backend trabaja la app (spec 012, RF-SPK-01). Lo decide el interruptor `blaze_activo` de
 * Remote Config, no la app: el plan de facturación no se puede consultar desde el cliente. Es Kotlin puro: solo
 * `core/backend/data` conoce Remote Config (RNF-SPK-01).
 */
enum class BackendMode {
    /** Plan Blaze: Cloud Functions y Storage (specs 003, 004, 006 y 008). */
    BLAZE,

    /** Plan Spark: solo Auth, Firestore y Remote Config, con las alternativas gratuitas de la spec 012. */
    FREE,
}

/**
 * Fuente del modo activo (spec 012, HU-SPK-01).
 */
interface BackendModeProvider {
    /** El modo activo. Si el interruptor no se pudo leer, [BackendMode.FREE], que funciona en los dos planes. */
    fun current(): BackendMode

    /** Pide el valor más reciente del interruptor sin esperar la respuesta; si falla, se queda con el último. */
    fun refresh()
}

/**
 * Nombre: GetBackendModeUseCase
 * Descripción: Devuelve el modo activo para los casos de uso que cambian de comportamiento según el plan
 * (spec 012, CA-SPK-04-01).
 */
class GetBackendModeUseCase @Inject constructor(private val provider: BackendModeProvider) {
    operator fun invoke(): BackendMode = provider.current()
}
