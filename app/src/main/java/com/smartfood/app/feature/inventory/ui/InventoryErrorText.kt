/**
 * Nombre: InventoryErrorText.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Único mapeo de [InventoryError] a textos de `strings_inventory.xml` (spec 006, plan §1 y §10,
 * RNF-INV-08), como `AuthErrorText` en autenticación. La voz agrega aquí su mapeo en el #43.
 */
package com.smartfood.app.feature.inventory.ui

import androidx.annotation.StringRes
import com.smartfood.app.R
import com.smartfood.app.feature.inventory.domain.model.InventoryError

/**
 * @return El identificador de texto del error, para mostrarlo bajo el nombre de la pantalla.
 */
@StringRes
fun InventoryError.messageRes(): Int = when (this) {
    InventoryError.Network -> R.string.error_inventory_network
    InventoryError.PermissionDenied -> R.string.error_inventory_permission
    InventoryError.NotFound -> R.string.error_inventory_not_found
    InventoryError.PhotoUploadFailed -> R.string.error_inventory_photo_upload
    InventoryError.NotAuthenticated -> R.string.error_inventory_permission
    is InventoryError.Validation -> R.string.error_inventory_unknown
    is InventoryError.Unknown -> R.string.error_inventory_unknown
}
