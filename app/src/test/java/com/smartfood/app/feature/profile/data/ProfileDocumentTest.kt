package com.smartfood.app.feature.profile.data

import com.smartfood.app.feature.profile.data.repository.ProfileDocument
import com.smartfood.app.feature.profile.data.repository.profileFromDocument
import com.smartfood.app.feature.profile.data.repository.toUpdateFields
import com.smartfood.app.feature.profile.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Nombre: ProfileDocumentTest.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pruebas unitarias de la conversión entre [UserProfile] y `users/{uid}` (spec 008, plan §2.1, T002).
 */
class ProfileDocumentTest {

    private val borrar = Any()
    private val ahora = Any()

    @Test
    fun `RF-PER-01 el perfil completo se lee del documento`() {
        val perfil = profileFromDocument(
            "u1",
            mapOf(
                "fullName" to "Pepito Perez", "email" to "pepito@correo.com", "phone" to "3001234567",
                "birthDate" to "2000-05-17", "photoPath" to "users/u1/profile/a.jpg", "photoUrl" to "https://foto",
                "termsVersion" to "1.0",
            )
        )
        assertEquals(
            UserProfile("u1", "Pepito Perez", "pepito@correo.com", "3001234567", LocalDate.of(2000, 5, 17), "https://foto", "users/u1/profile/a.jpg"),
            perfil
        )
    }

    @Test
    fun `RF-PER-01 un perfil del registro sin campos nuevos se lee sin errores y los datos mal formados se ignoran`() {
        assertEquals(UserProfile("u1", "Ana", "ana@correo.com"), profileFromDocument("u1", mapOf("fullName" to "Ana", "email" to "ana@correo.com")))

        val raro = profileFromDocument("u1", mapOf("fullName" to "Ana", "birthDate" to "17/05/2000", "photoPath" to "solo-ruta"))
        assertNull(raro.birthDate)
        assertNull("la foto necesita ruta y URL", raro.photoUrl)
    }

    @Test
    fun `RNF-PER-01 al editar se escriben solo los campos del perfil y lo vacio se borra`() {
        val campos = UserProfile("u1", "Ana", "ana@correo.com").toUpdateFields(borrar, ahora)

        assertEquals(setOf("fullName", "phone", "birthDate", "updatedAt"), campos.keys)
        assertEquals(borrar, campos[ProfileDocument.PHONE])
        assertEquals(borrar, campos[ProfileDocument.BIRTH_DATE])
        assertEquals(ahora, campos[ProfileDocument.UPDATED_AT])
        assertFalse("el correo no se escribe aquí", ProfileDocument.EMAIL in campos)
    }

    @Test
    fun `RF-PER-01 la foto y la fecha se escriben cuando existen`() {
        val campos = UserProfile("u1", "Ana", "a@b.co", "3001234567", LocalDate.of(1999, 1, 2), "https://f", "users/u1/profile/f.jpg")
            .toUpdateFields(borrar, ahora)

        assertEquals("3001234567", campos[ProfileDocument.PHONE])
        assertEquals("1999-01-02", campos[ProfileDocument.BIRTH_DATE])
        assertEquals("users/u1/profile/f.jpg", campos[ProfileDocument.PHOTO_PATH])
        assertEquals("https://f", campos[ProfileDocument.PHOTO_URL])
    }
}
