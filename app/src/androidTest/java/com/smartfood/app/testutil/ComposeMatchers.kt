/**
 * Nombre: ComposeMatchers.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Comprobaciones de Compose que repetían varias pruebas instrumentadas (#105).
 */
package com.smartfood.app.testutil

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText

/** El nodo editable declara [type] para el autocompletado del teléfono (spec 001, HU-07). */
fun conTipo(type: ContentType): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.ContentType, type)

/** Cada uno de los [textos] se ve en pantalla. */
fun SemanticsNodeInteractionsProvider.mostrado(vararg textos: String) {
    textos.forEach { onNodeWithText(it).assertIsDisplayed() }
}
