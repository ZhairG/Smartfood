package com.smartfood.app.feature.inventory.ui.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
 * Nombre: InventorySearchRoute.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Parte con estado de la búsqueda (spec 006, T035). Conecta el ViewModel con la pantalla y resuelve el
 * permiso de micrófono: pide el permiso al tocar el micrófono y, si ya está concedido, escucha sin volver a
 * preguntar (CA-INV-07-06). Guarda el patrón de `ProductCameraRoute`.
 *
 * @param onNavigateToEditor Abre "Editar Producto" del producto tocado (CA-INV-06-08).
 * @param onBack Vuelve a la pantalla anterior.
 * @param viewModel ViewModel de la búsqueda.
 */
@Composable
fun InventorySearchRoute(
    onNavigateToEditor: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: InventorySearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onEvent(InventorySearchEvent.VoicePermissionResult(granted))
    }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is InventorySearchEffect.NavigateToEditor -> onNavigateToEditor(effect.id)
            InventorySearchEffect.NavigateBack -> onBack()
            InventorySearchEffect.RequestMicPermission ->
                if (context.hasMicrophonePermission()) {
                    viewModel.onEvent(InventorySearchEvent.VoicePermissionResult(granted = true))
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

            InventorySearchEffect.ShowPermissionRationale -> showPermissionRationale = true
        }
    }

    InventorySearchScreen(
        state = state,
        onEvent = viewModel::onEvent,
        showPermissionRationale = showPermissionRationale,
        onOpenSettings = { context.openAppSettings() },
        onDismissPermissionRationale = { showPermissionRationale = false }
    )
}

/** Comprueba si el permiso de micrófono ya está concedido, para no volver a pedirlo (CA-INV-07-06). */
private fun Context.hasMicrophonePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Abre la pantalla de ajustes de la app, donde se concede el permiso de micrófono (CA-INV-07-06). */
private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
