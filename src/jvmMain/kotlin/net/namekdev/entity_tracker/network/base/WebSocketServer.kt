package net.namekdev.entity_tracker.network.base

import com.artemis.utils.Bag
import net.namekdev.entity_tracker.network.IServer
import net.namekdev.entity_tracker.network.RawConnectionCommunicator
import net.namekdev.entity_tracker.network.RawConnectionCommunicatorProvider
import net.namekdev.entity_tracker.network.RawConnectionOutputListener
import org.webbitserver.WebServer
import org.webbitserver.WebServers
import org.webbitserver.WebSocketConnection
import org.webbitserver.WebSocketHandler

/**
 *
 */
class WebSocketServer(val listeningPort: Int = 8025) : IServer() {
    private lateinit var _clientListenerProvider: RawConnectionCommunicatorProvider
    private lateinit var _server: WebServer
    private val _clients = Bag<AClient>()
    private var _clientCounter = 0


    override fun setClientCommunicatorProvider(communicatorProvider: RawConnectionCommunicatorProvider) {
        _clientListenerProvider = communicatorProvider
    }

    override fun start(): IServer {
        _server = WebServers.createWebServer(listeningPort)
        _server.add("/actions", WebSocketHandlerImpl())
        _server.start()

        return this
    }

    override fun stop() {
        _clients.clear()
        _clients.forEach {
            it.connection.close()
        }
        _server.stop()
    }


    inner class WebSocketHandlerImpl : WebSocketHandler {
        override fun onOpen(connection: WebSocketConnection) {
            ++_clientCounter
            val identifier = "client #" + _clientCounter
            val comm = _clientListenerProvider.getListener(identifier)
            val client = AClient(connection, comm)
            _clients.add(client)
            comm.connected(identifier, client)
        }

        override fun onMessage(connection: WebSocketConnection, msg: String) {
            System.out.println("WebSocket message: " + msg)
        }

        override fun onMessage(connection: WebSocketConnection, msg: ByteArray) {
            val client = _clients.find { it.connection == connection }

            if (client != null)
                client.comm.bytesReceived(msg, 0, msg.size)
            else
                System.out.println("WebSocket binary message: " + msg.size)
        }

        override fun onClose(connection: WebSocketConnection) {
            val client = _clients.find { it.connection == connection }

            if (client != null) {
                client.comm.disconnected()
                _clients.remove(client)
            }

            System.out.println("WebSocket closed")
        }

        override fun onPong(connection: WebSocketConnection, msg: ByteArray) {

        }

        override fun onPing(connection: WebSocketConnection, msg: ByteArray) {

        }
    }

    class AClient(
        val connection: WebSocketConnection,
        val comm: RawConnectionCommunicator
    ) : RawConnectionOutputListener {
        override fun send(buffer: ByteArray, offset: Int, length: Int) {
            connection.send(buffer, offset, length)
        }
    }
}