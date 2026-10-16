/**
 * Nombre: OptionSheet.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Hoja inferior con opciones de radio, "Guardar" y "Cancelar" (spec 006, Figma "Categoria" `476:4113`).
 * La usan la categoría (CA-INV-02-05) y la unidad de la cantidad (CA-INV-02-08). "Guardar" aplica la opción
 * marcada y "Cancelar" deja la anterior.
 *
 * @param title Título de la hoja.
 * @param options Opciones en orden.
 * @param selected Opción vigente, o `null` si no hay ninguna.
 * @param label Texto de cada opción.
 * @param onConfirm Recibe la opción marcada al tocar "Guardar".
 * @param onDismiss Cierra la hoja sin cambios.
 */
package com.smartfood.app.feature.inventory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    var marked by remember(selected) { mutableStateOf(selected ?: options.first()) }
    val spacing = MaterialTheme.spacing
    val metrics = LocalFormMetrics.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = spacing.panelRadius, topEnd = spacing.panelRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = metrics.formHorizontalPadding)
                .padding(bottom = metrics.sectionSpacing)
                .navigationBarsPadding()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Column(modifier = Modifier.padding(vertical = spacing.small).selectableGroup()) {
                options.forEach { option ->
                    val isMarked = option == marked
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = isMarked, role = Role.RadioButton, onClick = { marked = option })
                            .padding(vertical = spacing.extraSmall)
                    ) {
                        RadioButton(
                            selected = isMarked,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = GreenCheck)
                        )
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = spacing.small)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium), modifier = Modifier.fillMaxWidth()) {
                SheetButton(
                    text = stringResource(id = R.string.editor_sheet_save),
                    containerColor = GreenCheck,
                    onClick = { onConfirm(marked) },
                    modifier = Modifier.weight(1f)
                )
                SheetButton(
                    text = stringResource(id = R.string.editor_sheet_cancel),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Botón de la hoja: verde para "Guardar" y naranja para "Cancelar", como el Figma. */
@Composable
private fun SheetButton(
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(MaterialTheme.spacing.buttonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier.height(LocalFormMetrics.current.inputHeight)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
