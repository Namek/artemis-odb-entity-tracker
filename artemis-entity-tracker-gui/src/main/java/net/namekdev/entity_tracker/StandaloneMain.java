package net.namekdev.entity_tracker;

import net.namekdev.entity_tracker.network.base.Client;
import net.namekdev.entity_tracker.network.base.PersistentClient;
import net.namekdev.entity_tracker.network.base.Server;
import net.namekdev.entity_tracker.network.communicator.ExternalInterfaceCommunicator;
import net.namekdev.entity_tracker.ui.EntityTrackerMainWindow;

public class StandaloneMain {
	public static void main(String[] args) {
		String serverName = args.length > 0 ? args[0] : "localhost";
		int serverPort = args.length > 1 ? Integer.parseInt(args[1]) : Server.DEFAULT_PORT;

		init(serverName, serverPort);
	}

	public static void init(final String serverName, final int serverPort) {
		final EntityTrackerMainWindow window = new EntityTrackerMainWindow(true);
		final Client client = new PersistentClient(new ExternalInterfaceCommunicator(window));

		client.connect(serverName, serverPort);
	}
}
