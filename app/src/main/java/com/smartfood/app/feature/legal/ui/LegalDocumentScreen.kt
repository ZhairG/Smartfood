package com.smartfood.app.feature.legal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.SystemBarEffect
import com.smartfood.app.feature.legal.domain.model.LegalDocumentType
import com.smartfood.app.feature.legal.domain.model.LegalVersion

/**
 * Nombre: LegalDocumentScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pantalla stateless de un documento legal (spec 005, HU-LEG-01): barra con "Volver", aviso de vista
 * previa con la versión (CA-LEG-01-05) y secciones desplazables con títulos anunciados como encabezados
 * (CA-LEG-01-06). No tiene ViewModel porque no tiene estado ni eventos propios (plan 005 §1).
 *
 * @param type Documento que se muestra.
 * @param onBack Callback de la flecha "Volver".
 * @param modifier Modificador opcional para la apariencia exterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    type: LegalDocumentType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content = legalContent(type)
    val spacing = MaterialTheme.spacing

    SystemBarEffect(isLightBackground = !LocalIsDarkTheme.current)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // En este design system `background` es el verde del encabezado de marca; el panel claro es `surface`.
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = content.title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.legal_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.large,
                end = spacing.large,
                top = innerPadding.calculateTopPadding() + spacing.small,
                bottom = innerPadding.calculateBottomPadding() + spacing.extraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (LegalVersion.IS_PREVIEW) {
                item { PreviewNotice(modifier = Modifier.textColumn()) }
            }
            item {
                Text(
                    text = stringResource(id = content.intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.textColumn()
                )
            }
            items(content.sections) { section ->
                LegalSectionBlock(section = section, modifier = Modifier.textColumn())
            }
        }
    }
}

/** Ancho máximo del texto, para que las líneas no queden demasiado largas en tabletas. */
private val MaxTextWidth = 600.dp

/** Columna de texto centrada con ancho máximo. */
private fun Modifier.textColumn(): Modifier = widthIn(max = MaxTextWidth).fillMaxWidth()

/** Píldora "Vista previa" con la versión del documento (CA-LEG-01-05). */
@Composable
private fun PreviewNotice(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(MaterialTheme.spacing.inputRadius))
            .padding(MaterialTheme.spacing.medium)
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.legal_preview_notice),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(id = R.string.legal_version, LegalVersion.CURRENT),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/** Título (encabezado para el lector de pantalla) y cuerpo de una sección (CA-LEG-01-06). */
@Composable
private fun LegalSectionBlock(section: LegalSection, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Text(
            text = stringResource(id = section.title),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp, lineHeight = 25.5.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(id = section.body),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
            // onSurfaceVariant es gris claro, pensado para subtítulos; en párrafos largos el contraste es bajo.
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Términos · Claro")
@Composable
private fun LegalDocumentScreenTermsPreview() {
    SmartfoodTheme(themeMode = ThemeMode.LIGHT) {
        LegalDocumentScreen(type = LegalDocumentType.TERMS, onBack = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Privacidad · Oscuro")
@Composable
private fun LegalDocumentScreenPrivacyPreview() {
    SmartfoodTheme(themeMode = ThemeMode.DARK) {
        LegalDocumentScreen(type = LegalDocumentType.PRIVACY, onBack = {})
    }
}
