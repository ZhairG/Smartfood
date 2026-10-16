/**
 * Nombre: EditorResultHost.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Muestra encima de la lista (o de la búsqueda) el resultado que deja el editor al volver: "Producto
 * Guardado", "Guardado: «nombre» se actualizó" o "Producto eliminado", como el Figma `476:3851` (spec 006, T055,
 * CA-INV-02-06, 03-02 y 04-02). El editor deja el resultado en el `savedStateHandle` de la pantalla anterior; aquí se
 * muestra unos segundos y se limpia para no repetirlo.
 */
package com.smartfood.app.feature.inventory.ui.editor

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodResultDialog
import com.smartfood.app.core.designsystem.theme.spacing
import kotlinx.coroutines.delay

/** Clave del `savedStateHandle` donde el editor deja su resultado. */
const val EDITOR_RESULT_KEY = "editor_result"

/** Convierte el resultado en texto para guardarlo en el `savedStateHandle`. */
fun EditorResult.encode(): String = when (this) {
    EditorResult.Saved -> SAVED
    is EditorResult.Updated -> UPDATED_PREFIX + name
    EditorResult.Deleted -> DELETED
}

/** Lee el resultado guardado con [encode], o `null` si no hay. */
internal fun decodeEditorResult(raw: String?): EditorResult? = when {
    raw == null -> null
    raw == SAVED -> EditorResult.Saved
    raw == DELETED -> EditorResult.Deleted
    raw.startsWith(UPDATED_PREFIX) -> EditorResult.Updated(raw.removePrefix(UPDATED_PREFIX))
    else -> null
}

/**
 * Muestra el resultado del editor encima de la pantalla actual y lo limpia a los 2 s.
 *
 * @param savedStateHandle `savedStateHandle` del destino que recibe el resultado.
 */
@Composable
fun EditorResultHost(savedStateHandle: SavedStateHandle) {
    val raw by savedStateHandle.getStateFlow<String?>(EDITOR_RESULT_KEY, null).collectAsStateWithLifecycle()
    val result = decodeEditorResult(raw) ?: return

    LaunchedEffect(raw) {
        delay(ProductEditorViewModel.RESULT_DIALOG_MILLIS)
        savedStateHandle[EDITOR_RESULT_KEY] = null
    }

    when (result) {
        EditorResult.Saved -> SmartFoodResultDialog(
            icon = { ResultIcon(Icons.Outlined.Check, MaterialTheme.colorScheme.primary) },
            title = stringResource(id = R.string.editor_saved_title),
            body = stringResource(id = R.string.editor_saved_body)
        )
        is EditorResult.Updated -> SmartFoodResultDialog(
            icon = { ResultIcon(Icons.Outlined.Check, MaterialTheme.colorScheme.primary) },
            title = stringResource(id = R.string.editor_updated_title),
            body = stringResource(id = R.string.editor_updated_body, result.name)
        )
        EditorResult.Deleted -> SmartFoodResultDialog(
            icon = { ResultIcon(Icons.Outlined.Delete, MaterialTheme.colorScheme.error) },
            title = stringResource(id = R.string.editor_deleted_title),
            body = stringResource(id = R.string.editor_deleted_body),
            destructive = true
        )
    }
}

/** Ícono del resultado: el glifo ocupa el 40 % del círculo, como en el Figma (T025). */
@Composable
private fun ResultIcon(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * GlyphFraction)
    )
}

private const val SAVED = "saved"
private const val DELETED = "deleted"
private const val UPDATED_PREFIX = "updated:"
private const val GlyphFraction = 0.4f
