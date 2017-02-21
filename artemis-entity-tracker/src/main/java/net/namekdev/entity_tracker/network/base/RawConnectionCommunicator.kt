package net.namekdev.entity_tracker.network.base

import java.net.SocketAddress

interface RawConnectionCommunicator {
    fun connected(remoteAddress: SocketAddress, output: RawConnectionOutputListener)
    fun disconnected()
    fun bytesReceived(bytes: ByteArray, offset: Int, length: Int)
}
