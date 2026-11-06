package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.GreenSoft
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.feature.recipes.ui.components.OutlineActionButton
import com.smartfood.app.feature.recipes.ui.components.RecipeActionButton

/**
 * Nombre: OwnRecipeSavedScreen.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: "¡Receta guardada!" al crear (Figma `557:2551`) o editar (`666:3717`) una receta propia (spec 010,
 * CA-SAV-03-04, 03-07 y 04-02, T014).
 *
 * @param name Nombre de la receta.
 * @param edited `true` si se editó.
 * @param onView Abre el detalle.
 * @param onCreateAnother Abre "Nueva receta" vacía.
 * @param onBack Vuelve a Guardadas.
 */
@Composable
fun OwnRecipeSavedScreen(
    name: String,
    edited: Boolean,
    onView: () -> Unit,
    onCreateAnother: () -> Unit,
    onBack: () -> Unit,
) {
    OwnRecipeResultLayout(
        title = stringResource(id = if (edited) R.string.own_edit_title else R.string.saved_new_recipe_title),
        onBack = onBack,
        heading = stringResource(id = R.string.own_saved_heading),
        body = stringResource(id = if (edited) R.string.own_saved_updated else R.string.own_saved_created, name),
        icon = {
            ResultCircle(color = GreenSoft) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_check),
                    contentDescription = null,
                    tint = GreenCheck,
                    modifier = Modifier.size(width = 50.dp, height = 36.dp)
                )
            }
        },
        actions = {
            RecipeActionButton(
                text = stringResource(id = R.string.own_saved_view),
                onClick = onView,
                modifier = Modifier.fillMaxWidth()
            )
            OutlineActionButton(
                text = stringResource(id = R.string.own_saved_another),
                onClick = onCreateAnother,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun OwnRecipeSavedScreenPreview() {
    SmartfoodTheme {
        OwnRecipeSavedScreen("Arepas rellenas de vegetales", edited = false, onView = {}, onCreateAnother = {}, onBack = {})
    }
}
