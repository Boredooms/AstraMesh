package com.astramesh.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted node identity (docs/protocol.md §20 table `nodes`). */
@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val nodeId: String,
    val deviceName: String,
    val platformType: String,
    val publicKey: String,
    val keyFingerprint: String,
    /** Capabilities stored as a comma-separated list. */
    val capabilities: String,
    val relayCapable: Boolean,
    val lastSeen: Long,
    // Transient session/link state (nullable when only the identity is known).
    val sessionState: String? = null,
    val signalStrength: Int? = null,
    val lastContact: Long? = null,
)
