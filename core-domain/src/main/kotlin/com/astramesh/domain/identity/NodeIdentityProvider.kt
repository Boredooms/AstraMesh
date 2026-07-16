package com.astramesh.domain.identity

import com.astramesh.domain.model.Node

/**
 * Supplies this device's stable local identity (docs/protocol.md §5).
 *
 * Identity is generated locally on first launch and persists across restarts. No online
 * account is required. Implemented in the app layer (DataStore + generated keys).
 */
interface NodeIdentityProvider {
    /** This device's node id, stable across restarts. */
    suspend fun nodeId(): String

    /** The full local [Node] (id, name, public key, capabilities). */
    suspend fun localNode(): Node

    /** Base64 private key for session-key derivation (never transmitted). */
    suspend fun privateKey(): String
}
