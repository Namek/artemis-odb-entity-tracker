package net.namekdev.entity_tracker.network.base;

import java.net.SocketAddress;

public interface RawConnectionCommunicator {
	void connected(SocketAddress remoteAddress, RawConnectionOutputListener output);
	void disconnected();
	void bytesReceived(byte[] bytes, int offset, int length);
}
