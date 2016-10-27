package net.namekdev.entity_tracker.network.communicator;

import com.artemis.utils.BitVector;
import net.namekdev.entity_tracker.connectors.WorldController;
import net.namekdev.entity_tracker.connectors.WorldUpdateListener;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.model.FieldInfo;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer;

/**
 * Deserializes data from network and serializes data sent to the network.
 * Manages between logic events and pure network bytes.
 *
 * Communicator used by EntityTracker manager (server), one such communicator per client.
 *
 * @author Namek
 */
public class EntityTrackerCommunicator extends Communicator implements WorldUpdateListener {
	private WorldController _worldController;


	@Override
	public void bytesReceived(byte[] bytes, int offset, int length) {
		_deserializer.setSource(bytes, offset, length);

		byte packetType = _deserializer.readRawByte();

		switch (packetType) {
			case TYPE_SET_SYSTEM_STATE: {
				String systemName = _deserializer.readString();
				boolean isSystemOn = _deserializer.readBoolean();
				_worldController.setSystemState(systemName, isSystemOn);
				break;
			}
			case TYPE_REQUEST_COMPONENT_STATE: {
				int entityId = _deserializer.readInt();
				int componentIndex = _deserializer.readInt();
				_worldController.requestComponentState(entityId, componentIndex);
				break;
			}
			case TYPE_SET_COMPONENT_FIELD_VALUE: {
				int entityId = _deserializer.readInt();
				int componentIndex = _deserializer.readInt();
				int fieldIndex = _deserializer.readInt();
				Object value = _deserializer.readSomething(true);
				_worldController.setComponentFieldValue(entityId, componentIndex, fieldIndex, value);
				break;
			}

			default: throw new RuntimeException("Unknown packet type: " + (int)packetType);
		}
	}

	@Override
	public void injectWorldController(WorldController controller) {
		_worldController = controller;
	}


	@Override
	public int getListeningBitset() {
		return ENTITY_ADDED | ENTITY_DELETED | ENTITY_SYSTEM_STATS;
	}

	@Override
	public void addedSystem(int index, String name, BitVector allTypes, BitVector oneTypes, BitVector notTypes) {
		send(
			beginPacket(TYPE_ADDED_ENTITY_SYSTEM)
			.addInt(index)
			.addString(name)
			.addBitVector(allTypes)
			.addBitVector(oneTypes)
			.addBitVector(notTypes)
		);
	}

	@Override
	public void addedManager(String name) {
		send(
			beginPacket(TYPE_ADDED_MANAGER)
			.addString(name)
		);
	}

	@Override
	public void addedComponentType(int index, ComponentTypeInfo info) {
		NetworkSerializer p =
			beginPacket(TYPE_ADDED_COMPONENT_TYPE)
			.addInt(index)
			.addString(info.name)
			.beginArray(info.fields.size());

		FieldInfo field;
		for (int i = 0, n = info.fields.size(); i < n; ++i) {
			field = info.fields.get(i);
			p.addBoolean(field.isAccessible);
			p.addString(field.fieldName);
			p.addString(field.classType);
			p.addBoolean(field.isArray);
			p.addInt(field.valueType);
		}

		send(p);
	}

	@Override
	public void updatedEntitySystem(int index, int entitiesCount, int maxEntitiesCount) {
		send(
			beginPacket(TYPE_UPDATED_ENTITY_SYSTEM)
			.addInt(index)
			.addInt(entitiesCount)
			.addInt(maxEntitiesCount)
		);
	}

	@Override
	public void addedEntity(int entityId, BitVector components) {
		send(
			beginPacket(TYPE_ADDED_ENTITY)
			.addInt(entityId)
			.addBitVector(components)
		);
	}

	@Override
	public void deletedEntity(int entityId) {
		send(
			beginPacket(TYPE_DELETED_ENTITY)
			.addInt(entityId)
		);
	}

	@Override
	public void updatedComponentState(int entityId, int componentIndex, Object[] values) {
		NetworkSerializer p =
			beginPacket(TYPE_UPDATED_COMPONENT_STATE)
			.addInt(entityId)
			.addInt(componentIndex)
			.beginArray(values.length);

		for (int i = 0, n = values.length; i < n; ++i) {
			p.addSomething(values[i], true);
		}

		send(p);
	}
}
