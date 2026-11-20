package com.smartfood.app.core.database

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Nombre: DatabaseModule.kt
 * Autor: SmartFood Team
 * Fecha: Noviembre 2026
 * Descripción: Módulo Hilt de la base local (spec 014, plan §2): la base Room, sus DAO, el alcance de la app y
 * [LocalUserData].
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartFoodDatabase =
        Room.databaseBuilder(context, SmartFoodDatabase::class.java, SmartFoodDatabase.NAME).build()

    @Provides
    fun provideCookingDao(database: SmartFoodDatabase): CookingDao = database.cookingDao()

    @Provides
    fun provideRecentSearchDao(database: SmartFoodDatabase): RecentSearchDao = database.recentSearchDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

/** Enlaces de las interfaces de la base local. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindingsModule {

    @Binds
    @Singleton
    abstract fun bindLocalUserData(impl: RoomLocalUserData): LocalUserData

    @Binds
    abstract fun bindCurrentUid(impl: FirebaseCurrentUid): CurrentUid
}
