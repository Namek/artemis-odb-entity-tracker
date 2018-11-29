package net.namekdev.entity_tracker.connectors

interface WorldUpdateInterfaceListener : WorldUpdateListener {
	fun disconnected()
}