package com.astramesh.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.astramesh.protocol.Packet
import com.astramesh.protocol.ProtocolJson
import com.astramesh.transport.LinkState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * GATT central (client) role of the BLE transport (docs/architecture.md §9) -- the role that
 * opens the connection. [BleGattServer] is the role that accepts it; see that class's doc
 * comment for why ONE connection now carries traffic in both directions.
 *
 * - [send] WRITES to [BleConstants.PACKET_CHARACTERISTIC_UUID] to deliver a packet TO the
 *   server side.
 * - On connect, this class also subscribes to [BleConstants.NOTIFY_CHARACTERISTIC_UUID] so
 *   the server side can push packets BACK over this SAME connection -- e.g. B's HELLO_ACK
 *   reply to A's HELLO arrives as a notification on the connection A opened, instead of
 *   requiring B to independently open a second connection back to A.
 *
 * One [BluetoothGatt] connection per remote address is cached in [connections] so repeated
 * sends to the same peer don't pay a fresh connect + MTU negotiation + service discovery +
 * subscribe cost every time. A per-address [Mutex] serializes the connect/write sequence,
 * since [BluetoothGatt] callbacks are inherently async.
 */
@SuppressLint("MissingPermission")
class BleGattClient(private val context: Context) {

    private class Connection(val gatt: BluetoothGatt) {
        val mutex = Mutex()
        var mtu: Int = DEFAULT_ATT_MTU
        var ready = false
        var pendingWrite: CompletableDeferred<Boolean>? = null
        var pendingConnect: CompletableDeferred<Boolean>? = null
        val reassembly = ByteArrayOutputStream()

        // Reused/cached connections must always dispatch incoming notifications to whichever
        // caller most recently sent over them, not just whoever happened to open the
        // connection originally -- otherwise a later send() from a different code path would
        // silently never see replies pushed back on this link.
        @Volatile var onPacketReceived: (Packet) -> Unit = {}
    }

    private val connections = ConcurrentHashMap<String, Connection>()

    fun isAvailable(): Boolean = context.getSystemService(BluetoothManager::class.java) != null

    /** True if this client currently holds a ready outbound connection to [address]. */
    fun isConnectedTo(address: String): Boolean = connections[address]?.ready == true

    /**
     * Sends [packet] to the device at [address], opening/reusing a GATT connection.
     * [onLinkStateChanged] surfaces connection lifecycle; [onPacketReceived] surfaces any
     * packet the server side pushes back over this same connection as a notification (see
     * class doc comment) -- callers forward both as [com.astramesh.transport.TransportEvent]s.
     *
     * @return true only if every fragment of the packet was actually written and acknowledged
     *   by the remote GATT stack -- not merely handed to the OS.
     */
    suspend fun send(
        packet: Packet,
        address: String,
        onLinkStateChanged: (LinkState) -> Unit = {},
        onPacketReceived: (Packet) -> Unit = {},
    ): Boolean {
        val connection = obtainConnection(address, onLinkStateChanged, onPacketReceived) ?: return false
        connection.onPacketReceived = onPacketReceived
        return connection.mutex.withLock {
            val characteristic = connection.gatt.getService(BleConstants.SERVICE_UUID)
                ?.getCharacteristic(BleConstants.PACKET_CHARACTERISTIC_UUID)
            if (characteristic == null) {
                Log.w(TAG, "Packet characteristic missing on $address; dropping stale connection")
                closeConnection(address)
                return@withLock false
            }

            val bytes = ProtocolJson.encodePacket(packet).toByteArray(Charsets.UTF_8)
            // Reserve 1 byte per chunk for the LAST_CHUNK_FLAG/CONTINUATION_FLAG framing byte
            // BleGattServer expects (see that class's doc comment).
            val chunkSize = (connection.mtu - ATT_WRITE_OVERHEAD - 1).coerceAtLeast(1)
            val chunks = bytes.toList().chunked(chunkSize)
            for ((index, chunk) in chunks.withIndex()) {
                val isLast = index == chunks.lastIndex
                val flag = if (isLast) BleGattServer.LAST_CHUNK_FLAG else BleGattServer.CONTINUATION_FLAG
                val framed = ByteArray(chunk.size + 1)
                framed[0] = flag
                chunk.forEachIndexed { i, b -> framed[i + 1] = b }

                val wrote = writeChunk(connection, characteristic, framed)
                if (!wrote) {
                    Log.w(TAG, "Chunk write failed to $address at index $index/${chunks.size}")
                    return@withLock false
                }
            }
            true
        }
    }

    /** Writes one framed chunk and suspends until the GATT stack confirms the write. */
    private suspend fun writeChunk(
        connection: Connection,
        characteristic: BluetoothGattCharacteristic,
        framed: ByteArray,
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        connection.pendingWrite = deferred
        @Suppress("DEPRECATION")
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        @Suppress("DEPRECATION")
        characteristic.value = framed
        @Suppress("DEPRECATION")
        val accepted = connection.gatt.writeCharacteristic(characteristic)
        if (!accepted) return false
        return withTimeoutOrNull(WRITE_TIMEOUT_MS) { deferred.await() } ?: false
    }

    /**
     * Returns a connected, MTU-negotiated, notification-subscribed [Connection] for
     * [address], opening one if needed.
     *
     * Retries the connect attempt up to [CONNECT_ATTEMPTS] times. The very first
     * `connectGatt()` to a device that was JUST discovered by an active scan very commonly
     * fails once on real Android hardware (frequently surfaced as GATT error 133,
     * `GATT_ERROR`) purely from radio/timing contention between the scanner and the new
     * connection attempt -- not a real, persistent failure. Retrying immediately after
     * closing the failed [BluetoothGatt] resolves this in the overwhelming majority of cases.
     */
    private suspend fun obtainConnection(
        address: String,
        onLinkStateChanged: (LinkState) -> Unit,
        onPacketReceived: (Packet) -> Unit,
    ): Connection? {
        connections[address]?.let { existing ->
            if (existing.ready) return existing
            closeConnection(address)
        }

        repeat(CONNECT_ATTEMPTS) { attempt ->
            val connection = attemptConnect(address, onLinkStateChanged, onPacketReceived)
            if (connection != null) return connection
            if (attempt < CONNECT_ATTEMPTS - 1) delay(CONNECT_RETRY_DELAY_MS)
        }
        return null
    }

    private suspend fun attemptConnect(
        address: String,
        onLinkStateChanged: (LinkState) -> Unit,
        onPacketReceived: (Packet) -> Unit,
    ): Connection? {
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return null
        val adapter = manager.adapter?.takeIf { it.isEnabled } ?: return null
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return null

        onLinkStateChanged(LinkState.CONNECTING)
        val connectedDeferred = CompletableDeferred<Boolean>()
        var connectionRef: Connection? = null

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        runCatching { gatt.discoverServices() }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectionRef?.pendingConnect?.takeIf { !it.isCompleted }?.complete(false)
                        connectionRef?.pendingWrite?.takeIf { !it.isCompleted }?.complete(false)
                        onLinkStateChanged(LinkState.DISCONNECTED)
                        connections.remove(address)
                        runCatching { gatt.close() }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    connectionRef?.pendingConnect?.takeIf { !it.isCompleted }?.complete(false)
                    return
                }
                runCatching { gatt.requestMtu(BleConstants.TARGET_MTU) }
                    .onFailure {
                        // MTU negotiation itself failed to even start -- proceed straight to
                        // subscribing rather than getting stuck waiting for onMtuChanged.
                        subscribeToNotifications(gatt, connectionRef, connectedDeferred)
                    }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                // A rejected/failed MTU negotiation (some OEM stacks refuse it) is not fatal --
                // fall back to the default ATT MTU rather than failing the connection outright.
                connectionRef?.mtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else DEFAULT_ATT_MTU
                subscribeToNotifications(gatt, connectionRef, connectedDeferred)
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                connectionRef?.pendingWrite?.takeIf { !it.isCompleted }
                    ?.complete(status == BluetoothGatt.GATT_SUCCESS)
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int,
            ) {
                // Notification subscription (CCCD write) confirmed one way or another --
                // the connection is usable for sending regardless of whether the subscribe
                // itself succeeded (a failed subscribe just means we won't get replies pushed
                // back on this link, not that outbound sends are broken).
                connectionRef?.pendingConnect?.takeIf { !it.isCompleted }?.complete(true)
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                if (characteristic.uuid != BleConstants.NOTIFY_CHARACTERISTIC_UUID) return
                val value = characteristic.value ?: return
                val conn = connectionRef
                handleNotifyChunk(conn, value) { packet -> conn?.onPacketReceived?.invoke(packet) }
            }
        }

        val gatt = runCatching {
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        }.getOrNull() ?: run {
            onLinkStateChanged(LinkState.DISCONNECTED)
            return null
        }

        val connection = Connection(gatt).also { connectionRef = it }
        connection.pendingConnect = connectedDeferred
        connection.onPacketReceived = onPacketReceived
        connections[address] = connection

        val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { connectedDeferred.await() } ?: false
        if (!connected) {
            closeConnection(address)
            onLinkStateChanged(LinkState.DISCONNECTED)
            return null
        }
        connection.ready = true
        onLinkStateChanged(LinkState.CONNECTED)
        return connection
    }

    /** Reassembles a fragmented notification into a full packet once the last chunk arrives. */
    private fun handleNotifyChunk(
        connection: Connection?,
        value: ByteArray,
        onPacketReceived: (Packet) -> Unit,
    ) {
        if (connection == null || value.isEmpty()) return
        val isLastChunk = value[0] == BleGattServer.LAST_CHUNK_FLAG
        val chunkData = value.copyOfRange(1, value.size)
        connection.reassembly.write(chunkData)
        if (!isLastChunk) return

        val bytes = connection.reassembly.toByteArray()
        connection.reassembly.reset()
        val packet = runCatching { ProtocolJson.decodePacket(String(bytes, Charsets.UTF_8)) }
            .onFailure { Log.w(TAG, "Dropping malformed notify packet", it) }
            .getOrNull() ?: return
        onPacketReceived(packet)
    }

    /** Enables notifications both locally (setCharacteristicNotification) and on the remote CCCD. */
    @Suppress("DEPRECATION")
    private fun subscribeToNotifications(
        gatt: BluetoothGatt,
        connection: Connection?,
        connectedDeferred: CompletableDeferred<Boolean>,
    ) {
        val characteristic = gatt.getService(BleConstants.SERVICE_UUID)
            ?.getCharacteristic(BleConstants.NOTIFY_CHARACTERISTIC_UUID)
        if (characteristic == null) {
            // No notify characteristic (unexpected/older server) -- connection is still usable
            // for outbound sends, just without a return path on this link.
            connectedDeferred.takeIf { !it.isCompleted }?.complete(true)
            return
        }
        val subscribed = runCatching { gatt.setCharacteristicNotification(characteristic, true) }
            .getOrDefault(false)
        val descriptor = characteristic.getDescriptor(BleConstants.CCCD_UUID)
        if (!subscribed || descriptor == null) {
            connectedDeferred.takeIf { !it.isCompleted }?.complete(true)
            return
        }
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val wrote = runCatching { gatt.writeDescriptor(descriptor) }.getOrDefault(false)
        if (!wrote) {
            connectedDeferred.takeIf { !it.isCompleted }?.complete(true)
        }
        // If the write was accepted, onDescriptorWrite completes connectedDeferred instead.
    }

    private fun closeConnection(address: String) {
        connections.remove(address)?.let { runCatching { it.gatt.close() } }
    }

    /** Closes every cached connection. Call from [BleTransport.stop]. */
    fun stopAll() {
        connections.keys.toList().forEach { closeConnection(it) }
    }

    companion object {
        private const val TAG = "BleGattClient"

        /** Default ATT MTU before any negotiation (23 bytes, minus 3 bytes ATT header = 20 usable). */
        private const val DEFAULT_ATT_MTU = 23

        /** ATT protocol overhead subtracted from the negotiated MTU to get usable payload bytes. */
        private const val ATT_WRITE_OVERHEAD = 3

        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val WRITE_TIMEOUT_MS = 10_000L

        /** Total connectGatt() attempts per send before giving up (see obtainConnection doc). */
        private const val CONNECT_ATTEMPTS = 3

        /** Delay before retrying a failed connect attempt. */
        private const val CONNECT_RETRY_DELAY_MS = 800L
    }
}
