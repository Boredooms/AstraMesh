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
 * Discovery is fully implemented: this transport advertises the local node and scans for peers,
 * emitting [TransportEvent.PeerDiscovered] as nodes appear. Connected GATT packet exchange is
 * the deeper part of Stage 4 and is marked below as an explicit future stub — it does not block
 * discovery, which is the Stage 3 deliverable.
 */
class BleTransport @Inject constructor(
    @ApplicationContext private val context: Context,
) : Transport {

    override val kind: TransportKind = TransportKind.BLE

    private val advertiser = BleAdvertiser(context)
    private val scanner = BleScanner(context)
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
        endpoints.clear()
        lastSeenAt.clear()
        selfNodeId = null
    }

    override suspend fun send(packet: Packet, endpoint: PeerEndpoint): Boolean {
        // FUTURE STUB (Stage 4): open a GATT connection to endpoint.address, write the packet
        // bytes to BleConstants.PACKET_CHARACTERISTIC_UUID with fragmentation to TARGET_MTU, and
        // await the notification-based ACK. Discovery (Stage 3) does not depend on this path.
        _events.emit(
            TransportEvent.Error("BLE GATT send not yet implemented (Stage 4)")
        )
        return false
    }

    override fun knownEndpoints(): List<PeerEndpoint> = endpoints.values.toList()

    private companion object {
        /** How often to restart the scan, to counter OEM callback throttling. */
        const val RESCAN_INTERVAL_MS = 20_000L

        /** A peer not re-seen within this window is considered out of range and removed. */
        const val STALE_PEER_TIMEOUT_MS = 45_000L
    }
}
