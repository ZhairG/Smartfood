package com.smartfood.app.feature.inventory.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

/**
 * Nombre: ProductCameraRoute.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Parte con estado de la cámara (spec 006, T033): pide el permiso al abrir (CA-INV-05-03), lo vuelve a
 * comprobar al regresar de los ajustes, conecta CameraX al ciclo de vida y captura la foto. No tiene ViewModel
 * (plan §1): solo devuelve la ruta del archivo; guardarlo es decisión del editor.
 *
 * @param onPhotoCaptured Recibe la ruta de la foto guardada.
 * @param onBack Vuelve sin foto.
 */
@Composable
fun ProductCameraRoute(onPhotoCaptured: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var permission by remember {
        mutableStateOf(if (context.hasCameraPermission()) CameraPermission.GRANTED else CameraPermission.UNKNOWN)
    }
    var isCapturing by remember { mutableStateOf(false) }
    var captureFailed by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = if (granted) CameraPermission.GRANTED else CameraPermission.DENIED
    }
    LaunchedEffect(Unit) {
        if (permission == CameraPermission.UNKNOWN) launcher.launch(Manifest.permission.CAMERA)
    }
    // Al volver de los ajustes con el permiso concedido, aparece el visor.
    LifecycleResumeEffect(Unit) {
        if (context.hasCameraPermission()) permission = CameraPermission.GRANTED
        onPauseOrDispose { }
    }

    val controller = remember {
        LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) }
    }
    LaunchedEffect(permission) {
        if (permission == CameraPermission.GRANTED) controller.bindToLifecycle(lifecycleOwner)
    }

    ProductCameraScreen(
        permission = permission,
        preview = {
            AndroidView(
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        this.controller = controller
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        },
        isCapturing = isCapturing,
        captureFailed = captureFailed,
        onCapture = {
            scope.launch {
                isCapturing = true
                captureFailed = false
                controller.takePhoto(context)
                    .onSuccess { onPhotoCaptured(it.path) }
                    .onFailure { captureFailed = true }
                isCapturing = false
            }
        },
        onBack = onBack,
        onOpenSettings = { context.openAppSettings() }
    )
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** Abre la pantalla de ajustes de la app, donde se concede el permiso de cámara. */
private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
