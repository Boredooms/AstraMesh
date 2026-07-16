package com.astramesh.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Persisted emergency broadcast (docs/protocol.md §20 table `broadcasts`). */
@Entity(
    tableName = "broadcasts",
    indices = [Index("packetId", unique = true)],
)
data class BroadcastEntity(
    @PrimaryKey val id: String,
    val packetId: String,
    val senderId: String,
    val text: String,
    val severity: String,
    val timestamp: Long,
    val expiresAt: Long? = null,
    val outgoing: Boolean,
    val hopCount: Int,
)
