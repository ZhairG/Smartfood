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
import com.smartfood.app.feature.inventory.domain.model.SpeechError

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

/**
 * @return El identificador de texto del error de voz, para mostrarlo en el aviso (CA-INV-07-05, CA-INV-07-06).
 */
@StringRes
fun SpeechError.messageRes(): Int = when (this) {
    SpeechError.NO_MATCH -> R.string.search_voice_no_match
    SpeechError.NOT_AVAILABLE -> R.string.search_voice_not_available
    SpeechError.PERMISSION_DENIED -> R.string.search_voice_permission_denied
    SpeechError.NETWORK -> R.string.search_voice_network
}
