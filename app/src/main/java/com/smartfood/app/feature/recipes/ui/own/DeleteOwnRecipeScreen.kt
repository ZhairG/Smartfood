package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.ui.components.OutlineActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton

/**
 * Nombre: DeleteOwnRecipeScreen.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: "Eliminar receta" (spec 010, CA-SAV-05-01 y 05-02, T018), fiel al Figma `557:2620`.
 *
 * @param name Nombre de la receta.
 * @param state Estado de la confirmación.
 * @param onConfirm "Sí, eliminar".
 * @param onCancel "Cancelar" o la flecha.
 */
@Composable
fun DeleteOwnRecipeScreen(
    name: String,
    state: DeleteOwnRecipeUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val error = MaterialTheme.colorScheme.error
    OwnRecipeResultLayout(
        title = stringResource(id = R.string.own_delete_title),
        onBack = onCancel,
        heading = stringResource(id = R.string.own_delete_heading, name),
        body = stringResource(id = R.string.own_delete_body),
        icon = {
            ResultCircle(color = error.copy(alpha = CircleAlpha)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_figma_trash_red),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize)
                )
            }
        },
        actions = {
            state.error?.let {
                Text(
                    text = ownRecipeErrorReason(it, cookMinutes = ""),
                    color = error,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)
                )
            }
            OutlineActionButton(
                text = stringResource(id = R.string.own_delete_cancel),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
            RecipeActionButton(
                text = stringResource(id = R.string.own_delete_confirm),
                onClick = onConfirm,
                enabled = !state.isDeleting,
                containerColor = error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

// Figma 666:3463: rojo al 22 % y papelera de 40 px.
private const val CircleAlpha = 0.22f
private val IconSize = 40.dp

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun DeleteOwnRecipeScreenPreview() {
    SmartfoodTheme {
        DeleteOwnRecipeScreen("Pasta verde rápida", DeleteOwnRecipeUiState(), onConfirm = {}, onCancel = {})
    }
}
