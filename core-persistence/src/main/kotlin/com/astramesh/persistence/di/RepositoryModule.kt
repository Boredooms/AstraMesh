package com.astramesh.persistence.di

import com.astramesh.domain.repository.BroadcastRepository
import com.astramesh.domain.repository.FileTransferRepository
import com.astramesh.domain.repository.MessageRepository
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.domain.repository.RelayQueueRepository
import com.astramesh.persistence.repository.RoomBroadcastRepository
import com.astramesh.persistence.repository.RoomFileTransferRepository
import com.astramesh.persistence.repository.RoomMessageRepository
import com.astramesh.persistence.repository.RoomPeerRepository
import com.astramesh.persistence.repository.RoomRelayQueueRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the Room-backed repository implementations to their domain interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: RoomMessageRepository): MessageRepository

    @Binds
    @Singleton
    abstract fun bindPeerRepository(impl: RoomPeerRepository): PeerRepository

    @Binds
    @Singleton
    abstract fun bindBroadcastRepository(impl: RoomBroadcastRepository): BroadcastRepository

    @Binds
    @Singleton
    abstract fun bindFileTransferRepository(impl: RoomFileTransferRepository): FileTransferRepository

    @Binds
    @Singleton
    abstract fun bindRelayQueueRepository(impl: RoomRelayQueueRepository): RelayQueueRepository
}
