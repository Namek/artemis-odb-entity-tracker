package net.namekdev.entity_tracker.network.communicator;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;

import java.net.SocketAddress;

import com.artemis.utils.BitVector;
import net.namekdev.entity_tracker.connectors.WorldController;
import net.namekdev.entity_tracker.connectors.WorldUpdateInterfaceListener;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.model.FieldInfo;
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener;
import net.namekdev.entity_tracker.utils.Array;
import net.namekdev.entity_tracker.utils.ArrayPool;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer;
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode;
import net.namekdev.entity_tracker.utils.serialization.ValueTree;

/**
 * Communicator used by UI (client).
 *
 * @author Namek
 */
public class ExternalInterfaceCommunicator extends Communicator implements WorldController {
	private WorldUpdateInterfaceListener _listener;
//	private final ArrayPool<Object> _objectArrayPool = new ArrayPool<>(Object.class);
	private final Array<ComponentTypeInfo> _componentTypes = new Array<>();


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
				BitVector allTypes = _deserializer.readBitVector();
				BitVector oneTypes = _deserializer.readBitVector();
				BitVector notTypes = _deserializer.readBitVector();
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

				ComponentTypeInfo info = new ComponentTypeInfo(name);
				info.index = index;
				info.model = _deserializer.readDataDescription();
				_componentTypes.set(index, info);

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
				BitVector components = _deserializer.readBitVector();
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
				ObjectModelNode componentModel = _componentTypes.get(index).model;
				ValueTree valueTree = _deserializer.readObject(componentModel);

				_listener.updatedComponentState(entityId, index, valueTree);
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
	public void setManagerState(String name, boolean isOn) {
		send(
			beginPacket(TYPE_SET_MANAGER_STATE)
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
	public void setComponentFieldValue(int entityId, int componentIndex, int[] treePath, Object value) {
		NetworkSerializer p =
			beginPacket(TYPE_SET_COMPONENT_FIELD_VALUE)
			.addInt(entityId)
			.addInt(componentIndex)
			.addSomething(value);

		p.beginArray(TYPE_INT, treePath.length);
		for (int i = 0; i < treePath.length; ++i) {
			p.addInt(treePath[i]);
		}

		send(p);
	}
}
