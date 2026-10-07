package com.smartfood.app.feature.legal.domain.model

/**
 * Nombre: LegalDocument.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Documentos legales que se aceptan al registrarse (spec 005) y su versión vigente.
 */

/** Documentos que el visitante acepta con la casilla del registro (HU-LEG-01). */
enum class LegalDocumentType {
    TERMS,
    PRIVACY
}

/**
 * Versión vigente de los dos documentos, que se aceptan juntos (RF-LEG-02, ADR-LEG-03). Se guarda en el perfil como
 * constancia de la aceptación (HU-LEG-02). Al publicar el texto definitivo se cambian ambas constantes.
 */
object LegalVersion {
    const val CURRENT = "2026-10-borrador"

    /** `true` mientras el texto sea una plantilla: la pantalla muestra el aviso de vista previa (CA-LEG-01-05). */
    const val IS_PREVIEW = true
}
