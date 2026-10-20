package com.smartfood.app.feature.profile.data.di

import com.smartfood.app.feature.profile.data.local.DataStoreNotificationSettings
import com.smartfood.app.feature.profile.data.repository.FirebaseStorageProfilePhotoRepository
import com.smartfood.app.feature.profile.data.repository.RemoteEmailChangeRepository
import com.smartfood.app.feature.profile.data.repository.FirestoreProfileRepository
import com.smartfood.app.feature.profile.domain.repository.EmailChangeRepository
import com.smartfood.app.feature.profile.domain.repository.NotificationSettingsRepository
import com.smartfood.app.feature.profile.domain.repository.ProfilePhotoRepository
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

    /** Foto de perfil en Cloud Storage (RF-PER-02, T006). */
    @Binds
    @Singleton
    abstract fun bindProfilePhotoRepository(impl: FirebaseStorageProfilePhotoRepository): ProfilePhotoRepository

    /** Cambio de correo con código por las Cloud Functions (RF-PER-03, T009b). */
    @Binds
    @Singleton
    abstract fun bindEmailChangeRepository(impl: RemoteEmailChangeRepository): EmailChangeRepository

    /** Ajustes de notificaciones guardados en el teléfono (RF-PER-05, T014). */
    @Binds
    @Singleton
    abstract fun bindNotificationSettingsRepository(
        impl: DataStoreNotificationSettings
    ): NotificationSettingsRepository
}
