package com.astramesh.app.identity

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.astramesh.domain.identity.NodeIdentityProvider
import com.astramesh.domain.model.Capability
import com.astramesh.domain.model.Node
import com.astramesh.domain.model.PlatformType
import com.astramesh.security.KeyExchange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.identityStore by preferencesDataStore(name = "astramesh_identity")

/**
 * DataStore-backed [NodeIdentityProvider]. On first launch it generates a node id and an EC key
 * pair, then persists them so identity is stable across restarts (docs/protocol.md §5).
 */
@Singleton
class DataStoreNodeIdentity @Inject constructor(
    @ApplicationContext private val context: Context,
) : NodeIdentityProvider {

    override suspend fun nodeId(): String = ensure().nodeId

    override suspend fun localNode(): Node {
        val i = ensure()
        return Node(
            nodeId = i.nodeId,
            deviceName = i.deviceName,
            platformType = PlatformType.ANDROID,
            publicKey = i.publicKey,
            keyFingerprint = i.fingerprint,
            capabilities = setOf(
                Capability.CHAT,
                Capability.RELAY,
                Capability.BROADCAST,
                Capability.FILE_TRANSFER,
            ),
            lastSeen = System.currentTimeMillis(),
            relayCapable = true,
        )
    }

    override suspend fun privateKey(): String = ensure().privateKey

    private data class Identity(
        val nodeId: String,
        val deviceName: String,
        val publicKey: String,
        val privateKey: String,
        val fingerprint: String,
    )

    private suspend fun ensure(): Identity {
        val prefs = context.identityStore.data.first()
        val existingId = prefs[KEY_NODE_ID]
        if (existingId != null) {
            return Identity(
                nodeId = existingId,
                deviceName = prefs[KEY_NAME] ?: existingId.take(8),
                publicKey = prefs[KEY_PUBLIC] ?: "",
                privateKey = prefs[KEY_PRIVATE] ?: "",
                fingerprint = prefs[KEY_FINGERPRINT] ?: "",
            )
        }
        val keys = KeyExchange.generateKeyPair()
        val id = "node-" + UUID.randomUUID().toString().take(8)
        val name = "AstraMesh-" + id.takeLast(4)
        context.identityStore.edit { e ->
            e[KEY_NODE_ID] = id
            e[KEY_NAME] = name
            e[KEY_PUBLIC] = keys.publicKey
            e[KEY_PRIVATE] = keys.privateKey
            e[KEY_FINGERPRINT] = keys.fingerprint
        }
        return Identity(id, name, keys.publicKey, keys.privateKey, keys.fingerprint)
    }

    private companion object {
        val KEY_NODE_ID = stringPreferencesKey("node_id")
        val KEY_NAME = stringPreferencesKey("device_name")
        val KEY_PUBLIC = stringPreferencesKey("public_key")
        val KEY_PRIVATE = stringPreferencesKey("private_key")
        val KEY_FINGERPRINT = stringPreferencesKey("fingerprint")
    }
}
