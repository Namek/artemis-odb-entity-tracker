package net.namekdev.entity_tracker.network

interface RawConnectionCommunicator {
    fun connected(identifier: String, output: RawConnectionOutputListener)
    fun disconnected()
    fun bytesReceived(bytes: ByteArray, offset: Int, length: Int)
}
