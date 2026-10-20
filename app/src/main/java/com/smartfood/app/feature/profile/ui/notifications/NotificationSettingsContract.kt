package com.smartfood.app.feature.profile.ui.notifications

import androidx.compose.runtime.Immutable
import com.smartfood.app.feature.profile.domain.model.NotificationSettings

/**
 * Nombre: NotificationSettingsContract.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Contrato de "Notificaciones" (spec 008, HU-PER-04, Figma `593:2079`): estado, eventos y efectos. Cada
 * interruptor se guarda al tocarlo, sin botón "Guardar" (CA-PER-04-02). Si el permiso de Android 13+ está negado, el
 * aviso [NotificationSettingsDialog.PermissionDenied] explica cómo activarlo (CA-PER-04-06).
 */

/** Los tres avisos que se pueden encender o apagar (CA-PER-04-01). */
enum class NotificationSetting {
    /** Productos por vencer: lo usa el aviso diario del inventario (CA-PER-04-03). */
    EXPIRING_PRODUCTS,

    /** Recetas sugeridas: solo se guarda (CA-PER-04-05). */
    SUGGESTED_RECIPES,

    /** Inventario vacío: lo usa el aviso diario cuando no hay productos (CA-PER-04-04). */
    EMPTY_INVENTORY
}

/** Aviso abierto sobre la pantalla. */
sealed interface NotificationSettingsDialog {
    /** El permiso de notificaciones quedó negado (CA-PER-04-06). */
    data object PermissionDenied : NotificationSettingsDialog
}

/**
 * Estado de "Notificaciones".
 *
 * @property isLoading `true` hasta leer los ajustes guardados.
 * @property settings Ajustes vigentes; todos vienen encendidos la primera vez (CA-PER-04-01).
 * @property dialog Aviso abierto, o `null`.
 */
@Immutable
data class NotificationSettingsUiState(
    val isLoading: Boolean = true,
    val settings: NotificationSettings = NotificationSettings(),
    val dialog: NotificationSettingsDialog? = null
) {
    /** Valor del interruptor de [setting]. */
    fun isOn(setting: NotificationSetting): Boolean = when (setting) {
        NotificationSetting.EXPIRING_PRODUCTS -> settings.expiringProducts
        NotificationSetting.SUGGESTED_RECIPES -> settings.suggestedRecipes
        NotificationSetting.EMPTY_INVENTORY -> settings.emptyInventory
    }
}

/** Lo que el usuario hace en "Notificaciones". */
sealed interface NotificationSettingsEvent {
    /** Toca un interruptor. El Route lo intercepta si hace falta el permiso (CA-PER-04-06). */
    data class ToggleRequested(val setting: NotificationSetting, val enabled: Boolean) : NotificationSettingsEvent

    /** El permiso de notificaciones quedó negado: se muestra el aviso (CA-PER-04-06). */
    data object PermissionDenied : NotificationSettingsEvent

    /** "Abrir ajustes" del aviso: lleva a los ajustes de notificaciones del teléfono. */
    data object OpenSettingsClicked : NotificationSettingsEvent

    data object DialogDismissed : NotificationSettingsEvent
    data object BackClicked : NotificationSettingsEvent
}

/** Efectos de un solo uso. */
sealed interface NotificationSettingsEffect {
    /** Vuelve al Perfil. */
    data object NavigateBack : NotificationSettingsEffect

    /** Abre los ajustes de notificaciones de la app. */
    data object OpenAppSettings : NotificationSettingsEffect
}
