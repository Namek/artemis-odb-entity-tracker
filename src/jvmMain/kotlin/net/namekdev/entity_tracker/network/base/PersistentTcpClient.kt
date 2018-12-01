package net.namekdev.entity_tracker.network.base

import net.namekdev.entity_tracker.network.RawConnectionCommunicator


/**
 * Client that automatically reconnects.

 * @author Namek
 */
class PersistentTcpClient(connectionListener: RawConnectionCommunicator) : TcpClient() {
    @Volatile private var isReconnectEnabled: Boolean = false

    /**
     * Delay between two reconnects, specified in milliseconds.
     */
    var reconnectDelay = 1000


    init {
        super.connectionListener = connectionListener
    }

    override fun connect(serverName: String, serverPort: Int): TcpClient {
        return connect(serverName, serverPort, false)
    }

    fun connect(serverName: String, serverPort: Int, manualUpdate: Boolean): TcpClient {
        isReconnectEnabled = true

        val thread = Thread(object : Runnable {
            override fun run() {
                tryConnect()

                while (isReconnectEnabled) {
                    if (!isConnected) {
                        tryConnect()
                    }

                    try {
                        Thread.sleep(reconnectDelay.toLong())
                    }
                    catch (e: InterruptedException) {
                    }

                }
            }

            private fun tryConnect() {
                try {
                    super@PersistentTcpClient.connect(serverName, serverPort)

                    if (!manualUpdate)
                        super@PersistentTcpClient.startThread()
                }
                catch (ex: Exception) {
                }

            }
        })

        thread.start()

        return this
    }

    override fun stop() {
        super.stop()
        isReconnectEnabled = false
    }
}
