package net.namekdev.entity_tracker.network

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.WebSocket
import org.w3c.dom.WebSocket.Companion.CLOSED
import org.w3c.dom.WebSocket.Companion.CLOSING

class WebSocketClient(val connectionListener: RawConnectionCommunicator) {
    private var socket: WebSocket? = null

    var isConnected = false
        private set

    init {

    }

    fun connect(url: String) {
        if (socket?.readyState ?: CLOSED < CLOSING) {
            throw IllegalStateException("Cannot connect twice in the same time.")
        }

        socket = WebSocket(url)
        socket!!.let {
            it.binaryType = BinaryType.ARRAYBUFFER
            it.onopen = {
                isConnected = true
                connectionListener.connected(url, outputListener)
            }
            it.onclose = {
                isConnected = false
                connectionListener.disconnected()
            }
            it.onmessage = {
                val buf = Uint8Array(it.asDynamic().data.unsafeCast<ArrayBuffer>())
                val length = buf.length
                val bytes = ByteArray(length)

                for (j in 0 until length)
                    bytes[j] = buf[j]

                connectionListener.bytesReceived(bytes, 0, length)
                undefined
            }
        }
    }

    fun stop() {
        isConnected = false
        socket?.close()
    }

    fun send(buffer: ByteArray, offset: Int, length: Int) =
        outputListener.send(buffer, offset, length)


    private val outputListener = object : RawConnectionOutputListener {
        override fun send(buffer: ByteArray, offset: Int, length: Int) {
            try {
                val buf = Uint8Array(length)
                var i = 0
                for (j in offset until offset+length)
                    buf[i++] = buffer[j]

                socket!!.send(buf)
            }
            catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}