package net.namekdev.entity_tracker.network

/**
 *
 */
abstract class IServer {
    internal abstract fun setClientCommunicatorProvider(communicatorProvider: RawConnectionCommunicatorProvider)
    abstract fun start(): IServer
    abstract fun stop()
}