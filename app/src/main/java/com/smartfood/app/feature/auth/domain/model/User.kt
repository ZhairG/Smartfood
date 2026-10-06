package com.smartfood.app.feature.auth.domain.model

/**
 * <build>
 * <b>Nombre:</b> User.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 04/10/2026<br>
 * <b>Descripción:</b> Perfil del usuario de SmartFood (RF-AUTH-02). Se guarda en Firestore en `users/{uid}`.<br>
 * </build>
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
