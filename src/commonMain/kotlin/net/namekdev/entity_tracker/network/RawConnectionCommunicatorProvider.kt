package net.namekdev.entity_tracker.network

interface RawConnectionCommunicatorProvider {
    fun getListener(remoteName: String): RawConnectionCommunicator
}
