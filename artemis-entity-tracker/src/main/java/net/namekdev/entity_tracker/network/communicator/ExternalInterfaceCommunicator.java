package net.namekdev.entity_tracker.network.communicator;

import java.net.SocketAddress;
import java.util.BitSet;

import net.namekdev.entity_tracker.connectors.WorldController;
import net.namekdev.entity_tracker.connectors.WorldUpdateInterfaceListener;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.model.FieldInfo;
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener;
import net.namekdev.entity_tracker.utils.ArrayPool;

/**
 * Communicator used by UI (client).
 *
 * @author Namek
 */
public class ExternalInterfaceCommunicator extends Communicator implements WorldController {
	private WorldUpdateInterfaceListener _listener;
	private final ArrayPool<Object> _objectArrayPool = new ArrayPool<>(Object.class);


	public ExternalInterfaceCommunicator(WorldUpdateInterfaceListener listener) {
		_listener = listener;
	}

	@Override
	public void connected(SocketAddress remoteAddress, RawConnectionOutputListener output) {
		super.connected(remoteAddress, output);
		_listener.injectWorldController(this);
	}

	@Override
	public void disconnected() {
		_listener.disconnected();
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
				int size = _deserializer.beginArray();

				ComponentTypeInfo info = new ComponentTypeInfo(name);
				info.index = index;
				info.fields.ensureCapacity(size);

				for (int i = 0; i < size; ++i) {
					FieldInfo field = new FieldInfo();
					field.isAccessible = _deserializer.readBoolean();
					field.fieldName = _deserializer.readString();
					field.classType = _deserializer.readString();
					field.isArray = _deserializer.readBoolean();
					field.valueType = _deserializer.readInt();

					info.fields.insertElementAt(field, i);
				}

				_listener.addedComponentType(index, info);
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
			case TYPE_UPDATED_COMPONENT_STATE: {
				int entityId = _deserializer.readInt();
				int index = _deserializer.readInt();
				int size = _deserializer.beginArray();

				Object[] values = _objectArrayPool.obtain(size, true);

				for (int i = 0; i < size; ++i) {
					values[i] = _deserializer.readSomething(true);
				}

				_listener.updatedComponentState(entityId, index, values);
				_objectArrayPool.free(values, true);

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

	@Override
	public void requestComponentState(int entityId, int componentIndex) {
		send(
			beginPacket(TYPE_REQUEST_COMPONENT_STATE)
			.addInt(entityId)
			.addInt(componentIndex)
		);
	}

	@Override
	public void setComponentFieldValue(int entityId, int componentIndex, int fieldIndex, Object value) {
		send(
			beginPacket(TYPE_SET_COMPONENT_FIELD_VALUE)
			.addInt(entityId)
			.addInt(componentIndex)
			.addInt(fieldIndex)
			.addSomething(value)
		);
	}
}
