package com.smartfood.app.feature.profile.domain.model

/**
 * Nombre: NotificationSettings.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Avisos que el usuario elige recibir en este teléfono (spec 008, HU-PER-04). Todos vienen encendidos la
 * primera vez (CA-PER-04-01). "Recetas sugeridas" solo se guarda; su efecto lo define la spec 009 (CA-PER-04-05).
 *
 * @param expiringProducts Aviso diario de productos por vencer (CA-PER-04-03).
 * @param suggestedRecipes Aviso de recetas sugeridas (CA-PER-04-05).
 * @param emptyInventory Aviso cuando el inventario no tiene productos (CA-PER-04-04).
 */
data class NotificationSettings(
    val expiringProducts: Boolean = true,
    val suggestedRecipes: Boolean = true,
    val emptyInventory: Boolean = true
)
