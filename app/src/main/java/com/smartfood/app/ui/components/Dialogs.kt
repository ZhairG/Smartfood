package com.smartfood.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartfood.app.R
import com.smartfood.app.ui.theme.SmartfoodTheme

/**
 * Dialogo modal reutilizable que notifica al usuario la falta de conexión
 * y ofrece un botón para reintentar la acción.
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(45.dp) // Curvatura de Figma
                )
                .padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.SignalWifiOff,
                contentDescription = stringResource(id = R.string.offline_dialog_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.offline_dialog_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            SmartFoodPrimaryButton(
                text = if (isReconnecting) stringResource(id = R.string.offline_dialog_connecting) 
                       else stringResource(id = R.string.offline_dialog_button),
                onClick = onReconnect,
                enabled = !isReconnecting
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodOfflineDialogPreview() {
    SmartfoodTheme {
        SmartFoodOfflineDialog(
            isReconnecting = false,
            onReconnect = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodOfflineDialogConnectingPreview() {
    SmartfoodTheme {
        SmartFoodOfflineDialog(
            isReconnecting = true,
            onReconnect = {}
        )
    }
}
