package com.smartfood.app.feature.legal.ui

import androidx.annotation.StringRes
import com.smartfood.app.R
import com.smartfood.app.feature.legal.domain.model.LegalDocumentType

/**
 * Nombre: LegalContent.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Único lugar que relaciona cada [LegalDocumentType] con sus textos de `strings_legal.xml`
 * (spec 005, RNF-LEG-01). Para cambiar el contenido basta con editar los recursos.
 */

/**
 * Sección de un documento.
 *
 * @property title Título de la sección; se anuncia como encabezado (CA-LEG-01-06).
 * @property body Cuerpo de la sección.
 */
data class LegalSection(@StringRes val title: Int, @StringRes val body: Int)

/**
 * Contenido de un documento.
 *
 * @property title Título de la pantalla.
 * @property intro Párrafo introductorio.
 * @property sections Secciones en orden de lectura.
 */
data class LegalContent(
    @StringRes val title: Int,
    @StringRes val intro: Int,
    val sections: List<LegalSection>
)

/** Contenido del documento [type]. */
fun legalContent(type: LegalDocumentType): LegalContent = when (type) {
    LegalDocumentType.TERMS -> LegalContent(
        title = R.string.legal_terms_title,
        intro = R.string.legal_terms_intro,
        sections = listOf(
            LegalSection(R.string.legal_terms_s1_title, R.string.legal_terms_s1_body),
            LegalSection(R.string.legal_terms_s2_title, R.string.legal_terms_s2_body),
            LegalSection(R.string.legal_terms_s3_title, R.string.legal_terms_s3_body),
            LegalSection(R.string.legal_terms_s4_title, R.string.legal_terms_s4_body),
            LegalSection(R.string.legal_terms_s5_title, R.string.legal_terms_s5_body),
            LegalSection(R.string.legal_terms_s6_title, R.string.legal_terms_s6_body),
            LegalSection(R.string.legal_terms_s7_title, R.string.legal_terms_s7_body),
            LegalSection(R.string.legal_terms_s8_title, R.string.legal_terms_s8_body),
            LegalSection(R.string.legal_terms_s9_title, R.string.legal_terms_s9_body)
        )
    )

    LegalDocumentType.PRIVACY -> LegalContent(
        title = R.string.legal_privacy_title,
        intro = R.string.legal_privacy_intro,
        sections = listOf(
            LegalSection(R.string.legal_privacy_s1_title, R.string.legal_privacy_s1_body),
            LegalSection(R.string.legal_privacy_s2_title, R.string.legal_privacy_s2_body),
            LegalSection(R.string.legal_privacy_s3_title, R.string.legal_privacy_s3_body),
            LegalSection(R.string.legal_privacy_s4_title, R.string.legal_privacy_s4_body),
            LegalSection(R.string.legal_privacy_s5_title, R.string.legal_privacy_s5_body),
            LegalSection(R.string.legal_privacy_s6_title, R.string.legal_privacy_s6_body),
            LegalSection(R.string.legal_privacy_s7_title, R.string.legal_privacy_s7_body),
            LegalSection(R.string.legal_privacy_s8_title, R.string.legal_privacy_s8_body)
        )
    )
}
