package com.smartfood.app.feature.recipes.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.feature.recipes.ui.components.recipeCardShadow

/**
 * Nombre: DeletedRecipeBanner.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Aviso "Receta eliminada" con "DESHACER · 8 s" del Figma `557:2592` (spec 010, CA-SAV-05-03 a 05-06,
 * RNF-SAV-04, T019). TalkBack anuncia el aviso una vez y "Deshacer" es una acción con nombre fijo, para no leer cada segundo.
 *
 * @param name Nombre de la receta.
 * @param secondsLeft Segundos que quedan.
 * @param onUndo Se llama al tocar "DESHACER".
 * @param modifier Modificador opcional.
 */
@Composable
fun DeletedRecipeBanner(
    name: String,
    secondsLeft: Int,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(BannerRadius)
    val undoLabel = stringResource(id = R.string.cd_saved_undo)
    Column(
        verticalArrangement = Arrangement.spacedBy(BannerGap),
        modifier = modifier
            .fillMaxWidth()
            .recipeCardShadow()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(BannerPadding)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BannerGap),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
        ) {
            Text(
                text = stringResource(id = R.string.saved_deleted_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = stringResource(id = R.string.saved_deleted_body, name),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp)
            )
        }
        Text(
            text = stringResource(id = R.string.saved_deleted_undo, secondsLeft),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .clickable(onClick = onUndo)
                .clearAndSetSemantics {
                    contentDescription = undoLabel
                    role = Role.Button
                    onClick(label = undoLabel) {
                        onUndo()
                        true
                    }
                }
        )
    }
}

// Medidas del Figma 557:2592. No son de formulario, así que no van en FormMetrics.
private val BannerRadius = 20.dp
private val BannerPadding = 16.dp
private val BannerGap = 8.dp
