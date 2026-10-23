package com.smartfood.app.feature.recipes.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodTopBar
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.YellowSoft
import com.smartfood.app.feature.recipes.ui.components.OutlineActionButton

/**
 * Nombre: AiHelpScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: "Ayuda" de la IA (spec 009, CA-RIA-03-01, T018; Figma `557:2476`), sin estado y sin ViewModel, como
 * `LegalDocumentScreen`: "¿Cómo decide la IA?" con sus tres criterios, "Controles y seguridad", el aviso "La IA puede
 * equivocarse" y "Contactar soporte", que abre el correo del equipo (lo hace el grafo con [onContactSupport]).
 *
 * @param onBack Flecha atrás: vuelve al contexto (Figma `633:2997`).
 * @param onContactSupport Abre el correo a soporte.
 * @param modifier Modificador opcional.
 */
@Composable
fun AiHelpScreen(
    onBack: () -> Unit,
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SmartFoodTopBar(title = stringResource(id = R.string.recipes_ai_help_title), onBack = onBack)
        Column(
            verticalArrangement = Arrangement.spacedBy(ContentGap),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(ContentPadding)
        ) {
            Text(
                text = stringResource(id = R.string.recipes_ai_help_heading),
                color = MaterialTheme.colorScheme.onSurface,
                style = TitleStyle(),
                modifier = Modifier.semantics { heading() }
            )
            WhiteCard {
                Criterion(R.string.recipes_ai_help_urgency, R.string.recipes_ai_help_urgency_body)
                Criterion(R.string.recipes_ai_help_usage, R.string.recipes_ai_help_usage_body)
                Criterion(R.string.recipes_ai_help_budget, R.string.recipes_ai_help_budget_body)
            }
            Text(
                text = stringResource(id = R.string.recipes_ai_help_controls),
                color = MaterialTheme.colorScheme.onSurface,
                style = TitleStyle(),
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(id = R.string.recipes_ai_help_controls_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = BodyStyle()
            )
            // Tarjeta amarilla del aviso, como la del temporizador (YellowSoft).
            Column(
                verticalArrangement = Arrangement.spacedBy(CardInnerGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .cardShadow()
                    .clip(RoundedCornerShape(CardRadius))
                    .background(if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.surfaceVariant else YellowSoft)
                    .padding(CardPadding)
            ) {
                Text(
                    text = stringResource(id = R.string.recipes_ai_help_warning),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = CardTitleStyle()
                )
                Text(
                    text = stringResource(id = R.string.recipes_ai_help_warning_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = BodyStyle()
                )
            }
            OutlineActionButton(
                text = stringResource(id = R.string.recipes_ai_contact),
                onClick = onContactSupport,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Un criterio de "¿Cómo decide la IA?": título y explicación. */
@Composable
private fun Criterion(title: Int, body: Int) {
    Column {
        Text(text = stringResource(id = title), color = MaterialTheme.colorScheme.onSurface, style = CardTitleStyle())
        Text(text = stringResource(id = body), color = MaterialTheme.colorScheme.onSurfaceVariant, style = BodyStyle())
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AiHelpScreenPreview() {
    SmartfoodTheme {
        AiHelpScreen(onBack = {}, onContactSupport = {})
    }
}
