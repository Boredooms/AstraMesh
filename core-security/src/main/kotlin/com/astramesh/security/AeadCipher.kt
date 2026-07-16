package com.astramesh.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Authenticated encryption using AES-256-GCM (docs/protocol.md §12).
 *
 * GCM provides confidentiality **and** integrity: any tampering with the ciphertext, nonce,
 * or associated data makes [open] throw [DecryptionException]. A fresh random 12-byte nonce is
 * generated per message and returned alongside the ciphertext.
 *
 * Pure JVM crypto (`javax.crypto`), available on Android 26+ and the desktop companion.
 */
object AeadCipher {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val NONCE_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    private val random = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    /**
     * Encrypts [plaintext] under [key]. [associatedData] (e.g. routing header bytes) is
     * authenticated but not encrypted; the same value must be supplied to [open].
     */
    fun seal(
        key: SessionKey,
        plaintext: ByteArray,
        associatedData: ByteArray? = null,
    ): SealedPayload {
        val nonce = ByteArray(NONCE_LENGTH).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.ENCRYPT_MODE,
                secretKey(key),
                GCMParameterSpec(TAG_LENGTH_BITS, nonce),
            )
            associatedData?.let(::updateAAD)
        }
        val ciphertext = cipher.doFinal(plaintext)
        return SealedPayload(
            ciphertext = encoder.encodeToString(ciphertext),
            nonce = encoder.encodeToString(nonce),
        )
    }

    /**
     * Decrypts a [SealedPayload] under [key]. Throws [DecryptionException] if the key is wrong,
     * the ciphertext/nonce was altered, or [associatedData] does not match what was sealed.
     */
    fun open(
        key: SessionKey,
        sealed: SealedPayload,
        associatedData: ByteArray? = null,
    ): ByteArray {
        return try {
            val nonce = decoder.decode(sealed.nonce)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    secretKey(key),
                    GCMParameterSpec(TAG_LENGTH_BITS, nonce),
                )
                associatedData?.let(::updateAAD)
            }
            cipher.doFinal(decoder.decode(sealed.ciphertext))
        } catch (e: Exception) {
            throw DecryptionException("Failed to open sealed payload", e)
        }
    }

    private fun secretKey(key: SessionKey) =
        SecretKeySpec(decoder.decode(key.keyBase64), "AES")
}
