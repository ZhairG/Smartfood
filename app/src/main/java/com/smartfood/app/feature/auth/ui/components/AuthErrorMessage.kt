package com.smartfood.app.feature.auth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme

/**
 * Nombre: AuthErrorMessage.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componente animado para la notificación de errores en los formularios de autenticación.
 * Optimizado con [graphicsLayer] para renderizar las animaciones de aparición en la capa de dibujo GPU.
 *
 * @param message Texto del mensaje de error a mostrar. Si es nulo o vacío, se oculta animadamente.
 * @param modifier Modificador opcional para personalizar el diseño.
 */
@Composable
fun AuthErrorMessage(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        if (!message.isNullOrBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        clip = false
                    }
                    .padding(vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthErrorMessagePreview() {
    SmartfoodTheme {
        AuthErrorMessage(message = "Correo o contraseña incorrectos")
    }
}
