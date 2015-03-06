package net.namekdev.entity_tracker.network.communicator;

import java.net.SocketAddress;
import java.util.BitSet;

import net.namekdev.entity_tracker.connectors.WorldController;
import net.namekdev.entity_tracker.connectors.WorldUpdateListener;
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener;

/**
 * Communicator used by UI (client).
 *
 * @author Namek
 */
public class ExternalInterfaceCommunicator extends Communicator implements WorldController {
	private WorldUpdateListener _listener;


	public ExternalInterfaceCommunicator(WorldUpdateListener listener) {
		_listener = listener;
	}

	@Override
	public void connected(SocketAddress remoteAddress, RawConnectionOutputListener output) {
		super.connected(remoteAddress, output);
		_listener.injectWorldController(this);
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void bytesReceived(byte[] bytes, int offset, int length) {
		_deserializer.setSource(bytes, offset, length);

		byte packetType = _deserializer.readRawByte();

		switch (packetType) {
			case TYPE_ADDED_ENTITY_SYSTEM: {
				int index = _deserializer.readInt();
				String name = _deserializer.readString();
				BitSet allTypes = _deserializer.readBitSet();
				BitSet oneTypes = _deserializer.readBitSet();
				BitSet notTypes = _deserializer.readBitSet();
				_listener.addedSystem(index, name, allTypes, oneTypes, notTypes);
				break;
			}
			case TYPE_ADDED_MANAGER: {
				String name = _deserializer.readString();
				_listener.addedManager(name);
				break;
			}
			case TYPE_ADDED_COMPONENT_TYPE: {
				int index = _deserializer.readInt();
				String name = _deserializer.readString();
				_listener.addedComponentType(index, name);
				break;
			}
			case TYPE_UPDATED_ENTITY_SYSTEM: {
				int index = _deserializer.readInt();
				int entitiesCount = _deserializer.readInt();
				int maxEntitiesCount = _deserializer.readInt();
				_listener.updatedEntitySystem(index, entitiesCount, maxEntitiesCount);
				break;
			}
			case TYPE_ADDED_ENTITY: {
				int entityId = _deserializer.readInt();
				BitSet components = _deserializer.readBitSet();
				_listener.addedEntity(entityId, components);
				break;
			}
			case TYPE_DELETED_ENTITY: {
				int entityId = _deserializer.readInt();
				_listener.deletedEntity(entityId);
				break;
			}

			default: throw new RuntimeException("Unknown packet type: " + (int)packetType);
		}
	}

	@Override
	public void setSystemState(String name, boolean isOn) {
		send(
			beginPacket(TYPE_SET_SYSTEM_STATE)
			.addString(name)
			.addBoolean(isOn)
		);
	}
}
