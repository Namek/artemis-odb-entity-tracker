package net.namekdev.entity_tracker

import net.namekdev.entity_tracker.network.base.PersistentTcpClient
import net.namekdev.entity_tracker.network.base.TcpServer
import net.namekdev.entity_tracker.network.communicator.ExternalInterfaceCommunicator
import net.namekdev.entity_tracker.ui.EntityTrackerMainWindow

object StandaloneMain {
	@JvmStatic fun main(args: Array<String>) {
		val serverName = if (args.size > 0) args[0] else "localhost"
		val serverPort = if (args.size > 1) Integer.parseInt(args!![1]) else TcpServer.DEFAULT_PORT
		init(serverName, serverPort)
	}

	fun init(serverName: String, serverPort: Int) {
		val window = EntityTrackerMainWindow(true)
		val client = PersistentTcpClient(ExternalInterfaceCommunicator(window))
		client.connect(serverName, serverPort)
	}
}