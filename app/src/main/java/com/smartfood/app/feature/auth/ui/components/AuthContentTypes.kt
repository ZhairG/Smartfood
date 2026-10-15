package com.smartfood.app.feature.auth.ui.components

import androidx.compose.ui.autofill.ContentType

/**
 * Nombre: AuthContentTypes.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Tipos de autocompletado de los formularios de autenticación (spec 001, HU-07). El correo es a la vez
 * el usuario de la cuenta y una dirección de correo. Se declara una sola vez porque cada `+` crea un objeto nuevo y
 * las pruebas comparan el tipo del campo con esta misma instancia.
 */
val EmailAccountContentType: ContentType = ContentType.Username + ContentType.EmailAddress
