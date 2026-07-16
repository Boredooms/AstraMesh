package com.astramesh.app.di

import com.astramesh.app.identity.DataStoreNodeIdentity
import com.astramesh.domain.identity.NodeIdentityProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IdentityModule {

    @Binds
    @Singleton
    abstract fun bindNodeIdentity(impl: DataStoreNodeIdentity): NodeIdentityProvider
}
