package com.astramesh.security

/**
 * A node's asymmetric key material for the mesh handshake (docs/protocol.md §11–12).
 *
 * Keys are encoded as Base64 strings so they can travel inside protocol payloads and be
 * persisted locally without extra platform types. The private key never leaves the device.
 */
data class KeyPairMaterial(
    /** Base64 X.509-encoded public key (safe to share). */
    val publicKey: String,
    /** Base64 PKCS#8-encoded private key (kept local, never transmitted). */
    val privateKey: String,
    /** Short human-comparable fingerprint of the public key. */
    val fingerprint: String,
)

/** A derived symmetric session key shared between two nodes. */
data class SessionKey(
    /** Raw 32-byte AES key, Base64-encoded. */
    val keyBase64: String,
)

/** Result of AEAD encryption: ciphertext and the nonce needed to decrypt it. */
data class SealedPayload(
    /** Base64 ciphertext (includes the GCM auth tag). */
    val ciphertext: String,
    /** Base64 12-byte GCM nonce. */
    val nonce: String,
)

/** Raised when decryption fails: wrong key, tampered ciphertext, or bad nonce. */
class DecryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
