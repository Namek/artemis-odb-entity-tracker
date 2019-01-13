package net.namekdev.entity_tracker.connectors

interface IWorldUpdateInterfaceListener<BitVectorType> : IWorldUpdateListener<BitVectorType> {
	fun disconnected()
}