package net.namekdev.entity_tracker.network.base

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

import com.artemis.utils.Bag

/**
 * Multi-threaded multi-client server.
 *
 * @author Namek
 */
open class Server {
    protected var listeningPort = DEFAULT_PORT
    protected var socket: ServerSocket? = null
    protected var isRunning: Boolean = false
    protected lateinit var runningThread: Thread
    protected val clients = Bag<Client>()

    protected lateinit var clientListenerProvider: RawConnectionCommunicatorProvider


    constructor(clientListenerProvider: RawConnectionCommunicatorProvider) {
        this.clientListenerProvider = clientListenerProvider
    }

    constructor(clientListenerProvider: RawConnectionCommunicatorProvider, listeningPort: Int) {
        this.clientListenerProvider = clientListenerProvider
        this.listeningPort = listeningPort
    }

    protected constructor() {}

    /**
     * Starts listening in new thread.
     */
    fun start(): Server {
        if (socket != null && !socket!!.isClosed) {
            throw IllegalStateException("Cannot serve twice in the same time.")
        }

        try {
            socket = ServerSocket(listeningPort)
        }
        catch (e: IOException) {
            throw RuntimeException("Couldn't start server on port " + listeningPort, e)
        }

        runningThread = Thread(ServerThread())
        runningThread!!.start()

        return this
    }

    fun stop() {
        this.isRunning = false

        var i = 0
        val n = clients.size()
        while (i < n) {
            val client = clients.get(i)
            client.stop()
            clients.remove(client)
            ++i
        }

        try {
            socket!!.close()
        }
        catch (e: IOException) {
            throw RuntimeException("Couldn't shutdown server.", e)
        }
    }

    protected fun createSocketListener(socket: Socket): Client {
        val connectionListener = clientListenerProvider!!.getListener(socket.remoteSocketAddress.toString())
        return Client(socket, connectionListener)
    }


    companion object {
        const val DEFAULT_PORT = 1087
    }


    inner class ServerThread : Runnable {
        override fun run() {
            synchronized(this) {
                runningThread = Thread.currentThread()
            }

            isRunning = true

            while (isRunning) {
                var clientSocket: Socket? = null
                try {
                    clientSocket = socket!!.accept()
                    clientSocket!!.tcpNoDelay = true
                }
                catch (e: IOException) {
                    if (isRunning) {
                        throw RuntimeException("Error accepting client connection", e)
                    }

                    return
                }

                val client = createSocketListener(clientSocket)
                client.initSocket()
                val clientThread = Thread(client.threadRunnable)

                clients.add(client)
                clientThread.start()
            }
        }
    }
}
