package com.smartfood.app.testutil

import com.smartfood.app.feature.auth.domain.model.AuthError
import com.smartfood.app.feature.auth.domain.model.toAuthError
import com.smartfood.app.feature.inventory.domain.model.InventoryError
import com.smartfood.app.feature.inventory.domain.model.toInventoryError

/**
 * Nombre: ResultExt.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Utilidades de aserción para resultados del dominio.
 */

/** Error de negocio de un `Result` fallido, o `null` si fue exitoso. */
val Result<*>.authError: AuthError?
    get() = exceptionOrNull()?.toAuthError()

/** Error del inventario de un `Result` fallido, o `null` si fue exitoso (spec 006). */
val Result<*>.inventoryError: InventoryError?
    get() = exceptionOrNull()?.toInventoryError()
