package com.astramesh.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.astramesh.protocol.Packet
import com.astramesh.protocol.ProtocolJson
import com.astramesh.transport.PeerEndpoint
import com.astramesh.transport.TransportKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * GATT peripheral (server) role of the BLE transport (docs/architecture.md §9).
 *
 * Every AstraMesh node runs both a GATT server (this class) and a GATT client
 * ([BleGattClient]) simultaneously -- there's no fixed central/peripheral split at the app
 * level. But unlike an earlier version of this transport, ONE connection now carries traffic
 * in BOTH directions:
 *
 * - The connecting side (client) WRITES to [BleConstants.PACKET_CHARACTERISTIC_UUID] to
 *   deliver a packet TO the server side (handled by [onCharacteristicWriteRequest]).
 * - The server side NOTIFIES on [BleConstants.NOTIFY_CHARACTERISTIC_UUID] to push a packet
 *   BACK to the connected client, over that SAME connection (see [sendTo]).
 *
 * This means whichever phone happens to open the connection (e.g. to deliver a HELLO) does
 * not need the other phone to independently scan, find its MAC address, and open a second
 * connection just to send the reply -- that requirement was the root cause of handshakes
 * completing on only one side: the reply's independent connection attempt depended on radio
 * timing / scan state on the replying phone that was never guaranteed to be ready.
 *
 * ## Fragmentation
 * Same chunk framing as before: `[1 flag byte][chunk bytes]`, [LAST_CHUNK_FLAG] marking the
 * final chunk. Both writes (inbound) and notifications (outbound) use this framing so
 * [BleGattClient]'s reassembly logic on the notify side mirrors this class's reassembly logic
 * on the write side.
 */
@SuppressLint("MissingPermission")
class BleGattServer(private val context: Context) {

    private var gattServer: BluetoothGattServer? = null
    private val reassembly = ConcurrentHashMap<String, ByteArrayOutputStream>()
    private val connectedDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val deviceMtu = ConcurrentHashMap<String, Int>()
    private val notifyMutexes = ConcurrentHashMap<String, Mutex>()
    private val pendingNotify = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    fun isAvailable(): Boolean = context.getSystemService(BluetoothManager::class.java) != null

    /** True if [address] currently has an open connection to this server (they connected to us). */
    fun isConnectedTo(address: String): Boolean = connectedDevices.containsKey(address)

    /**
     * Opens the GATT server and registers the packet-exchange service. Idempotent.
     *
     * @param onPacket invoked once a message is fully reassembled from a sender's write.
     * @param endpointFor resolves a connected device's Bluetooth address to a known
     *   [PeerEndpoint] from prior scan results, so callers see the discovery-time nodeId
     *   instead of a raw MAC address. Falls back to a synthetic endpoint built from the
     *   packet's own claimed [Packet.senderId] if the address isn't a known scan result yet.
     */
    fun start(
        onPacket: (Packet, PeerEndpoint) -> Unit,
        endpointFor: (String) -> PeerEndpoint?,
        onError: (String) -> Unit = {},
    ) {
        if (gattServer != null) return
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter
        if (manager == null || adapter == null || !adapter.isEnabled) {
            onError("BLE GATT server unavailable (Bluetooth off or unsupported)")
            return
        }

        val callback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                when (newState) {
                    BluetoothGatt.STATE_CONNECTED -> connectedDevices[device.address] = device
                    BluetoothGatt.STATE_DISCONNECTED -> {
                        connectedDevices.remove(device.address)
                        deviceMtu.remove(device.address)
                        reassembly.remove(device.address)
                        notifyMutexes.remove(device.address)
                        pendingNotify.remove(device.address)?.takeIf { !it.isCompleted }?.complete(false)
                    }
                }
            }

            override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
                deviceMtu[device.address] = mtu
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                if (characteristic.uuid != BleConstants.PACKET_CHARACTERISTIC_UUID || value.isEmpty()) {
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                    return
                }

                val isLastChunk = value[0] == LAST_CHUNK_FLAG
                val chunkData = value.copyOfRange(1, value.size)
                val buffer = reassembly.getOrPut(device.address) { ByteArrayOutputStream() }
                buffer.write(chunkData)

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
                }

                if (isLastChunk) {
                    val bytes = buffer.toByteArray()
                    reassembly.remove(device.address)
                    val packet = runCatching { ProtocolJson.decodePacket(String(bytes, Charsets.UTF_8)) }
                        .onFailure { Log.w(TAG, "Dropping malformed packet from ${device.address}", it) }
                        .getOrNull() ?: return
                    val endpoint = endpointFor(device.address) ?: PeerEndpoint(
                        nodeId = packet.senderId,
                        address = device.address,
                        transport = TransportKind.BLE,
                    )
                    onPacket(packet, endpoint)
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                // The client writes this to subscribe to notifications on the NOTIFY
                // characteristic (see BleGattClient.enableNotifications). We don't gate sendTo
                // on tracking the subscription explicitly -- Android's notifyCharacteristicChanged
                // simply won't deliver anything if the client never subscribed -- but a proper
                // ATT response is still required or some client stacks will stall waiting for it.
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            }

            override fun onNotificationSent(device: BluetoothDevice, status: Int) {
                pendingNotify[device.address]?.takeIf { !it.isCompleted }
                    ?.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }

        val service = BluetoothGattService(BleConstants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val writeCharacteristic = BluetoothGattCharacteristic(
            BleConstants.PACKET_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        val notifyCharacteristic = BluetoothGattCharacteristic(
            BleConstants.NOTIFY_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        notifyCharacteristic.addDescriptor(
            BluetoothGattDescriptor(
                BleConstants.CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            )
        )
        service.addCharacteristic(writeCharacteristic)
        service.addCharacteristic(notifyCharacteristic)

        val server = manager.openGattServer(context, callback)
        if (server == null) {
            onError("Failed to open GATT server")
            return
        }
        runCatching { server.addService(service) }
            .onFailure { Log.w(TAG, "addService failed", it) }
        gattServer = server
    }

    /**
     * Pushes [packet] to [address] over its EXISTING inbound connection (they connected to
     * us), fragmenting to that device's negotiated MTU and awaiting each chunk's
     * [BluetoothGattServerCallback.onNotificationSent] confirmation before sending the next.
     *
     * @return false if [address] has no open connection to this server, or if any chunk fails
     *   to send/confirm.
     */
    suspend fun sendTo(address: String, packet: Packet): Boolean {
        val server = gattServer ?: return false
        val device = connectedDevices[address] ?: return false
        val characteristic = server.getService(BleConstants.SERVICE_UUID)
            ?.getCharacteristic(BleConstants.NOTIFY_CHARACTERISTIC_UUID) ?: return false
        val mutex = notifyMutexes.getOrPut(address) { Mutex() }

        return mutex.withLock {
            val mtu = deviceMtu[address] ?: DEFAULT_ATT_MTU
            val chunkSize = (mtu - ATT_WRITE_OVERHEAD - 1).coerceAtLeast(1)
            val bytes = ProtocolJson.encodePacket(packet).toByteArray(Charsets.UTF_8)
            val chunks = bytes.toList().chunked(chunkSize)

            for ((index, chunk) in chunks.withIndex()) {
                val isLast = index == chunks.lastIndex
                val flag = if (isLast) LAST_CHUNK_FLAG else CONTINUATION_FLAG
                val framed = ByteArray(chunk.size + 1)
                framed[0] = flag
                chunk.forEachIndexed { i, b -> framed[i + 1] = b }

                val sent = notifyChunk(server, device, characteristic, framed, address)
                if (!sent) {
                    Log.w(TAG, "Notify chunk failed to $address at index $index/${chunks.size}")
                    return@withLock false
                }
            }
            true
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun notifyChunk(
        server: BluetoothGattServer,
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        framed: ByteArray,
        address: String,
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingNotify[address] = deferred
        characteristic.value = framed
        val queued = runCatching { server.notifyCharacteristicChanged(device, characteristic, false) }
            .getOrDefault(false)
        if (!queued) return false
        return withTimeoutOrNull(NOTIFY_TIMEOUT_MS) { deferred.await() } ?: false
    }

    /** Closes the GATT server and drops all connection state. Idempotent. */
    fun stop() {
        runCatching { gattServer?.close() }
        gattServer = null
        reassembly.clear()
        connectedDevices.clear()
        deviceMtu.clear()
        notifyMutexes.clear()
        pendingNotify.clear()
    }

    companion object {
        private const val TAG = "BleGattServer"

        /** Chunk framing flag: this is the final chunk of a message. */
        const val LAST_CHUNK_FLAG: Byte = 1

        /** Chunk framing flag: more chunks for this message follow. */
        const val CONTINUATION_FLAG: Byte = 0

        private const val DEFAULT_ATT_MTU = 23
        private const val ATT_WRITE_OVERHEAD = 3
        private const val NOTIFY_TIMEOUT_MS = 10_000L
    }
}
