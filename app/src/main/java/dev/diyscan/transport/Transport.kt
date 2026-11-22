package dev.diyscan.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * A byte pipe to the adapter. Bluetooth Classic is the only one for now;
 * BLE, Wi-Fi (TCP 192.168.0.10:35000) or USB can be added later by
 * implementing this same interface. Nothing above this layer cares.
 */
interface Transport {
    val input: InputStream
    val output: OutputStream
    suspend fun connect()
    fun close()
}

/** Bluetooth Classic serial (SPP) — what ELM327 / OBDLink MX+ / Vgate adapters use. */
@SuppressLint("MissingPermission") // permission is requested in MainActivity before use
class BluetoothClassicTransport(private val device: BluetoothDevice) : Transport {

    private var socket: BluetoothSocket? = null

    override val input: InputStream get() = socket!!.inputStream
    override val output: OutputStream get() = socket!!.outputStream

    override suspend fun connect() = withContext(Dispatchers.IO) {
        val s = try {
            device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
        } catch (e: Exception) {
            // Some cheap adapters only accept the insecure socket.
            device.createInsecureRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
        }
        socket = s
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
