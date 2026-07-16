package com.astramesh.persistence.di

import android.content.Context
import androidx.room.Room
import com.astramesh.persistence.AstraMeshDatabase
import com.astramesh.persistence.dao.BroadcastDao
import com.astramesh.persistence.dao.FileTransferDao
import com.astramesh.persistence.dao.MessageDao
import com.astramesh.persistence.dao.NodeDao
import com.astramesh.persistence.dao.RelayQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AstraMeshDatabase =
        Room.databaseBuilder(context, AstraMeshDatabase::class.java, AstraMeshDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNodeDao(db: AstraMeshDatabase): NodeDao = db.nodeDao()

    @Provides
    fun provideMessageDao(db: AstraMeshDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideBroadcastDao(db: AstraMeshDatabase): BroadcastDao = db.broadcastDao()

    @Provides
    fun provideFileTransferDao(db: AstraMeshDatabase): FileTransferDao = db.fileTransferDao()

    @Provides
    fun provideRelayQueueDao(db: AstraMeshDatabase): RelayQueueDao = db.relayQueueDao()
}
