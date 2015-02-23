package net.namekdev.entity_tracker.network;

import java.net.SocketAddress;

import net.namekdev.entity_tracker.network.base.RawConnectionCommunicator;
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener;
import net.namekdev.entity_tracker.utils.serialization.NetworkDeserializer;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer;

/**
 * Defines basics of network protocol for communication between EntityTracker Manager and external UI.
 *
 * @author Namek
 */
public abstract class Communicator implements RawConnectionCommunicator {
	protected RawConnectionOutputListener _output;
	protected final NetworkSerializer _serializer = NetworkSerialization.createSerializer();
	protected final NetworkDeserializer _deserializer = NetworkSerialization.createDeserializer();

	protected static final byte TYPE_ADDED_ENTITY_SYSTEM = 60;
	protected static final byte TYPE_ADDED_MANAGER = 61;
	protected static final byte TYPE_ADDED_COMPONENT_TYPE = 63;
	protected static final byte TYPE_ADDED_ENTITY = 65;
	protected static final byte TYPE_DELETED_ENTITY = 68;


	@Override
	public void connected(SocketAddress remoteAddress, RawConnectionOutputListener output) {
		_output = output;
	}

	protected void send(NetworkSerializer serializer) {
		NetworkSerializer.SerializeResult data = serializer.getResult();
		_output.send(data.buffer, 0, data.size);
	}

	protected NetworkSerializer beginPacket(byte packetType) {
		return _serializer.reset().addRawByte(packetType);
	}
}
