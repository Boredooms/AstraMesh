package com.astramesh.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.astramesh.protocol.Packet
import com.astramesh.protocol.ProtocolJson
import com.astramesh.transport.LinkState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * GATT central (client) role of the BLE transport (docs/architecture.md §9) -- the sending
 * half. [BleGattServer] is the receiving half; see that class's doc comment for why both
 * roles run on every node.
 *
 * One [BluetoothGatt] connection per remote address is opened lazily and cached in
 * [connections] so repeated sends to the same peer don't pay a fresh connect + MTU
 * negotiation + service discovery cost every time (mirrors the `endpoints` caching pattern
 * already used in [BleTransport]). A per-address [Mutex] serializes the connect/write
 * sequence for that address, since [BluetoothGatt] callbacks are inherently async and a
 * second concurrent send to the same peer must not race the first one's chunk writes.
 */
@SuppressLint("MissingPermission")
class BleGattClient(private val context: Context) {

    private class Connection(val gatt: BluetoothGatt) {
        val mutex = Mutex()
        var mtu: Int = DEFAULT_ATT_MTU
        var ready = false
        var pendingWrite: CompletableDeferred<Boolean>? = null
        var pendingConnect: CompletableDeferred<Boolean>? = null
        var pendingMtu: CompletableDeferred<Int>? = null
    }

    private val connections = ConcurrentHashMap<String, Connection>()

    fun isAvailable(): Boolean = context.getSystemService(BluetoothManager::class.java) != null

    /**
     * Sends [packet] to the device at [address], opening/reusing a GATT connection.
     * [onLinkStateChanged] surfaces connection lifecycle to the caller so it can be forwarded
     * as [com.astramesh.transport.TransportEvent.LinkStateChanged].
     *
     * @return true only if every fragment of the packet was actually written and acknowledged
     *   by the remote GATT stack -- not merely handed to the OS.
     */
    suspend fun send(
        packet: Packet,
        address: String,
        onLinkStateChanged: (LinkState) -> Unit = {},
    ): Boolean {
        val connection = obtainConnection(address, onLinkStateChanged) ?: return false
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

    /** Returns a connected, MTU-negotiated [Connection] for [address], opening one if needed. */
    private suspend fun obtainConnection(
        address: String,
        onLinkStateChanged: (LinkState) -> Unit,
    ): Connection? {
        connections[address]?.let { existing ->
            if (existing.ready) return existing
            closeConnection(address)
        }

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
                    .onFailure { connectionRef?.pendingConnect?.takeIf { !it.isCompleted }?.complete(true) }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                // A rejected/failed MTU negotiation (some OEM stacks refuse it) is not fatal --
                // fall back to the default ATT MTU rather than failing the connection outright.
                connectionRef?.mtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else DEFAULT_ATT_MTU
                connectionRef?.pendingConnect?.takeIf { !it.isCompleted }?.complete(true)
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
        }

        val gatt = runCatching {
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        }.getOrNull() ?: run {
            onLinkStateChanged(LinkState.DISCONNECTED)
            return null
        }

        val connection = Connection(gatt).also { connectionRef = it }
        connection.pendingConnect = connectedDeferred
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
    }
}
