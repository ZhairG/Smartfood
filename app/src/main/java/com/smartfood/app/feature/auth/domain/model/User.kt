package com.smartfood.app.feature.auth.domain.model

/**
 * Nombre: User.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Perfil del usuario de SmartFood (RF-AUTH-02). Se guarda en Firestore en `users/{uid}`.
 *
 * @property uid Identificador único de la cuenta en Firebase Authentication.
 * @property fullName Nombre completo ingresado en el registro.
 * @property email Correo electrónico de la cuenta.
 */
data class User(
    val uid: String,
    val fullName: String,
    val email: String
)
