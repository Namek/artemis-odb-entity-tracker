package net.namekdev.entity_tracker.connectors

interface WorldUpdateInterfaceListener<BitVectorType> : WorldUpdateListener<BitVectorType> {
	fun disconnected()
}