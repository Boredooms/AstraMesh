package com.astramesh.transport.ble

import android.content.Context
import com.astramesh.protocol.Packet
import com.astramesh.transport.PeerEndpoint
import com.astramesh.transport.Transport
import com.astramesh.transport.TransportEvent
import com.astramesh.transport.TransportKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Bluetooth LE implementation of [Transport] (docs/architecture.md §9).
 *
 * Discovery, packet send, and packet receive are all implemented. Every node runs a
 * [BleGattServer] (receiving role) and a [BleGattClient] (sending role) simultaneously --
 * see [BleGattServer]'s doc comment for why there's no fixed central/peripheral split.
 * [send] opens/reuses a GATT client connection to the target address, fragments the
 * JSON-encoded packet to the negotiated MTU, and returns true only once every fragment is
 * actually written and acknowledged by the remote stack.
 */
class BleTransport @Inject constructor(
    @ApplicationContext private val context: Context,
) : Transport {

    override val kind: TransportKind = TransportKind.BLE

    private val advertiser = BleAdvertiser(context)
    private val scanner = BleScanner(context)
    private val gattServer = BleGattServer(context)
    private val gattClient = BleGattClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    private val endpoints = ConcurrentHashMap<String, PeerEndpoint>()
    private val lastSeenAt = ConcurrentHashMap<String, Long>()
    private var selfNodeId: String? = null
    private var maintenanceJob: Job? = null

    override fun isAvailable(): Boolean = advertiser.isAvailable() || scanner.isAvailable()

    override suspend fun start(selfNodeId: String) {
        this.selfNodeId = selfNodeId
        startRadios(selfNodeId)
        gattServer.start(
            onPacket = { packet, endpoint ->
                scope.launch { _events.emit(TransportEvent.PacketReceived(packet, endpoint)) }
            },
            endpointFor = { address -> endpoints.values.firstOrNull { it.address == address } },
            onError = { message ->
                scope.launch { _events.emit(TransportEvent.Error(message)) }
            },
        )
        maintenanceJob?.cancel()
        maintenanceJob = scope.launch { runMaintenanceLoop() }
    }

    private fun startRadios(selfNodeId: String) {
        advertiser.start(selfNodeId, onError = { message ->
            scope.launch { _events.emit(TransportEvent.Error(message)) }
        })
        scanner.start(
            onPeer = { endpoint ->
                if (endpoint.nodeId == selfNodeId) return@start
                lastSeenAt[endpoint.nodeId] = System.currentTimeMillis()
                val isNew = endpoints.put(endpoint.nodeId, endpoint) == null
                if (isNew) {
                    scope.launch { _events.emit(TransportEvent.PeerDiscovered(endpoint)) }
                }
            },
            onError = { message ->
                scope.launch { _events.emit(TransportEvent.Error(message)) }
            },
        )
    }

    /**
     * Periodically restarts the scan and expires peers not seen recently.
     *
     * Two independent problems this solves:
     * 1. Some OEM Bluetooth stacks silently throttle or stop delivering scan callbacks after
     *    running for a while, even though the scan is technically still "active" — there's no
     *    error, results just stop arriving. A cheap, reliable fix is to periodically stop and
     *    restart the scan rather than trusting one long-lived session.
     * 2. Without ever emitting [TransportEvent.PeerLost], a peer that walked out of range (or
     *    closed the app) stays listed as "nearby" forever — nothing ever removed it.
     */
    private suspend fun runMaintenanceLoop() {
        while (scope.isActive) {
            delay(RESCAN_INTERVAL_MS)
            val cutoff = System.currentTimeMillis() - STALE_PEER_TIMEOUT_MS
            val stale = lastSeenAt.filterValues { it < cutoff }.keys
            for (nodeId in stale) {
                endpoints.remove(nodeId)
                lastSeenAt.remove(nodeId)
                _events.emit(TransportEvent.PeerLost(nodeId))
            }

            val self = selfNodeId ?: continue
            scanner.stop()
            scanner.start(
                onPeer = { endpoint ->
                    if (endpoint.nodeId == self) return@start
                    lastSeenAt[endpoint.nodeId] = System.currentTimeMillis()
                    val isNew = endpoints.put(endpoint.nodeId, endpoint) == null
                    if (isNew) {
                        scope.launch { _events.emit(TransportEvent.PeerDiscovered(endpoint)) }
                    }
                },
                onError = { message ->
                    scope.launch { _events.emit(TransportEvent.Error(message)) }
                },
            )
        }
    }

    override suspend fun stop() {
        maintenanceJob?.cancel()
        maintenanceJob = null
        scanner.stop()
        advertiser.stop()
        gattServer.stop()
        gattClient.stopAll()
        endpoints.clear()
        lastSeenAt.clear()
        selfNodeId = null
    }

    override suspend fun send(packet: Packet, endpoint: PeerEndpoint): Boolean {
        // If this peer already has an OPEN connection to our GATT server (they connected to
        // us first, e.g. to deliver a HELLO), push the reply over that same connection via a
        // notification instead of opening a second, independent outbound connection. Needing
        // a second connection -- which itself depends on this side having already scanned and
        // resolved the peer's MAC address -- was the root cause of handshakes/messages
        // completing on only one side (see BleGattServer's doc comment).
        if (gattServer.isConnectedTo(endpoint.address)) {
            val sent = gattServer.sendTo(endpoint.address, packet)
            if (sent) return true
            // Fall through to a fresh outbound connection if the existing inbound link can't
            // carry the notification (e.g. the peer never subscribed).
        }

        val sent = gattClient.send(
            packet = packet,
            address = endpoint.address,
            onLinkStateChanged = { state ->
                scope.launch { _events.emit(TransportEvent.LinkStateChanged(endpoint.nodeId, state)) }
            },
            onPacketReceived = { received ->
                scope.launch { _events.emit(TransportEvent.PacketReceived(received, endpoint)) }
            },
        )
        if (!sent) {
            _events.emit(TransportEvent.Error("BLE send to ${endpoint.nodeId} failed"))
        }
        return sent
    }

    override fun knownEndpoints(): List<PeerEndpoint> = endpoints.values.toList()

    private companion object {
        /** How often to restart the scan, to counter OEM callback throttling. */
        const val RESCAN_INTERVAL_MS = 20_000L

        /** A peer not re-seen within this window is considered out of range and removed. */
        const val STALE_PEER_TIMEOUT_MS = 45_000L
    }
}
