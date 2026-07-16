package com.astramesh.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A queued, still-encrypted packet awaiting a route (docs/protocol.md §20 table
 * `packet_cache`). The full packet envelope is stored as JSON so it can be re-decoded and
 * forwarded byte-for-byte once a route appears — this node never decrypts [payloadJson].
 */
@Entity(tableName = "relay_queue")
data class RelayQueueEntity(
    @PrimaryKey val packetId: String,
    val payloadJson: String,
    val queuedAt: Long,
)
