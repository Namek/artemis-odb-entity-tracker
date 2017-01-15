package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.network.base.Client
import net.namekdev.entity_tracker.network.base.PersistentClient
import net.namekdev.entity_tracker.network.base.Server
import net.namekdev.entity_tracker.network.communicator.ExternalInterfaceCommunicator
import net.namekdev.entity_tracker.ui.EntityTrackerMainWindow

object StandaloneMain {
	@JvmStatic fun main(args: Array<String>?) {
		val serverName = if (args!!.size > 0) args!![0] else "localhost"
		val serverPort = if (args!!.size > 1) Integer.parseInt(args!![1]) else Server.DEFAULT_PORT
		init(serverName, serverPort)
	}

	fun init(serverName: String?, serverPort: Int) {
		val window = EntityTrackerMainWindow(true)
		val client = PersistentClient(ExternalInterfaceCommunicator(window))
		client!!.connect(serverName, serverPort)
	}
}