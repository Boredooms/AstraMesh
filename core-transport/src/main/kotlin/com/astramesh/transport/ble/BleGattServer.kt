package com.astramesh.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * GATT peripheral (server) role of the BLE transport (docs/architecture.md §9).
 *
 * Every AstraMesh node runs both a GATT server (this class, receiving) and a GATT client
 * ([BleGattClient], sending) -- there's no fixed central/peripheral split at the app level,
 * because a mesh node must both push packets to and accept packets from any neighbor. A->B and
 * B->A are two independent connections, each opened by whichever side is sending for that
 * particular packet (e.g. B connects to A's server to deliver an ACK, even though A originally
 * connected to B's server to deliver the chat message it's acknowledging).
 *
 * ## Fragmentation
 * A [Packet] JSON-encodes to more bytes than fit in a single BLE write once ATT overhead is
 * accounted for (default ATT MTU is 23 bytes -> 20 usable payload bytes; even a negotiated
 * [BleConstants.TARGET_MTU] is still smaller than many packets). Each GATT write to
 * [BleConstants.PACKET_CHARACTERISTIC_UUID] therefore carries one *chunk*, framed as
 * `[1 flag byte][chunk bytes]` -- [LAST_CHUNK_FLAG] marks the final chunk of a message,
 * [CONTINUATION_FLAG] means more chunks follow. Chunks are reassembled per sender
 * [BluetoothDevice.getAddress]; a phone can have several simultaneous inbound connections
 * mid-reassembly at once.
 */
@SuppressLint("MissingPermission")
class BleGattServer(private val context: Context) {

    private var gattServer: BluetoothGattServer? = null
    private val reassembly = ConcurrentHashMap<String, ByteArrayOutputStream>()

    fun isAvailable(): Boolean = context.getSystemService(BluetoothManager::class.java) != null

    /**
     * Opens the GATT server and registers the packet-exchange service. Idempotent.
     *
     * @param onPacket invoked once a message is fully reassembled from a sender.
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
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    reassembly.remove(device.address)
                }
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
        }

        val service = BluetoothGattService(BleConstants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            BleConstants.PACKET_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        service.addCharacteristic(characteristic)

        val server = manager.openGattServer(context, callback)
        if (server == null) {
            onError("Failed to open GATT server")
            return
        }
        runCatching { server.addService(service) }
            .onFailure { Log.w(TAG, "addService failed", it) }
        gattServer = server
    }

    /** Closes the GATT server and drops any in-flight reassembly buffers. Idempotent. */
    fun stop() {
        runCatching { gattServer?.close() }
        gattServer = null
        reassembly.clear()
    }

    companion object {
        private const val TAG = "BleGattServer"

        /** Chunk framing flag: this is the final chunk of a message. */
        const val LAST_CHUNK_FLAG: Byte = 1

        /** Chunk framing flag: more chunks for this message follow. */
        const val CONTINUATION_FLAG: Byte = 0
    }
}
