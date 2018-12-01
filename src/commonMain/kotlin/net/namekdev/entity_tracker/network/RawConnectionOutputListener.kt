package net.namekdev.entity_tracker.network

interface RawConnectionOutputListener {
    fun send(buffer: ByteArray, offset: Int, length: Int)
}
