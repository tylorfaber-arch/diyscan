package dev.diyscan.transport

import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue

/**
 * USB OBD adapters (ELM327 USB clones, OBDLink SX, etc.) over USB OTG.
 * They are USB-serial chips (FTDI, CH340, Prolific, CP210x) with an ELM327/STN behind them,
 * so this is just another byte pipe — the ELM327 driver above doesn't change.
 *
 * Baud rate varies by adapter (38400 is common for clones, 115200 for OBDLink SX),
 * so connect() tries the usual ones until the adapter answers ATI.
 */
class UsbSerialTransport(
    private val usb: UsbManager,
    private val driver: UsbSerialDriver,
) : Transport {

    private val rx = LinkedBlockingQueue<Int>()
    private var port: UsbSerialPort? = null
    private var io: SerialInputOutputManager? = null
    var baud: Int = 0
        private set

    override val input: InputStream = object : InputStream() {
        override fun read(): Int = rx.take()
        override fun available(): Int = rx.size
    }

    override val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()))
        override fun write(b: ByteArray, off: Int, len: Int) {
            val p = port ?: throw IOException("USB adapter not open")
            p.write(b.copyOfRange(off, off + len), 1000)
        }
    }

    override suspend fun connect() = withContext(Dispatchers.IO) {
        val conn = usb.openDevice(driver.device) ?: throw IOException("USB permission denied or device unavailable")
        val p = driver.ports.firstOrNull() ?: throw IOException("Adapter has no serial port")
        p.open(conn)
        port = p
        runCatching { p.setDTR(true); p.setRTS(true) }

        io = SerialInputOutputManager(p, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) { data.forEach { rx.put(it.toInt() and 0xFF) } }
            override fun onRunError(e: Exception) { rx.put(-1) } // unplugged → ELM driver sees disconnect
        }).also { it.start() }

        for (rate in listOf(38400, 115200, 9600, 230400, 500000)) {
            p.setParameters(rate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            rx.clear()
            output.write("\rATI\r".toByteArray())
            val reply = StringBuilder()
            val until = System.currentTimeMillis() + 900
            while (System.currentTimeMillis() < until) {
                while (rx.isNotEmpty()) { val b = rx.poll() ?: break; if (b >= 0) reply.append(b.toChar()) }
                if (reply.contains('>')) break
                delay(20)
            }
            val r = reply.toString().uppercase()
            if ("ELM" in r || "STN" in r || "OBD" in r) {
                baud = rate
                delay(100); rx.clear()
                return@withContext
            }
        }
        close()
        throw IOException("USB device found, but no ELM327 answered at any common baud rate")
    }

    override fun close() {
        runCatching { io?.stop() }
        runCatching { port?.close() }
        io = null; port = null
        rx.put(-1)
    }
}
