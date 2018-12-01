package net.namekdev.entity_tracker.network.base

import net.namekdev.entity_tracker.network.RawConnectionCommunicator
import net.namekdev.entity_tracker.network.RawConnectionOutputListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.UnknownHostException
import java.util.Arrays

/**
 * @author Namek
 */
open class TcpClient {
    protected var remoteName: String? = null
    protected var serverPort = TcpServer.DEFAULT_PORT

    protected var socket: Socket? = null
    protected var thread: Thread? = null
    protected lateinit var input: InputStream
    protected lateinit var output: OutputStream

    protected var _isRunning: Boolean = false
    private val _buffer = ByteArray(10240)
    private var _incomingSize = -1
    private var _lastHeartbeatTime = System.currentTimeMillis()

    lateinit var connectionListener: RawConnectionCommunicator

    /**
     * Time between heartbeats, specified in milliseconds.
     */
    var heartbeatDelay = 1000


    constructor() {}

    constructor(connectionListener: RawConnectionCommunicator) {
        this.connectionListener = connectionListener
    }

    internal constructor(socket: Socket, connectionListener: RawConnectionCommunicator) {
        this.socket = socket
        this.connectionListener = connectionListener
        _isRunning = socket.isConnected && !socket.isClosed
    }


    /**
     * Connects to server. You can chain `#startThread()` call.

     * @param serverName
     * *
     * @param serverPort
     */
    open fun connect(serverName: String, serverPort: Int): TcpClient {
        if (isConnected) {
            throw IllegalStateException("Cannot connect twice in the same time.")
        }

        this.remoteName = serverName
        this.serverPort = serverPort

        try {
            socket = Socket(serverName, serverPort)
            initSocket()

            return this
        }
        catch (e: UnknownHostException) {
            throw RuntimeException(e)
        }
        catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    fun startThread() {
        if (!_isRunning) {
            throw RuntimeException("Call #connect() first!")
        }

        thread = Thread(threadRunnable)
        thread!!.start()
    }

    internal fun initSocket() {
        try {
            socket!!.tcpNoDelay = true
            input = socket!!.inputStream
            output = socket!!.outputStream
            _isRunning = true

            connectionListener!!.connected(socket!!.remoteSocketAddress.toString(), outputListener)
        }
        catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Checks for new bytes in network buffer. Also sends hearbeats.

     *
     * This method can be run manually or used through [.startThread].
     */
    fun update(): Boolean {
        if (_isRunning && isConnected) {
            var n = 0
            try {
                n = input.available()

                if (n == 0) {
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - _lastHeartbeatTime > heartbeatDelay) {
                        _lastHeartbeatTime = currentTime

                        output.write(heartbeat, 0, heartbeat.size)
                        output.flush()
                    }
                }
                else
                    do {
                        if (_incomingSize <= 0 && n >= IntegerBYTES) {
                            input.read(_buffer, 0, IntegerBYTES)
                            _incomingSize = readRawInt(_buffer, 0)
                            n -= IntegerBYTES
                            continue
                        }

                        if (_incomingSize > 0 && n >= _incomingSize) {
                            input.read(_buffer, 0, _incomingSize)
                            connectionListener.bytesReceived(_buffer, 0, _incomingSize)
                            _incomingSize = 0
                        }

                        n = input.available()
                    } while (n > 0)
            }
            catch (e: Exception) {
                _isRunning = false
            }

        }

        return _isRunning
    }

    val isConnected: Boolean
        get() = _isRunning && socket != null && !socket!!.isClosed && !socket!!.isOutputShutdown

    open fun stop() {
        _isRunning = false

        try {
            input.close()
        }
        catch (e: Exception) {
        }

        try {
            output.close()
        }
        catch (e: Exception) {
        }

        try {
            socket!!.close()
        }
        catch (e: IOException) {
        }

    }

    fun send(buffer: ByteArray, offset: Int, length: Int) {
        outputListener.send(buffer, offset, length)
    }

    internal val threadRunnable: Runnable = Runnable {
        while (_isRunning && !socket!!.isClosed) {
            if (!update()) {
                connectionListener!!.disconnected()
                break
            }

            try {
                Thread.sleep(100)
            }
            catch (e: InterruptedException) {
                if (_isRunning) {
                    throw RuntimeException(e)
                }

                return@Runnable
            }
        }

        stop()
    }

    private val outputListener = object : RawConnectionOutputListener {
        override fun send(buffer: ByteArray, offset: Int, length: Int) {
            try {
                output.write(length shr 24 and 0xFF)
                output.write(length shr 16 and 0xFF)
                output.write(length shr 8 and 0xFF)
                output.write(length and 0xFF)
                output.write(buffer, offset, length)
                output.flush()
            }
            catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    companion object {
        private val IntegerBYTES = Integer.SIZE / 8

        private val heartbeat: ByteArray

        init {
            heartbeat = ByteArray(IntegerBYTES)
            Arrays.fill(heartbeat, 0.toByte())
        }

        protected fun readRawInt(buffer: ByteArray, offset: Int): Int {
            var offset = offset
            var value = buffer[offset++].toInt() and 0xFF
            value = value shl 8
            value = value or (buffer[offset++].toInt() and 0xFF)
            value = value shl 8
            value = value or (buffer[offset++].toInt() and 0xFF)
            value = value shl 8
            value = value or (buffer[offset].toInt() and 0xFF)

            return value
        }
    }
}
