package net.namekdev.entity_tracker.network.base


/**
 * Client that automatically reconnects.

 * @author Namek
 */
class PersistentClient(connectionListener: RawConnectionCommunicator) : Client() {
    @Volatile private var isReconnectEnabled: Boolean = false

    /**
     * Delay between two reconnects, specified in milliseconds.
     */
    var reconnectDelay = 1000


    init {
        super.connectionListener = connectionListener
    }

    override fun connect(serverName: String, serverPort: Int): Client {
        return connect(serverName, serverPort, false)
    }

    fun connect(serverName: String, serverPort: Int, manualUpdate: Boolean): Client {
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
                    super@PersistentClient.connect(serverName, serverPort)

                    if (!manualUpdate)
                        super@PersistentClient.startThread()
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
