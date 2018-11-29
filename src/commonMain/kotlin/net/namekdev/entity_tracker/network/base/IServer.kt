package net.namekdev.entity_tracker.network.base

/**
 *
 */
abstract class IServer {
    internal abstract fun setCommunicator(communicatorProvider: RawConnectionCommunicatorProvider)
    abstract fun start(): IServer
    abstract fun stop()
}