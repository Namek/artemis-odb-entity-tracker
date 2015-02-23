package net.namekdev.entity_tracker.network.base;

public interface RawConnectionOutputListener {
	void send(byte[] buffer, int offset, int length);
}
