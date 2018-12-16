package net.namekdev.entity_tracker.network

import net.namekdev.entity_tracker.network.base.TcpClient
import net.namekdev.entity_tracker.network.base.TcpServer

object SocketTest {
    @JvmStatic fun main(args: Array<String>) {
        val server = TcpServer()
        server.setClientCommunicatorProvider(object : RawConnectionCommunicatorProvider {
            override fun getListener(remoteName: String): RawConnectionCommunicator {
                return object : RawConnectionCommunicator {

                    override fun disconnected() {

                    }

                    override fun connected(identifier: String, output: RawConnectionOutputListener) {
                        println("server: client connected")
                    }

                    override fun bytesReceived(bytes: ByteArray, offset: Int, length: Int) {}
                }
            }
        })
        server.start()

        val client = TcpClient(object : RawConnectionCommunicator {
            override fun disconnected() {

            }

            override fun connected(identifier: String, output: RawConnectionOutputListener) {
                println("client: connected!")
            }

            override fun bytesReceived(bytes: ByteArray, offset: Int, length: Int) {}
        })
        client.connect("localhost", TcpServer.DEFAULT_PORT)
    }
}
