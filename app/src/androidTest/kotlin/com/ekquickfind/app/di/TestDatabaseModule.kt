package com.ekquickfind.app.di

import android.content.Context
import androidx.room.Room
import com.ekquickfind.data.EkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Every instrumented test gets a fresh in-memory database, so the device owner's pins and history are never touched. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): EkDatabase = Room.inMemoryDatabaseBuilder(context, EkDatabase::class.java).build()
}
