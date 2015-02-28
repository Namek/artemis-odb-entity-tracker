package net.namekdev.entity_tracker.network;

import java.util.BitSet;

import net.namekdev.entity_tracker.connectors.WorldUpdateListener;

/**
 * Communicator used by UI (client).
 * Direction: client to server = window to entity tracker
 *
 * @author Namek
 */
public class ExternalInterfaceCommunicator extends Communicator {
	private WorldUpdateListener _listener;


	public ExternalInterfaceCommunicator(WorldUpdateListener listener) {
		_listener = listener;
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
				String name = _deserializer.readString();
				BitSet allTypes = _deserializer.readBitSet();
				BitSet oneTypes = _deserializer.readBitSet();
				BitSet notTypes = _deserializer.readBitSet();
				_listener.addedSystem(name, allTypes, oneTypes, notTypes);
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
}
