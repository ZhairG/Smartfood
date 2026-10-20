package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.GreyNeutral
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.util.inputBorderAndBackground

/**
 * Nombre: OtpCodeInput.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componente atómico para ingreso de código OTP de 4 dígitos según Figma.
 * Muestra 4 casillas independientes con borde de foco, borde de error y transferencia fluida de datos.
 *
 * @param code Valor actual del código de 4 dígitos.
 * @param onCodeChanged Callback notificado al modificar el valor del código.
 * @param modifier Modificador opcional.
 * @param length Longitud del código (por defecto 4 dígitos).
 * @param isError Si es `true`, aplica resaltado de error con borde rojo en las casillas.
 * @param enabled Habilita o deshabilita la edición del código.
 * @param onDone Acción de la tecla "Listo": oculta el teclado y luego se ejecuta (CA-UI-02-03).
 */
@Composable
fun SmartFoodOtpCodeInput(
    code: String,
    onCodeChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
    isError: Boolean = false,
    enabled: Boolean = true,
    onDone: () -> Unit = {}
) {
    val metrics = LocalFormMetrics.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = code,
        onValueChange = { newValue ->
            if (newValue.length <= length && newValue.all { it.isDigit() }) {
                onCodeChanged(newValue)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onDone()
            }
        ),
        enabled = enabled,
        singleLine = true,
        interactionSource = interactionSource,
        modifier = modifier.focusRequester(focusRequester),
        decorationBox = {
            // Casillas rectangulares centradas, con la proporción y separación de Figma (CA-UI-07-05). Si el ancho no
            // alcanza (por ejemplo dentro de un aviso, spec 008), casillas y separación se reducen en la misma
            // proporción para que ninguna quede cortada.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val needed = metrics.otpBoxWidth * length + metrics.otpSpacing * (length - 1)
                val scale = if (maxWidth < needed) maxWidth / needed else 1f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.otpSpacing * scale, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(length) { index ->
                        val char = code.getOrNull(index)?.toString() ?: ""
                        val isBoxFocused = isFocused && (index == code.length || (index == length - 1 && code.length == length))

                        val borderColor = when {
                            isError -> MaterialTheme.colorScheme.error
                            isBoxFocused -> MaterialTheme.colorScheme.primary
                            else -> GreyNeutral
                        }
                        val borderWidth = if (isError) 2.dp else 1.dp

                        Box(
                            modifier = Modifier
                                .size(width = metrics.otpBoxWidth * scale, height = metrics.otpBoxHeight * scale)
                                .inputBorderAndBackground(
                                    shape = RoundedCornerShape(10.dp),
                                    borderWidth = borderWidth,
                                    borderColor = borderColor,
                                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun OtpCodeInputNormalPreview() {
    SmartfoodTheme {
        SmartFoodOtpCodeInput(
            code = "442",
            onCodeChanged = {},
            isError = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OtpCodeInputErrorPreview() {
    SmartfoodTheme {
        SmartFoodOtpCodeInput(
            code = "4423",
            onCodeChanged = {},
            isError = true
        )
    }
}
