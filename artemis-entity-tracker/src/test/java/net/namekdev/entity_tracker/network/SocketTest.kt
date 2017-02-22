package net.namekdev.entity_tracker.network

import java.net.SocketAddress

import net.namekdev.entity_tracker.network.base.Client
import net.namekdev.entity_tracker.network.base.RawConnectionCommunicator
import net.namekdev.entity_tracker.network.base.RawConnectionCommunicatorProvider
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener
import net.namekdev.entity_tracker.network.base.Server

object SocketTest {
    @JvmStatic fun main(args: Array<String>) {
        val server = Server(object : RawConnectionCommunicatorProvider {
            override fun getListener(remoteName: String): RawConnectionCommunicator {
                return object : RawConnectionCommunicator {

                    override fun disconnected() {

                    }

                    override fun connected(remoteAddress: SocketAddress, output: RawConnectionOutputListener) {
                        println("server: client connected")
                    }

                    override fun bytesReceived(bytes: ByteArray, offset: Int, length: Int) {}
                }
            }
        })
        server.start()

        val client = Client(object : RawConnectionCommunicator {
            override fun disconnected() {

            }

            override fun connected(remoteAddress: SocketAddress, output: RawConnectionOutputListener) {
                println("client: connected!")
            }

            override fun bytesReceived(bytes: ByteArray, offset: Int, length: Int) {}
        })
        client.connect("localhost", Server.DEFAULT_PORT)
    }
}
