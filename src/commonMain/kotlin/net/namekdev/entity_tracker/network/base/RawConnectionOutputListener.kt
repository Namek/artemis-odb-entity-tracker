package net.namekdev.entity_tracker.network.base

interface RawConnectionOutputListener {
    fun send(buffer: ByteArray, offset: Int, length: Int)
}
