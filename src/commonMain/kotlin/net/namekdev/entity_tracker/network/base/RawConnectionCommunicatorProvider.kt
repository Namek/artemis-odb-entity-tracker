package net.namekdev.entity_tracker.network.base

interface RawConnectionCommunicatorProvider {
    fun getListener(remoteName: String): RawConnectionCommunicator
}
