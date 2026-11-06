package com.smartfood.app.feature.recipes.data.mapper

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeError
import com.smartfood.app.feature.recipes.domain.model.OwnRecipeException

/**
 * Nombre: OwnRecipeErrorMapper.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Traduce errores de Firebase a errores de recetas propias (spec 010, T009).
 */
object OwnRecipeErrorMapper {
    fun map(throwable: Throwable): OwnRecipeError = when (throwable) {
        is OwnRecipeException -> throwable.error
        is FirebaseNetworkException -> OwnRecipeError.Network
        is FirebaseFirestoreException -> when (throwable.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> OwnRecipeError.Network
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> OwnRecipeError.PermissionDenied
            FirebaseFirestoreException.Code.NOT_FOUND -> OwnRecipeError.NotFound
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> OwnRecipeError.NotAuthenticated
            else -> OwnRecipeError.Unknown(throwable.code.name)
        }
        else -> OwnRecipeError.Unknown(throwable.message)
    }

    fun toException(throwable: Throwable): OwnRecipeException =
        throwable as? OwnRecipeException ?: OwnRecipeException(map(throwable))
}

fun <T> Result<T>.mapOwnRecipeError(): Result<T> =
    exceptionOrNull()?.let { Result.failure(OwnRecipeErrorMapper.toException(it)) } ?: this
