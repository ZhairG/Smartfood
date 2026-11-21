package com.smartfood.app.core.database

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

/**
 * Nombre: CurrentUid.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: El `uid` del usuario con sesión, con el que la base local filtra cada consulta (spec 014, RNF-LOC-02).
 * Las clases de datos dependen de esta interfaz y no de `FirebaseAuth`, para probarlas con un fake.
 */
fun interface CurrentUid {

    /** El `uid` con sesión, o `null` si no hay sesión. */
    fun get(): String?
}

/** [CurrentUid] desde Firebase Authentication. */
class FirebaseCurrentUid @Inject constructor(private val auth: FirebaseAuth) : CurrentUid {
    override fun get(): String? = auth.currentUser?.uid
}
