package net.namekdev.entity_tracker.network.base;

import java.net.SocketAddress;

public interface RawConnectionCommunicator {
	void connected(SocketAddress remoteAddress, RawConnectionOutputListener output);
	void disconnected();

	/** @return consumed bytes count */
	int bytesReceived(byte[] bytes, int offset, int length);
}
