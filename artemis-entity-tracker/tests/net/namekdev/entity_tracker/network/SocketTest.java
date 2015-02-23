package net.namekdev.entity_tracker.network;

import java.net.SocketAddress;

import net.namekdev.entity_tracker.network.base.Client;
import net.namekdev.entity_tracker.network.base.RawConnectionCommunicator;
import net.namekdev.entity_tracker.network.base.RawConnectionCommunicatorProvider;
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener;
import net.namekdev.entity_tracker.network.base.Server;

public class SocketTest {
	public static void main(String[] args) {
		Server server = new Server(new RawConnectionCommunicatorProvider() {
			@Override
			public RawConnectionCommunicator getListener(String remoteName) {
				return new RawConnectionCommunicator() {

					@Override
					public void disconnected() {

					}

					@Override
					public void connected(SocketAddress remoteAddress, RawConnectionOutputListener output) {
						System.out.println("server: client connected");
					}

					@Override
					public int bytesReceived(byte[] bytes, int offset, int length) {
						return length;
					}
				};
			}
		});
		server.start();

		Client client = new Client(new RawConnectionCommunicator() {
			@Override
			public void disconnected() {

			}

			@Override
			public void connected(SocketAddress remoteAddress, RawConnectionOutputListener output) {
				System.out.println("client: connected!");
			}

			@Override
			public int bytesReceived(byte[] bytes, int offset, int length) {
				return length;
			}
		});
//		client.connect("localhost", Server.DEFAULT_PORT);
//		client.startThread();
	}
}
