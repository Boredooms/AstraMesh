package com.astramesh.mesh.di

import com.astramesh.domain.identity.NodeIdentityProvider
import com.astramesh.domain.repository.MessageRepository
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.mesh.MeshCoordinator
import com.astramesh.mesh.SessionKeyManager
import com.astramesh.routing.EpidemicRoutingEngine
import com.astramesh.routing.RoutingEngine
import com.astramesh.transport.Transport
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the process-lifetime [CoroutineScope] the mesh's long-lived collectors run on. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MeshScope

/**
 * Wires the pure-Kotlin mesh engines to their Android-scoped singletons (docs/architecture.md
 * §3, §14). [MeshCoordinator] itself has no Android dependencies, so only its collaborators
 * (transport, repositories, identity) need Hilt bindings from other modules.
 */
@Module
@InstallIn(SingletonComponent::class)
object MeshModule {

    @Provides
    @Singleton
    fun provideRoutingEngine(): RoutingEngine = EpidemicRoutingEngine()

    @Provides
    @Singleton
    @MeshScope
    fun provideMeshScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideSessionKeyManager(identity: NodeIdentityProvider): SessionKeyManager {
        // Identity is generated once on first launch and persists locally; reading the private
        // key here is a one-time synchronous bootstrap of a process-lifetime singleton.
        val privateKey = runBlocking { identity.privateKey() }
        return SessionKeyManager(privateKey)
    }

    @Provides
    @Singleton
    fun provideMeshCoordinator(
        transport: Transport,
        routing: RoutingEngine,
        messages: MessageRepository,
        peers: PeerRepository,
        identity: NodeIdentityProvider,
        sessionKeys: SessionKeyManager,
        @MeshScope scope: CoroutineScope,
    ): MeshCoordinator {
        val coordinator = MeshCoordinator(
            transport = transport,
            routing = routing,
            messages = messages,
            peers = peers,
            identity = identity,
            sessionKeys = sessionKeys,
        )
        coordinator.start(scope)
        return coordinator
    }
}
