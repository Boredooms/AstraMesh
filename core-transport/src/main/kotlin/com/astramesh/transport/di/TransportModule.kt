package com.astramesh.transport.di

import com.astramesh.transport.Transport
import com.astramesh.transport.ble.BleTransport
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the default (BLE) [Transport] to the rest of the app (docs/architecture.md §9). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TransportModule {

    @Binds
    @Singleton
    abstract fun bindTransport(impl: BleTransport): Transport
}
