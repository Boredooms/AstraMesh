package com.astramesh.security

import com.astramesh.protocol.Packet
import com.astramesh.protocol.ProtocolJson
import com.astramesh.protocol.payload.PacketPayload

/**
 * Bridges the [PacketPayload] protocol layer and the [AeadCipher] security layer
 * (docs/protocol.md §12, docs/architecture.md §12).
 *
 * Sending: serialize the typed payload to JSON, seal it under the session key, and pack the
 * nonce + ciphertext into [Packet.payload]. Receiving: reverse it. Relay nodes only ever see
 * the opaque sealed string — they cannot read content.
 *
 * The sealed form stored in [Packet.payload] is `"<nonceB64>:<ciphertextB64>"`.
 */
object MessageCipher {

    private const val SEP = ":"

    /** Encrypts [payload] under [key] and returns the opaque string for [Packet.payload]. */
    fun sealPayload(key: SessionKey, payload: PacketPayload): String {
        val json = ProtocolJson.encodePayload(payload)
        val sealed = AeadCipher.seal(key, json.toByteArray(Charsets.UTF_8))
        return "${sealed.nonce}$SEP${sealed.ciphertext}"
    }

    /** Decrypts an opaque [Packet.payload] string under [key] back into a typed payload. */
    fun openPayload(key: SessionKey, sealedPayload: String): PacketPayload {
        val parts = sealedPayload.split(SEP, limit = 2)
        if (parts.size != 2) {
            throw DecryptionException("Malformed sealed payload")
        }
        val json = AeadCipher.open(key, SealedPayload(nonce = parts[0], ciphertext = parts[1]))
        return ProtocolJson.decodePayload(json.toString(Charsets.UTF_8))
    }

    /** Returns a copy of [packet] whose payload is the encrypted form of [payload]. */
    fun encryptInto(packet: Packet, key: SessionKey, payload: PacketPayload): Packet =
        packet.copy(payload = sealPayload(key, payload))

    /** Decrypts the payload carried by [packet]. */
    fun decryptFrom(packet: Packet, key: SessionKey): PacketPayload =
        openPayload(key, packet.payload)
}
