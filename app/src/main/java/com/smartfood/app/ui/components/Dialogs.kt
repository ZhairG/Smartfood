package com.smartfood.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartfood.app.R
import com.smartfood.app.ui.theme.SmartfoodTheme
import com.smartfood.app.utils.figmaShadow

/**
 * Nombre: Dialogs.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componentes modales reutilizables para el flujo de la aplicación.
 */

/**
 * Diálogo modal reutilizable para notificar estados de desconexión a la red.
 * Ofrece al usuario un botón primario para reintentar la conexión.
 *
 * @param isReconnecting Indica si se encuentra en proceso un reintento de reconexión.
 * @param onReconnect Callback invocado al presionar el botón de reintento.
 * @param onDismissRequest Callback invocado al intentar cerrar el diálogo.
 */
@Composable
fun SmartFoodOfflineDialog(
    isReconnecting: Boolean = false,
    onReconnect: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        SmartFoodOfflineDialogContent(
            isReconnecting = isReconnecting,
            onReconnect = onReconnect
        )
    }
}

/**
 * Contenido del diálogo de desconexión desvinculado del contenedor [Dialog] para permitir
 * la previsualización directa en las vistas previas de Android Studio.
 *
 * @param isReconnecting Indica si se encuentra en proceso un reintento de reconexión.
 * @param onReconnect Callback invocado al presionar el botón de reintento.
 * @param modifier Modificador opcional.
 */
@Composable
private fun SmartFoodOfflineDialogContent(
    isReconnecting: Boolean,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialogShape = RoundedCornerShape(45.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .figmaShadow(
                color = Color(0x80000000), // #000000 con 50% de opacidad (0x80)
                borderRadius = 45.dp,
                blurRadius = 4.dp,
                offsetY = 4.dp
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = dialogShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SignalWifiOff,
                    contentDescription = stringResource(id = R.string.offline_dialog_title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.offline_dialog_title),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 29.dp, end = 29.dp)
            ) {
                SmartFoodPrimaryButton(
                    text = if (isReconnecting) stringResource(id = R.string.offline_dialog_connecting) 
                           else stringResource(id = R.string.offline_dialog_button),
                    onClick = onReconnect
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF556B2F)
@Composable
private fun SmartFoodOfflineDialogPreview() {
    SmartfoodTheme {
        SmartFoodOfflineDialogContent(
            isReconnecting = false,
            onReconnect = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF556B2F)
@Composable
private fun SmartFoodOfflineDialogConnectingPreview() {
    SmartfoodTheme {
        SmartFoodOfflineDialogContent(
            isReconnecting = true,
            onReconnect = {}
        )
    }
}
