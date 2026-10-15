package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.White
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.util.inputBorderAndBackground

/**
 * Nombre: Inputs.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componentes atómicos de entrada de datos (TextField, PasswordField, Checkbox y Términos).
 * Reutiliza las extensiones utilitarias de [inputBorderAndBackground].
 */

private val InputShape = RoundedCornerShape(10.dp)

/**
 * Componente atómico de Campo de Texto genérico con estilo de SmartFood basado en el mockup inicial.
 *
 * @param label Texto descriptivo superior opcional. Forma parte del área táctil y de la semántica del campo.
 * @param value Valor actual del texto ingresado.
 * @param onValueChange Callback notificado al modificar el texto.
 * @param modifier Modificador opcional.
 * @param placeholder Texto de sugerencia exhibido cuando [value] está vacío.
 * @param leadingContent Composable opcional al inicio del contenedor de texto.
 * @param trailingContent Composable opcional al final del contenedor de texto.
 * @param keyboardOptions Opciones de comportamiento del teclado.
 * @param keyboardActions Acciones disparadas desde las teclas del teclado.
 * @param visualTransformation Transforma la representación visual del texto.
 * @param enabled Si es `false`, deshabilita la interacción con el campo.
 * @param isError Si es `true`, dibuja el borde con color de error (rojo).
 * @param onFocusChanged Callback notificado en el mismo frame en que el campo gana o pierde el foco.
 */
@Composable
fun SmartFoodTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    isError: Boolean = false,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val metrics = LocalFormMetrics.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val currentOnFocusChanged by rememberUpdatedState(onFocusChanged)

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val borderWidth = if (isError) 2.dp else 1.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        // El foco se notifica en el mismo frame (RNF-UI-04).
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { currentOnFocusChanged?.invoke(it.isFocused) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            // La etiqueta vive dentro del campo: tocarla lo enfoca y su texto forma parte de la
            // semántica del campo, así que los lectores de pantalla la anuncian (CA-UI-02-01 y CA-UI-02-08).
            Column(verticalArrangement = Arrangement.spacedBy(metrics.labelSpacing)) {
                if (label != null) {
                    Text(
                        text = label,
                        fontSize = metrics.labelFontSize,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(metrics.inputHeight)
                        .inputBorderAndBackground(
                            shape = InputShape,
                            borderWidth = borderWidth,
                            borderColor = borderColor,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(
                            start = if (leadingContent != null) 12.dp else 15.dp,
                            end = if (trailingContent != null) 6.dp else 15.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    leadingContent?.invoke()
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                    trailingContent?.invoke()
                }
            }
        },
    )
}

/**
 * Sobrecarga de conveniencia de [SmartFoodTextField] con el parámetro [label] como primer argumento
 * para conservar compatibilidad con llamadas que usan posición.
 */
@Composable
fun SmartFoodTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    isError: Boolean = false,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    SmartFoodTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        enabled = enabled,
        isError = isError,
        onFocusChanged = onFocusChanged,
    )
}

/**
 * Variante específica de [SmartFoodTextField] configurada exclusivamente para contraseñas.
 * Gestiona de forma encapsulada la visibilidad del texto mediante el botón de visibilidad.
 */
@Composable
fun SmartFoodPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    isError: Boolean = false,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    SmartFoodTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        enabled = enabled,
        isError = isError,
        onFocusChanged = onFocusChanged,
        trailingContent = {
            IconButton(onClick = { visible = !visible }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (visible) {
                        stringResource(id = R.string.cd_hide_password)
                    } else {
                        stringResource(id = R.string.cd_show_password)
                    },
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
    )
}

/**
 * Componente atómico de Selección no editable con estilo de SmartFood.
 * Comparte la caja, bordes, colores y tipografía de [SmartFoodTextField], pero reacciona
 * al toque ejecutando [onClick] (útil para calendarios, selectores de categoría u hojas inferiores).
 *
 * @param value Valor actual seleccionado que se muestra en el campo.
 * @param onClick Callback notificado al tocar el selector.
 * @param modifier Modificador opcional.
 * @param label Texto descriptivo superior opcional.
 * @param placeholder Texto de sugerencia exhibido cuando [value] está vacío.
 * @param leadingContent Composable opcional al inicio del contenedor (ícono descriptivo).
 * @param trailingContent Composable opcional al final del contenedor (flecha de desplegable, calendario, etc.).
 * @param enabled Si es `false`, deshabilita la interacción con el selector.
 * @param isError Si es `true`, dibuja el borde con color de error (rojo).
 */
@Composable
fun SmartFoodSelectorField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val metrics = LocalFormMetrics.current

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val borderWidth = if (isError) 2.dp else 1.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.labelSpacing)
    ) {
        if (label != null) {
            Text(
                text = label,
                fontSize = metrics.labelFontSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.inputHeight)
                .inputBorderAndBackground(
                    shape = InputShape,
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button
                )
                .padding(
                    start = if (leadingContent != null) 12.dp else 15.dp,
                    end = if (trailingContent != null) 12.dp else 15.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingContent?.invoke()
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (value.isNotEmpty()) {
                    Text(
                        text = value,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}

/**
 * Componente atómico de Casilla de Verificación (Checkbox) interactiva con etiqueta.
 */
@Composable
fun SmartFoodCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.size(24.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = GreenCheck,
                checkmarkColor = White,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Casilla de verificación de aceptación de Términos y Condiciones con enlaces naranja estilo Figma.
 * Tocar la casilla o el texto alterna la aceptación (CA-01-05); tocar un enlace solo abre su documento
 * (spec 005, CA-LEG-01-01, CA-LEG-01-02). El toque se resuelve por carácter: así un enlace partido en dos líneas no
 * tapa el resto del texto. Como la fila fusiona su semántica, los enlaces se ofrecen como acciones del lector de
 * pantalla (CA-LEG-01-07).
 *
 * @param checked Estado de selección de la casilla.
 * @param onCheckedChange Callback notificado al alternar el estado.
 * @param onTermsClick Callback del enlace "Términos y condiciones".
 * @param onPrivacyClick Callback del enlace "Política de privacidad".
 * @param modifier Modificador opcional.
 */
@Composable
fun SmartFoodTermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentOnTermsClick by rememberUpdatedState(onTermsClick)
    val currentOnPrivacyClick by rememberUpdatedState(onPrivacyClick)
    val openTermsLabel = stringResource(id = R.string.legal_open_terms_action)
    val openPrivacyLabel = stringResource(id = R.string.legal_open_privacy_action)

    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
    val accept = stringResource(id = R.string.register_terms_accept).trimEnd()
    val terms = stringResource(id = R.string.register_terms_link)
    val and = stringResource(id = R.string.register_terms_and).trim()
    val privacy = stringResource(id = R.string.register_privacy_link)
    val text = remember(accept, terms, and, privacy, textColor, linkStyle) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = textColor)) { append("$accept ") }
            withAnnotation(LEGAL_LINK_ANNOTATION, TERMS_LINK) { withStyle(linkStyle) { append(terms) } }
            withStyle(SpanStyle(color = textColor)) { append(" $and ") }
            withAnnotation(LEGAL_LINK_ANNOTATION, PRIVACY_LINK) { withStyle(linkStyle) { append(privacy) } }
        }
    }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        modifier = modifier
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(openTermsLabel) { currentOnTermsClick(); true },
                    CustomAccessibilityAction(openPrivacyLabel) { currentOnPrivacyClick(); true }
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.size(24.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = GreenCheck,
                checkmarkColor = White,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            onTextLayout = { textLayout = it },
            modifier = Modifier.pointerInput(text) {
                detectTapGestures { position ->
                    when (textLayout?.legalLinkAt(text, position)) {
                        TERMS_LINK -> currentOnTermsClick()
                        PRIVACY_LINK -> currentOnPrivacyClick()
                        else -> currentOnCheckedChange(!currentChecked)
                    }
                }
            }
        )
    }
}

private const val LEGAL_LINK_ANNOTATION = "legal_link"
private const val TERMS_LINK = "terms"
private const val PRIVACY_LINK = "privacy"

/** Enlace legal del carácter que está exactamente bajo [position], o `null` si no hay uno. */
private fun TextLayoutResult.legalLinkAt(text: AnnotatedString, position: Offset): String? {
    val offset = getOffsetForPosition(position)
    val index = listOf(offset, offset - 1).firstOrNull { it in text.indices && getBoundingBox(it).contains(position) }
        ?: return null
    return text.getStringAnnotations(LEGAL_LINK_ANNOTATION, index, index + 1).firstOrNull()?.item
}

@Preview(showBackground = true)
@Composable
private fun SmartFoodTextFieldPreview() {
    SmartfoodTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SmartFoodTextField(
                label = "Correo electrónico",
                value = "",
                placeholder = "nombre@correo.com",
                onValueChange = {}
            )
            SmartFoodPasswordField(
                label = "Contraseña",
                value = "12345678",
                onValueChange = {}
            )
            SmartFoodSelectorField(
                label = "Seleccionar categoría",
                value = "Lácteos",
                placeholder = "Elegir...",
                onClick = {}
            )
            SmartFoodCheckbox(
                label = "Recuérdame",
                checked = true,
                onCheckedChange = {}
            )
            SmartFoodTermsCheckbox(
                checked = true,
                onCheckedChange = {},
                onTermsClick = {},
                onPrivacyClick = {}
            )
        }
    }
}
