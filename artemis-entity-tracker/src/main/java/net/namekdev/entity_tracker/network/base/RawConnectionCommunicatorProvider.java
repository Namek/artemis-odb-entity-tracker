package net.namekdev.entity_tracker.network.base;

public interface RawConnectionCommunicatorProvider {
	RawConnectionCommunicator getListener(String remoteName);
}
