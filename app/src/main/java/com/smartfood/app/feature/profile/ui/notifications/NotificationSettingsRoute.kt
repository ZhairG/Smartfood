package com.smartfood.app.feature.profile.ui.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartfood.app.core.navigation.ObserveEffects

/**
 * Nombre: NotificationSettingsRoute.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Parte con estado de "Notificaciones" (spec 008, T017). Conecta el ViewModel con la pantalla y se encarga
 * del permiso de notificaciones de Android 13+: al encender un interruptor sin permiso lo pide; si se niega, avisa al
 * ViewModel para mostrar el aviso con "Abrir ajustes" (CA-PER-04-06). En Android 7 (API 24), donde no existe
 * `ACTION_APP_NOTIFICATION_SETTINGS`, abre los detalles de la app.
 *
 * @param onBack Vuelve al Perfil.
 * @param viewModel ViewModel de la pantalla.
 */
@Composable
fun NotificationSettingsRoute(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Interruptor que espera la respuesta del permiso; `null` si no hay ninguno pendiente.
    var pendingSetting by remember { mutableStateOf<NotificationSetting?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val setting = pendingSetting
        pendingSetting = null
        if (granted && setting != null) {
            viewModel.onEvent(NotificationSettingsEvent.ToggleRequested(setting, enabled = true))
        } else if (!granted) {
            viewModel.onEvent(NotificationSettingsEvent.PermissionDenied)
        }
    }

    BackHandler { viewModel.onEvent(NotificationSettingsEvent.BackClicked) }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            NotificationSettingsEffect.NavigateBack -> onBack()
            NotificationSettingsEffect.OpenAppSettings -> context.startActivity(notificationSettingsIntent(context))
        }
    }

    NotificationSettingsScreen(
        state = state,
        onEvent = { event ->
            if (event is NotificationSettingsEvent.ToggleRequested && event.enabled && needsNotificationPermission(context)) {
                pendingSetting = event.setting
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onEvent(event)
            }
        }
    )
}

/** En API 33+ la app necesita el permiso `POST_NOTIFICATIONS` para poder publicar avisos (CA-PER-04-06). */
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** Ajustes de notificaciones de la app; en API 24 y 25 no existe el atajo y se abren los detalles de la app. */
private fun notificationSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
