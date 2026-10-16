/**
 * Nombre: EditorLineFields.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Campos del editor de producto con línea inferior, como el Figma (`476:4106` campo editable y
 * `476:4197` lista desplegable): ícono a la izquierda, texto en gris como sugerencia y en negro con valor, y una
 * línea debajo que se pone roja con error (spec 006, T055). Reemplazan en el editor a la caja con borde de
 * autenticación, que no cambia. Las medidas salen de `LocalFormMetrics` (RNF-INV-06).
 */
package com.smartfood.app.feature.inventory.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics

/**
 * Campo de texto con línea inferior.
 *
 * @param value Texto actual.
 * @param onValueChange Recibe el texto nuevo.
 * @param placeholder Sugerencia cuando está vacío; también es el nombre accesible si falta [accessibilityLabel].
 * @param icon Ícono de la izquierda.
 * @param modifier Modificador opcional.
 * @param isError Pone la línea y el texto en rojo.
 * @param accessibilityLabel Nombre que anuncia TalkBack.
 * @param keyboardOptions Teclado del campo.
 */
@Composable
internal fun EditorLineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    accessibilityLabel: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val name = accessibilityLabel ?: placeholder
    // El mismo enfoque de los campos de autenticación: la línea se pone verde al enfocar (FieldFrame).
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        textStyle = lineTextStyle(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = name },
        decorationBox = { innerTextField ->
            LineFrame(icon = icon, isError = isError, isFocused = isFocused) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        LineText(text = placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextField()
                }
            }
        }
    )
}

/**
 * Selector con línea inferior (fecha, categoría y unidad): se toca para abrir su hoja o calendario.
 *
 * @param value Valor elegido, o vacío.
 * @param placeholder Sugerencia cuando no hay valor.
 * @param onClick Abre el selector.
 * @param modifier Modificador opcional.
 * @param icon Ícono de la izquierda, opcional.
 * @param trailing Contenido de la derecha (la flecha de la categoría), opcional.
 * @param isError Pone la línea en rojo.
 * @param accessibilityLabel Nombre que anuncia TalkBack cuando hay un valor.
 */
@Composable
internal fun EditorLineSelector(
    value: String,
    placeholder: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    accessibilityLabel: String? = null,
) {
    val spokenName = accessibilityLabel ?: placeholder?.takeIf { value.isNotEmpty() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (spokenName != null) Modifier.semantics { contentDescription = spokenName } else Modifier)
    ) {
        LineFrame(icon = icon, isError = isError, trailing = trailing) {
            if (value.isNotEmpty()) {
                LineText(text = value, color = MaterialTheme.colorScheme.onSurface)
            } else if (placeholder != null) {
                LineText(text = placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Marco común: ícono, contenido, contenido final y la línea inferior (roja con error, verde al enfocar). */
@Composable
private fun LineFrame(
    icon: (@Composable () -> Unit)?,
    isError: Boolean,
    isFocused: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val metrics = LocalFormMetrics.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = metrics.inputHeight)
                .padding(vertical = LineVerticalPadding)
        ) {
            if (icon != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(metrics.fieldIconSize)) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        icon()
                    }
                }
                Spacer(modifier = Modifier.width(metrics.fieldIconSpacing))
            }
            Box(modifier = Modifier.weight(1f)) { content() }
            if (trailing != null) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    trailing()
                }
            }
        }
        HorizontalDivider(
            color = when {
                isError -> MaterialTheme.colorScheme.error
                isFocused -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            },
            thickness = if (isError || isFocused) ErrorLine else Line
        )
    }
}

@Composable
private fun LineText(text: String, color: Color) {
    Text(text = text, style = lineTextStyle(color), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Texto de los campos del Figma: Poppins Medium de 13 px. */
@Composable
private fun lineTextStyle(color: Color): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(fontSize = LineTextSize, fontWeight = FontWeight.Medium, color = color)

// Medidas del Figma (476:4106 y 476:4197): texto de 13 px y línea de 1 px. No son medidas nuevas de formulario: el
// alto, el ícono y su separación salen de FormMetrics.
private val LineTextSize = 13.sp
private val Line = 1.dp
private val ErrorLine = 2.dp
private val LineVerticalPadding = 4.dp
