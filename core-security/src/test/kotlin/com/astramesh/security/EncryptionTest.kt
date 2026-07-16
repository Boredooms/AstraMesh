package com.astramesh.security

import com.astramesh.protocol.Packet
import com.astramesh.protocol.PacketType
import com.astramesh.protocol.payload.ChatPayload
import com.astramesh.protocol.payload.PacketPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptionTest {

    @Test
    fun ecdh_bothSides_deriveSameSessionKey() {
        val alice = KeyExchange.generateKeyPair()
        val bob = KeyExchange.generateKeyPair()

        val aliceView = KeyExchange.deriveSessionKey(alice.privateKey, bob.publicKey)
        val bobView = KeyExchange.deriveSessionKey(bob.privateKey, alice.publicKey)

        assertEquals(aliceView.keyBase64, bobView.keyBase64)
    }

    @Test
    fun differentPeers_deriveDifferentKeys() {
        val alice = KeyExchange.generateKeyPair()
        val bob = KeyExchange.generateKeyPair()
        val eve = KeyExchange.generateKeyPair()

        val ab = KeyExchange.deriveSessionKey(alice.privateKey, bob.publicKey)
        val ae = KeyExchange.deriveSessionKey(alice.privateKey, eve.publicKey)
        assertNotEquals(ab.keyBase64, ae.keyBase64)
    }

    @Test
    fun fingerprint_isStableAndDerivedFromPublicKey() {
        val kp = KeyExchange.generateKeyPair()
        assertEquals(kp.fingerprint, KeyExchange.fingerprintOf(kp.publicKey))
    }

    @Test
    fun aead_roundTrips() {
        val key = sharedKey()
        val message = "emergency: shelter at grid 12".toByteArray()
        val sealed = AeadCipher.seal(key, message)
        val opened = AeadCipher.open(key, sealed)
        assertEquals(String(message), String(opened))
    }

    @Test
    fun aead_producesFreshNoncePerCall() {
        val key = sharedKey()
        val a = AeadCipher.seal(key, "same".toByteArray())
        val b = AeadCipher.seal(key, "same".toByteArray())
        // Same plaintext must not yield identical ciphertext (nonce differs).
        assertNotEquals(a.nonce, b.nonce)
        assertNotEquals(a.ciphertext, b.ciphertext)
    }

    @Test
    fun aead_wrongKey_fails() {
        val sealed = AeadCipher.seal(sharedKey(), "secret".toByteArray())
        val wrong = KeyExchange.deriveSessionKey(
            KeyExchange.generateKeyPair().privateKey,
            KeyExchange.generateKeyPair().publicKey,
        )
        assertThrows(DecryptionException::class.java) {
            AeadCipher.open(wrong, sealed)
        }
    }

    @Test
    fun aead_tamperedCiphertext_fails() {
        val key = sharedKey()
        val sealed = AeadCipher.seal(key, "secret".toByteArray())
        val tampered = sealed.copy(
            ciphertext = flipFirstBase64Char(sealed.ciphertext),
        )
        assertThrows(DecryptionException::class.java) {
            AeadCipher.open(key, tampered)
        }
    }

    @Test
    fun aead_associatedData_mustMatch() {
        val key = sharedKey()
        val sealed = AeadCipher.seal(key, "body".toByteArray(), associatedData = "hdr-1".toByteArray())
        // Correct AAD opens.
        assertEquals("body", String(AeadCipher.open(key, sealed, "hdr-1".toByteArray())))
        // Wrong AAD fails.
        assertThrows(DecryptionException::class.java) {
            AeadCipher.open(key, sealed, "hdr-2".toByteArray())
        }
    }

    @Test
    fun messageCipher_sealsTypedPayloadIntoPacket_andOpens() {
        val key = sharedKey()
        val original: PacketPayload = ChatPayload(text = "meet at the ridge", replyToPacketId = "p0")

        val packet = Packet(
            packetId = "p1",
            type = PacketType.CHAT,
            senderId = "a",
            receiverId = "b",
            timestamp = 1L,
            ttl = 5,
            payload = "", // filled by encryptInto
        )
        val encrypted = MessageCipher.encryptInto(packet, key, original)

        // Payload on the wire is opaque, not the plaintext.
        assertTrue(encrypted.payload.isNotEmpty())
        assertTrue(!encrypted.payload.contains("meet at the ridge"))

        val opened = MessageCipher.decryptFrom(encrypted, key)
        assertEquals(original, opened)
    }

    @Test
    fun messageCipher_malformedPayload_throws() {
        assertThrows(DecryptionException::class.java) {
            MessageCipher.openPayload(sharedKey(), "no-separator-here")
        }
    }

    // --- helpers ---

    private fun sharedKey(): SessionKey {
        val alice = KeyExchange.generateKeyPair()
        val bob = KeyExchange.generateKeyPair()
        return KeyExchange.deriveSessionKey(alice.privateKey, bob.publicKey)
    }

    private fun flipFirstBase64Char(b64: String): String {
        val c = b64[0]
        val replacement = if (c == 'A') 'B' else 'A'
        return replacement + b64.substring(1)
    }
}
