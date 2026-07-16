package com.astramesh.mesh

import com.astramesh.security.KeyExchange
import com.astramesh.security.SessionKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Derives and caches per-peer session keys (docs/protocol.md §12).
 *
 * Given this node's private key and a peer's public key, ECDH yields a shared [SessionKey]
 * used to seal/open that peer's packet payloads. Keys are cached so we don't recompute the
 * agreement for every message.
 */
class SessionKeyManager(
    private val localPrivateKey: String,
) {
    private val cache = ConcurrentHashMap<String, SessionKey>()

    /** Returns the session key for [peerNodeId], deriving it from [peerPublicKey] if needed. */
    fun keyFor(peerNodeId: String, peerPublicKey: String): SessionKey? {
        if (peerPublicKey.isBlank()) return null
        return cache.getOrPut(peerNodeId) {
            KeyExchange.deriveSessionKey(localPrivateKey, peerPublicKey)
        }
    }

    fun clear() = cache.clear()
}
