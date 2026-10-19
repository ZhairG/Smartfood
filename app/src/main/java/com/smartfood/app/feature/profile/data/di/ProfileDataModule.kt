package com.smartfood.app.feature.profile.data.di

import com.smartfood.app.feature.profile.data.repository.FirestoreProfileRepository
import com.smartfood.app.feature.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Nombre: ProfileDataModule.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Enlaza los contratos del dominio del Perfil con sus implementaciones (spec 008, plan §1). Cada Issue
 * enlaza aquí lo que crea: el perfil (#109, T002), la foto (#109, T006), las preferencias (#110) y las notificaciones
 * (#111).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileDataModule {

    /** Perfil del usuario en Firestore (RF-PER-01, T002). */
    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: FirestoreProfileRepository): ProfileRepository
}
