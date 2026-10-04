package com.smartfood.app.testutil

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.toAuthError

/**
 * Nombre: ResultExt.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Utilidades de aserción para resultados del dominio.
 */

/** Error de negocio de un `Result` fallido, o `null` si fue exitoso. */
val Result<*>.authError: AuthError?
    get() = exceptionOrNull()?.toAuthError()
