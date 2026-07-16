package com.astramesh.security

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.KeyAgreement

/**
 * Elliptic-curve Diffie–Hellman key exchange (docs/protocol.md §11–12).
 *
 * Uses NIST P-256 (`secp256r1`), which is available on every Android API level this app
 * targets (minSdk 26) and on the JVM desktop companion. Two nodes exchange public keys during
 * the handshake, then each derives the same 256-bit [SessionKey] locally — the shared secret
 * is never transmitted.
 *
 * This object is pure JVM crypto with no Android dependencies, so it is unit-testable here.
 */
object KeyExchange {

    private const val KEY_ALGORITHM = "EC"
    private const val CURVE = "secp256r1"
    private const val AGREEMENT = "ECDH"

    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    /** Generates a fresh EC key pair for this node identity. */
    fun generateKeyPair(): KeyPairMaterial {
        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM).apply {
            initialize(java.security.spec.ECGenParameterSpec(CURVE))
        }
        val pair = generator.generateKeyPair()
        val publicB64 = encoder.encodeToString(pair.public.encoded)
        val privateB64 = encoder.encodeToString(pair.private.encoded)
        return KeyPairMaterial(
            publicKey = publicB64,
            privateKey = privateB64,
            fingerprint = fingerprintOf(publicB64),
        )
    }

    /**
     * Derives the shared 256-bit session key from this node's [privateKeyB64] and the peer's
     * [peerPublicKeyB64]. Both sides compute the identical key. The shared secret is hashed
     * with SHA-256 to produce a uniform AES key.
     */
    fun deriveSessionKey(privateKeyB64: String, peerPublicKeyB64: String): SessionKey {
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        val privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(decoder.decode(privateKeyB64))
        )
        val peerPublic = keyFactory.generatePublic(
            X509EncodedKeySpec(decoder.decode(peerPublicKeyB64))
        )
        val agreement = KeyAgreement.getInstance(AGREEMENT).apply {
            init(privateKey)
            doPhase(peerPublic, true)
        }
        val sharedSecret = agreement.generateSecret()
        val aesKey = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        return SessionKey(encoder.encodeToString(aesKey))
    }

    /** Short, human-comparable fingerprint (first 8 bytes of SHA-256, hex) of a public key. */
    fun fingerprintOf(publicKeyB64: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(decoder.decode(publicKeyB64))
        return digest.take(8).joinToString(":") { "%02x".format(it) }
    }
}
