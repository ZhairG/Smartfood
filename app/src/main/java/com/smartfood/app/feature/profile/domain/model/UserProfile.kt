package com.smartfood.app.feature.profile.domain.model

import java.time.LocalDate

/**
 * Nombre: UserProfile.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Perfil completo del usuario para la pestaña Perfil y "Editar información" (spec 008, plan §2.2,
 * RF-PER-01). Amplía el `User` del registro (spec 001) con los datos opcionales que se editan en el Perfil.
 *
 * @property uid Identificador de la cuenta en Firebase Authentication.
 * @property fullName Nombre completo, de 2 a 50 caracteres (CA-PER-02-05).
 * @property email Correo guardado en el perfil; se iguala al de la cuenta al abrir el Perfil (CA-PER-02-12).
 * @property phone Celular colombiano de 10 dígitos que empieza por 3, sin "+57" ni espacios (CA-PER-02-06).
 * @property birthDate Fecha de nacimiento, no posterior a hoy (CA-PER-02-07).
 * @property photoUrl URL de descarga de la foto de perfil, si tiene.
 * @property photoPath Ruta de la foto en Cloud Storage, necesaria para borrarla al cambiarla.
 */
data class UserProfile(
    val uid: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val birthDate: LocalDate? = null,
    val photoUrl: String? = null,
    val photoPath: String? = null,
)
