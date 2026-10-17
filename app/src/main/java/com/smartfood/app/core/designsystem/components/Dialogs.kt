package com.smartfood.app.core.designsystem.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.ShadowColorModal
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.figmaShadow

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
            dismissOnClickOutside = false,
            // Figma (nodo 585:4441): la tarjeta ocupa casi todo el ancho, a 6 dp de los bordes.
            usePlatformDefaultWidth = false
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
    // Radio propio de este diálogo (45 dp, Figma `700:1913`), no el de `panelRadius` (50 dp, diálogos de inventario).
    val dialogShape = RoundedCornerShape(45.dp)

    Box(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp)
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
        // Medidas del frame "Mensaje_Claro" de Figma (nodo 700:1913): ícono a 6 dp del borde, título justo debajo
        // y botón a 111 dp del borde superior. El ícono (72 dp) también es el suyo, no `iconDialogSize` (100 dp).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_wifi_off),
                contentDescription = stringResource(id = R.string.offline_dialog_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = stringResource(id = R.string.offline_dialog_title),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(5.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 29.dp, end = 29.dp)
            ) {
                // Figma 281:333: mientras reconecta, el botón sigue naranja y anima "Conectando…".
                SmartFoodPrimaryButton(
                    text = stringResource(id = R.string.offline_dialog_button),
                    onClick = onReconnect,
                    isLoading = isReconnecting,
                    loadingText = stringResource(id = R.string.offline_dialog_connecting)
                )
            }
        }
    }
}

/**
 * Muestra una confirmación para una acción destructiva, como eliminar un producto o su imagen.
 *
 * Figma: "Eliminar Producto" `476:3809` y "Eliminar Imagen Inventario" `476:4021` (spec 006, CA-INV-04-01 y 05-04).
 *
 * @param icon Contenido que dibuja el ícono de la acción a confirmar.
 * @param question Pregunta mostrada al usuario.
 * @param confirmText Texto del botón destructivo.
 * @param dismissText Texto del botón para cancelar.
 * @param onConfirm Callback invocado al confirmar la acción.
 * @param onDismiss Callback invocado al cancelar o cerrar el diálogo.
 * @param modifier Modificador opcional para el contenido del diálogo.
 */
@Composable
fun SmartFoodConfirmDialog(
    icon: @Composable () -> Unit,
    question: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        BlurBehindDialog()
        SmartFoodConfirmDialogContent(
            icon = icon,
            question = question,
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}

/** Muestra el contenido de [SmartFoodConfirmDialog] para reutilizarlo en vistas previas. */
@Composable
private fun SmartFoodConfirmDialogContent(
    icon: @Composable () -> Unit,
    question: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val metrics = LocalFormMetrics.current
    val dialogShape = RoundedCornerShape(spacing.panelRadius)

    Column(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .padding(horizontal = spacing.extraSmall, vertical = spacing.small)
            .figmaShadow(
                color = ShadowColorModal,
                borderRadius = spacing.panelRadius,
                blurRadius = spacing.extraSmall,
                offsetY = spacing.extraSmall
            )
            .background(MaterialTheme.colorScheme.surfaceVariant, dialogShape)
            .padding(
                horizontal = metrics.formHorizontalPadding,
                vertical = metrics.sectionSpacing
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing)
    ) {
        Box(
            modifier = Modifier
                .size(spacing.iconDialogSize)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Text(
            text = question,
            color = MaterialTheme.colorScheme.primary,
            // Figma 476:3814: pregunta en Poppins Bold de 20 sp.
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        // Figma 476:3815 y 476:3817: los dos botones del Figma son el mismo botón primario, el de confirmar en su
        // variante destructiva (`destructive`, `error`).
        SmartFoodPrimaryButton(
            text = confirmText,
            onClick = onConfirm,
            destructive = true
        )
        SmartFoodPrimaryButton(
            text = dismissText,
            onClick = onDismiss
        )
    }
}

/**
 * Muestra el resultado de una acción completada para informar al usuario.
 *
 * Figma: "Guardar Producto" `476:3926` (spec 006, CA-INV-02-06 y 03-02).
 *
 * El diálogo **no** se cierra solo: ni con atrás ni tocando fuera. Lo quita de la composición quien lo muestra
 * (el editor lo retira cuando navega de vuelta a la lista), junto con la acción que lo abrió.
 *
 * @param icon Contenido que dibuja el ícono del resultado.
 * @param title Título del resultado.
 * @param body Mensaje adicional opcional.
 * @param destructive `true` para un resultado de borrado ("Imagen Eliminada", "Producto eliminado"): el círculo del
 * ícono va en rosado (`error` al 22 %), como el Figma `476:3960`.
 * @param modifier Modificador opcional para el contenido del diálogo.
 */
@Composable
fun SmartFoodResultDialog(
    icon: @Composable () -> Unit,
    title: String,
    body: String? = null,
    destructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Sin cierre propio: atrás y el toque fuera no lo cierran (ver el KDoc); lo quita quien lo muestra.
    Dialog(onDismissRequest = {}) {
        BlurBehindDialog()
        SmartFoodResultDialogContent(
            icon = icon,
            title = title,
            body = body,
            destructive = destructive,
            modifier = modifier
        )
    }
}

/**
 * Indica que una acción está en curso, como guardar un producto, para que el usuario sepa que la app trabaja.
 *
 * El Figma no tiene un estado de carga: usa la tarjeta de [SmartFoodResultDialog] con un indicador circular en lugar
 * del ícono (spec 006, CA-INV-02-12, T056, desviación en §8). No se cierra con atrás ni tocando fuera; lo quita quien
 * lo muestra cuando la acción termina.
 *
 * @param text Acción en curso, como "Guardando producto…". TalkBack la anuncia al aparecer.
 * @param modifier Modificador opcional para el contenido del diálogo.
 */
@Composable
fun SmartFoodLoadingDialog(
    text: String,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = {}) {
        BlurBehindDialog()
        SmartFoodResultDialogContent(icon = { LoadingIndicator() }, title = text, body = null, modifier = modifier)
    }
}

/** Indicador circular del diálogo de carga, del mismo tamaño relativo que los íconos de resultado. */
@Composable
private fun LoadingIndicator() {
    CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * LoadingIndicatorFraction)
    )
}

/** Muestra el contenido de [SmartFoodResultDialog] para reutilizarlo en vistas previas. */
@Composable
private fun SmartFoodResultDialogContent(
    icon: @Composable () -> Unit,
    title: String,
    body: String?,
    destructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val metrics = LocalFormMetrics.current
    val dialogShape = RoundedCornerShape(spacing.panelRadius)

    Column(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .padding(horizontal = spacing.extraSmall, vertical = spacing.small)
            .figmaShadow(
                color = ShadowColorModal,
                borderRadius = spacing.panelRadius,
                blurRadius = spacing.extraSmall,
                offsetY = spacing.extraSmall
            )
            .background(MaterialTheme.colorScheme.surfaceVariant, dialogShape)
            .padding(
                horizontal = metrics.formHorizontalPadding,
                vertical = metrics.sectionSpacing
            )
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing)
    ) {
        Box(
            modifier = Modifier
                .size(spacing.iconDialogSize)
                .background(
                    if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            // Figma 476:3932: título en Poppins Bold de 20 sp.
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        body?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.secondary,
                // Figma 476:3933: cuerpo en Poppins Bold de 15 sp, el tamaño de `bodyLarge`.
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
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

@Preview(showBackground = true)
@Composable
private fun SmartFoodConfirmDialogPreview() {
    SmartfoodTheme {
        SmartFoodConfirmDialogContent(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    // Figma 476:3813: el glifo ocupa el 40 % del círculo. Los vectores llegan con el #37 (T027).
                    modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * 0.4f)
                )
            },
            question = "¿Desea eliminar el producto?",
            confirmText = "Eliminar",
            dismissText = "Cancelar",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodResultDialogPreview() {
    SmartfoodTheme {
        SmartFoodResultDialogContent(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    // Figma 476:3928: el glifo ocupa el 40 % del círculo. Los vectores llegan con el #37 (T027).
                    modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * 0.4f)
                )
            },
            title = "Producto Guardado",
            body = "Te avisaremos antes de que caduque para que no se desperdicie."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodLoadingDialogPreview() {
    SmartfoodTheme {
        SmartFoodResultDialogContent(icon = { LoadingIndicator() }, title = "Guardando producto…", body = null)
    }
}

/**
 * Desenfoca lo que queda detrás del diálogo, como el Figma (`476:3851` y `476:4021`), con un velo muy claro. El
 * desenfoque de ventanas existe desde Android 12 (API 31); antes solo queda el velo.
 */
@Composable
private fun BlurBehindDialog() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    SideEffect {
        window.setDimAmount(DialogDimAmount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = DialogBlurRadiusPx }
        }
    }
}

/** Velo detrás de los diálogos: muy claro, porque el fondo ya va desenfocado (Figma). */
private const val DialogDimAmount = 0.15f

/** Parte del círculo que ocupa el indicador de carga. */
private const val LoadingIndicatorFraction = 0.5f

/** Radio del desenfoque detrás de los diálogos, en píxeles. */
private const val DialogBlurRadiusPx = 40
