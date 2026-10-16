/**
 * Nombre: ProductEditorScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla sin estado del editor de producto (spec 006, HU-INV-02 a HU-INV-05; Figma "Añadir Producto"
 * `476:3664` y "Editar Producto" `476:3689`). Es una pantalla interna: barra superior con la flecha atrás y sin barra
 * inferior (CA-INV-09-04; el Figma la muestra, pero manda la spec). Las medidas salen de `LocalFormMetrics`
 * (RNF-INV-06); el formulario se desplaza con un solo `verticalScroll` y el teclado se aplica fuera de él.
 *
 * @param state Estado del editor.
 * @param onEvent Recibe lo que hace el usuario.
 */
package com.smartfood.app.feature.inventory.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodConfirmDialog
import com.smartfood.app.core.designsystem.components.SmartFoodPrimaryButton
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.components.SmartFoodSelectorField
import com.smartfood.app.core.designsystem.components.SmartFoodTextField
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.formMetricsForWindow
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.feature.inventory.domain.model.FoodCategory
import com.smartfood.app.feature.inventory.domain.model.InventoryField
import com.smartfood.app.feature.inventory.domain.model.QuantityUnit
import com.smartfood.app.feature.inventory.ui.components.ExpiryDatePicker
import com.smartfood.app.feature.inventory.ui.components.OptionSheet
import com.smartfood.app.feature.inventory.ui.components.PhotoSlot
import com.smartfood.app.feature.inventory.ui.components.titleRes
import com.smartfood.app.feature.inventory.ui.messageRes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProductEditorScreen(
    state: ProductEditorUiState,
    onEvent: (ProductEditorEvent) -> Unit,
) {
    val isEdit = state.mode == EditorMode.EDIT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SmartFoodTopBar(
            title = stringResource(id = if (isEdit) R.string.editor_title_edit else R.string.editor_title_add),
            onBack = { onEvent(ProductEditorEvent.BackClicked) },
            action = if (isEdit) {
                {
                    TextButton(
                        onClick = { onEvent(ProductEditorEvent.SaveClicked) },
                        enabled = state.isSaveEnabled && !state.isSaving
                    ) {
                        Text(
                            text = stringResource(id = R.string.editor_save),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                null
            }
        )

        // El teclado se aplica fuera del desplazable; Compose mantiene visible el campo enfocado (spec 002).
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .navigationBarsPadding()
        ) {
            CompositionLocalProvider(LocalFormMetrics provides formMetricsForWindow(maxHeight)) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    EditorForm(state = state, onEvent = onEvent)
                }
            }
        }
    }

    EditorDialogs(state = state, onEvent = onEvent)
}

/** Foto, campos y botón inferior del editor. */
@Composable
private fun EditorForm(state: ProductEditorUiState, onEvent: (ProductEditorEvent) -> Unit) {
    val metrics = LocalFormMetrics.current
    val errorField = state.errorField
    val errorText = state.error?.let { stringResource(id = it.messageRes()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(metrics.fieldSpacing),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = metrics.formHorizontalPadding, vertical = metrics.sectionSpacing)
    ) {
        PhotoSlot(
            photo = state.photo,
            onCameraClick = { onEvent(ProductEditorEvent.CameraClicked) },
            onRemoveClick = { onEvent(ProductEditorEvent.RemovePhotoClicked) }
        )

        SmartFoodTextField(
            value = state.name,
            onValueChange = { onEvent(ProductEditorEvent.NameChanged(it)) },
            placeholder = stringResource(id = R.string.editor_name_placeholder),
            accessibilityLabel = stringResource(id = R.string.editor_name_label),
            leadingContent = { FieldIcon(Icons.Outlined.ShoppingCart) },
            isError = errorField == InventoryField.NAME
        )
        SmartFoodSelectorField(
            value = state.expiresOn?.format(DateFormatter).orEmpty(),
            onClick = { onEvent(ProductEditorEvent.ExpiryClicked) },
            placeholder = stringResource(id = R.string.editor_expiry_placeholder),
            leadingContent = { FieldIcon(Icons.Outlined.CalendarMonth) },
            isError = errorField == InventoryField.EXPIRY
        )
        SmartFoodSelectorField(
            value = state.category?.let { stringResource(id = it.titleRes) }.orEmpty(),
            onClick = { onEvent(ProductEditorEvent.CategoryClicked) },
            placeholder = stringResource(id = R.string.editor_category_placeholder),
            leadingContent = { FieldIcon(Icons.Outlined.Sell) },
            trailingContent = { FieldIcon(Icons.AutoMirrored.Outlined.KeyboardArrowRight) },
            isError = errorField == InventoryField.CATEGORY
        )
        // Fila opcional "Cantidad" (T002, desviación de la spec §8): número y unidad.
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.fieldIconSpacing)) {
            SmartFoodTextField(
                value = state.quantityText,
                onValueChange = { onEvent(ProductEditorEvent.QuantityChanged(it)) },
                placeholder = stringResource(id = R.string.editor_quantity_placeholder),
                leadingContent = { FieldIcon(Icons.Outlined.Scale) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = errorField == InventoryField.QUANTITY,
                modifier = Modifier.weight(QuantityWeight)
            )
            SmartFoodSelectorField(
                value = stringResource(id = (state.unit ?: QuantityUnit.UNITS).titleRes),
                onClick = { onEvent(ProductEditorEvent.UnitClicked) },
                accessibilityLabel = stringResource(id = R.string.editor_unit_label),
                modifier = Modifier.weight(UnitWeight)
            )
        }

        if (errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }

        Spacer(modifier = Modifier.height(metrics.sectionSpacing))

        if (state.mode == EditorMode.ADD) {
            SmartFoodPrimaryButton(
                text = stringResource(id = R.string.editor_add_button),
                onClick = { onEvent(ProductEditorEvent.SaveClicked) },
                enabled = state.isSaveEnabled,
                isLoading = state.isSaving,
                leadingIcon = Icons.Rounded.Add
            )
        } else {
            SmartFoodPrimaryButton(
                text = stringResource(id = R.string.editor_delete_button),
                onClick = { onEvent(ProductEditorEvent.DeleteClicked) },
                enabled = !state.isSaving,
                destructive = true
            )
        }
    }
}

/** Hojas, calendario y diálogos que dependen de [ProductEditorUiState.dialog]. */
@Composable
private fun EditorDialogs(state: ProductEditorUiState, onEvent: (ProductEditorEvent) -> Unit) {
    val dismiss = { onEvent(ProductEditorEvent.DialogDismissed) }
    when (val dialog = state.dialog) {
        EditorDialog.CategorySheet -> OptionSheet(
            title = stringResource(id = R.string.editor_category_sheet_title),
            options = CategoryOptions,
            selected = state.category,
            label = { stringResource(id = it.titleRes) },
            onConfirm = { onEvent(ProductEditorEvent.CategoryPicked(it)) },
            onDismiss = dismiss
        )
        EditorDialog.UnitSheet -> OptionSheet(
            title = stringResource(id = R.string.editor_unit_sheet_title),
            options = UnitOptions,
            selected = state.unit ?: QuantityUnit.UNITS,
            label = { stringResource(id = it.titleRes) },
            onConfirm = { onEvent(ProductEditorEvent.UnitPicked(it)) },
            onDismiss = dismiss
        )
        EditorDialog.DatePicker -> ExpiryDatePicker(
            selected = state.expiresOn,
            original = state.originalExpiresOn,
            onConfirm = { onEvent(ProductEditorEvent.ExpiryPicked(it)) },
            onDismiss = dismiss
        )
        EditorDialog.ConfirmDelete -> SmartFoodConfirmDialog(
            icon = { DialogIcon(Icons.Outlined.Delete, MaterialTheme.colorScheme.error) },
            question = stringResource(id = R.string.editor_confirm_delete),
            confirmText = stringResource(id = R.string.editor_confirm),
            dismissText = stringResource(id = R.string.editor_cancel),
            onConfirm = { onEvent(ProductEditorEvent.DeleteConfirmed) },
            onDismiss = dismiss
        )
        EditorDialog.ConfirmRemovePhoto -> SmartFoodConfirmDialog(
            icon = { DialogIcon(Icons.Outlined.Delete, MaterialTheme.colorScheme.error) },
            question = stringResource(id = R.string.editor_confirm_remove_photo),
            confirmText = stringResource(id = R.string.editor_confirm),
            dismissText = stringResource(id = R.string.editor_cancel),
            onConfirm = { onEvent(ProductEditorEvent.RemovePhotoConfirmed) },
            onDismiss = dismiss
        )
        EditorDialog.ProductSaved -> SmartFoodResultDialog(
            icon = { DialogIcon(Icons.Outlined.Check, MaterialTheme.colorScheme.primary) },
            title = stringResource(id = R.string.editor_saved_title),
            body = stringResource(id = R.string.editor_saved_body)
        )
        is EditorDialog.ProductUpdated -> SmartFoodResultDialog(
            icon = { DialogIcon(Icons.Outlined.Check, MaterialTheme.colorScheme.primary) },
            title = stringResource(id = R.string.editor_updated_title),
            body = stringResource(id = R.string.editor_updated_body, dialog.name)
        )
        EditorDialog.ProductDeleted -> SmartFoodResultDialog(
            icon = { DialogIcon(Icons.Outlined.Check, MaterialTheme.colorScheme.primary) },
            title = stringResource(id = R.string.editor_deleted_title)
        )
        EditorDialog.PhotoRemoved -> SmartFoodResultDialog(
            icon = { DialogIcon(Icons.Outlined.Check, MaterialTheme.colorScheme.primary) },
            title = stringResource(id = R.string.editor_photo_removed)
        )
        null -> Unit
    }
}

/** Ícono gris de un campo; el campo lo dibuja en su caja de `fieldIconSize`. */
@Composable
private fun FieldIcon(icon: ImageVector) {
    Icon(imageVector = icon, contentDescription = null)
}

/** Ícono de un diálogo: el glifo ocupa el 40 % del círculo, como en el Figma (T025). */
@Composable
private fun DialogIcon(icon: ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * DialogGlyphFraction)
    )
}

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Reparto del ancho de la fila de cantidad: el número ocupa más que la unidad.
private const val QuantityWeight = 0.6f
private const val UnitWeight = 0.4f
private const val DialogGlyphFraction = 0.4f

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ProductEditorScreenAddPreview() {
    SmartfoodTheme {
        ProductEditorScreen(state = ProductEditorUiState(), onEvent = {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ProductEditorScreenEditPreview() {
    SmartfoodTheme {
        ProductEditorScreen(
            state = ProductEditorUiState(
                mode = EditorMode.EDIT,
                name = "Carne",
                expiresOn = LocalDate.of(2026, 9, 20),
                category = FoodCategory.PROTEINS,
                quantityText = "1.5",
                unit = QuantityUnit.KILOGRAMS,
                isSaveEnabled = true
            ),
            onEvent = {}
        )
    }
}
