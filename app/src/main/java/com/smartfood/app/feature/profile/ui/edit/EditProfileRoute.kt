package com.smartfood.app.feature.profile.ui.edit

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartfood.app.core.navigation.ObserveEffects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Nombre: EditProfileRoute.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Parte con estado de "Editar información" (spec 008, T007). Conecta el ViewModel con la pantalla, abre
 * la cámara del sistema (`TakePicture`) o el selector de fotos (CA-PER-02-08), borra del teléfono las fotos que ya no
 * se van a guardar e intercepta "atrás" para descartar los cambios (CA-PER-02-13).
 *
 * @param onBack Vuelve al Perfil.
 * @param viewModel ViewModel de la pantalla.
 */
@Composable
fun EditProfileRoute(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Archivo donde escribe la cámara; se guarda para sobrevivir si Android recrea la actividad mientras está abierta.
    var cameraTarget by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val path = cameraTarget ?: return@rememberLauncherForActivityResult
        cameraTarget = null
        if (taken) {
            viewModel.onEvent(EditProfileEvent.PhotoPicked(path))
        } else {
            scope.launch(Dispatchers.IO) { File(path).delete() }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val file = withContext(Dispatchers.IO) { ProfilePhotoFiles.copyFromGallery(context, uri) }
                file?.let { viewModel.onEvent(EditProfileEvent.PhotoPicked(it.path)) }
            }
        }
    }

    val launchCamera = {
        val (file, uri) = ProfilePhotoFiles.newCameraTarget(context)
        cameraTarget = file.path
        cameraLauncher.launch(uri)
    }
    // La app declara CAMERA por la cámara del inventario; con el permiso declarado, Android exige concederlo también
    // para abrir la cámara del sistema. Si se niega, no pasa nada: queda la galería.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    BackHandler { viewModel.onEvent(EditProfileEvent.BackClicked) }

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            EditProfileEffect.OpenCamera -> {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            EditProfileEffect.OpenGallery -> galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            is EditProfileEffect.DeleteLocalPhoto -> scope.launch(Dispatchers.IO) { File(effect.path).delete() }
            EditProfileEffect.NavigateBack -> onBack()
        }
    }

    EditProfileScreen(state = state, onEvent = viewModel::onEvent)
}
