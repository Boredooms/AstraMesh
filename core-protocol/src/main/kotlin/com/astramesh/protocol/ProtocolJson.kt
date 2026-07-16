package com.astramesh.protocol

import com.astramesh.protocol.payload.PacketPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Central serialization for the AstraMesh wire protocol.
 *
 * A single [Json] configuration is used everywhere so encoding is deterministic across
 * phones and the desktop companion. Unknown keys are ignored to preserve forward
 * compatibility with newer protocol versions (docs/protocol.md §4, §24).
 */
object ProtocolJson {

    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "kind"
        explicitNulls = false
    }

    // ---- Envelope ----

    fun encodePacket(packet: Packet): String = json.encodeToString(packet)

    fun decodePacket(text: String): Packet = json.decodeFromString(text)

    // ---- Typed payloads (the decrypted content inside Packet.payload) ----

    fun encodePayload(payload: PacketPayload): String = json.encodeToString(payload)

    fun decodePayload(text: String): PacketPayload = json.decodeFromString(text)
}
