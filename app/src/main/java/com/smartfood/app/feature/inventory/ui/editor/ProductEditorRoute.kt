/**
 * Nombre: ProductEditorRoute.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Parte con estado del editor de producto (spec 006, T032). Conecta el ViewModel con la pantalla, recibe
 * la foto de la cámara por el `savedStateHandle` (CA-INV-05-02), intercepta "atrás" para descartar los cambios
 * (CA-INV-03-04) y borra del teléfono las fotos que ya no se van a guardar.
 *
 * @param savedStateHandle `savedStateHandle` del destino del editor, donde la cámara deja la foto.
 * @param onNavigateToCamera Abre el visor de la cámara.
 * @param onBack Vuelve a la pantalla anterior.
 * @param viewModel ViewModel del editor.
 */
package com.smartfood.app.feature.inventory.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
    viewModel: ProductEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
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
            is ProductEditorEffect.DeleteLocalPhoto -> scope.launch(Dispatchers.IO) { File(effect.path).delete() }
            // Lo atiende la T039 (#44).
            ProductEditorEffect.RequestNotificationPermission -> Unit
        }
    }

    ProductEditorScreen(state = state, onEvent = viewModel::onEvent)
}
