/**
 * Nombre: ProductEditorRoute.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Parte con estado del editor de producto (spec 006, T032). Conecta el ViewModel con la pantalla, recibe
 * la foto de la cámara por el `savedStateHandle` (CA-INV-05-02), intercepta "atrás" para descartar los cambios
 * (CA-INV-03-04) y borra del teléfono las fotos que ya no se van a guardar. También pide el permiso de
 * notificaciones la primera vez que se guarda un producto (CA-INV-08-05).
 *
 * @param savedStateHandle `savedStateHandle` del destino del editor, donde la cámara deja la foto.
 * @param onNavigateToCamera Abre el visor de la cámara.
 * @param onBack Vuelve a la pantalla anterior.
 * @param onBackWithResult Vuelve a la lista con el resultado para mostrarlo encima.
 * @param viewModel ViewModel del editor.
 */
package com.smartfood.app.feature.inventory.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartfood.app.core.navigation.ObserveEffects
import com.smartfood.app.feature.inventory.ui.camera.CAPTURED_PHOTO_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProductEditorRoute(
    savedStateHandle: SavedStateHandle,
    onNavigateToCamera: () -> Unit,
    onBack: () -> Unit,
    onBackWithResult: (EditorResult) -> Unit,
    viewModel: ProductEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // El permiso de notificaciones se pide una sola vez, tras el primer guardado (CA-INV-08-05). Si el usuario lo
    // niega, el inventario funciona igual y no se le vuelve a pedir (RNF-INV-07).
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Sin acción: negar el permiso no cambia nada en la app. */ }
    val capturedPhoto by savedStateHandle.getStateFlow<String?>(CAPTURED_PHOTO_KEY, null)
        .collectAsStateWithLifecycle()

    // La foto llega una sola vez: se entrega al ViewModel y se limpia para no repetirla al recomponer.
    LaunchedEffect(capturedPhoto) {
        capturedPhoto?.let { path ->
            viewModel.onEvent(ProductEditorEvent.PhotoCaptured(path))
            savedStateHandle[CAPTURED_PHOTO_KEY] = null
        }
    }

    BackHandler { viewModel.onEvent(ProductEditorEvent.BackClicked) }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            ProductEditorEffect.NavigateToCamera -> onNavigateToCamera()
            ProductEditorEffect.NavigateBack -> onBack()
            is ProductEditorEffect.NavigateBackWithResult -> onBackWithResult(effect.result)
            is ProductEditorEffect.DeleteLocalPhoto -> scope.launch(Dispatchers.IO) { File(effect.path).delete() }
            ProductEditorEffect.RequestNotificationPermission -> {
                // El permiso existe desde Android 13; en versiones anteriores no hay nada que pedir (CA-INV-08-05).
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val alreadyGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!alreadyGranted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    ProductEditorScreen(state = state, onEvent = viewModel::onEvent)
}
