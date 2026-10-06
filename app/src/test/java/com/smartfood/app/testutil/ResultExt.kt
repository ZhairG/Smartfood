package com.smartfood.app.testutil

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.toAuthError

/**
 * <build>
 * <b>Nombre:</b> ResultExt.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Utilidades de aserción para resultados del dominio.<br>
 * </build>
 */

/** Error de negocio de un `Result` fallido, o `null` si fue exitoso. */
val Result<*>.authError: AuthError?
    get() = exceptionOrNull()?.toAuthError()
