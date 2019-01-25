package net.namekdev.entity_tracker.network

import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateInterfaceListener
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.utils.AutoSizedArray
import net.namekdev.entity_tracker.utils.CommonBitVector
import net.namekdev.entity_tracker.utils.serialization.ClientNetworkDeserializer
import net.namekdev.entity_tracker.utils.serialization.ClientNetworkSerializer
import net.namekdev.entity_tracker.utils.serialization.DataType

/**
 * Used by UI (client).
 *
 * @author Namek
 */
class ExternalInterfaceCommunicator(
    private val _listener: IWorldUpdateInterfaceListener<CommonBitVector>
) : Communicator(), IWorldController {
    //	private final ArrayPool<Object> _objectArrayPool = new ArrayPool<>(Object.class);
    private val _componentTypes = AutoSizedArray<ComponentTypeInfo>()

    private val _serializer = ClientNetworkSerializer()
    private val _deserializer = ClientNetworkDeserializer()


    override fun connected(identifier: String, output: RawConnectionOutputListener) {
        super.connected(identifier, output)
        _listener.injectWorldController(this)
    }

    override fun disconnected() {
        _listener.disconnected()
    }

    override fun bytesReceived(bytes: ByteArray, offset: Int, length: Int) {
        _deserializer.setSource(bytes, offset, length)

        val packetType = _deserializer.readRawByte()

        when (packetType) {
            Communicator.TYPE_ADDED_ENTITY_SYSTEM -> {
                val index = _deserializer.readInt()
                val name = _deserializer.readString()!!
                val allTypes = _deserializer.readBitVector()
                val oneTypes = _deserializer.readBitVector()
                val notTypes = _deserializer.readBitVector()
                _listener.addedSystem(index, name, allTypes, oneTypes, notTypes)
            }
            Communicator.TYPE_ADDED_MANAGER -> {
                val name = _deserializer.readString()!!
                _listener.addedManager(name)
            }
            Communicator.TYPE_ADDED_COMPONENT_TYPE -> {
                val index = _deserializer.readInt()
                val name = _deserializer.readString()

                val info = ComponentTypeInfo(name as String)
                info.index = index
                info.model = _deserializer.readDataDescription()
                _componentTypes.set(index, info)

                _listener.addedComponentType(index, info)
            }
            Communicator.TYPE_UPDATED_ENTITY_SYSTEM -> {
                val index = _deserializer.readInt()
                val entitiesCount = _deserializer.readInt()
                val maxEntitiesCount = _deserializer.readInt()
                _listener.updatedEntitySystem(index, entitiesCount, maxEntitiesCount)
            }
            Communicator.TYPE_ADDED_ENTITY -> {
                val entityId = _deserializer.readInt()
                val components = _deserializer.readBitVector()!!
                _listener.addedEntity(entityId, components)
            }
            Communicator.TYPE_DELETED_ENTITY -> {
                val entityId = _deserializer.readInt()
                _listener.deletedEntity(entityId)
            }
            Communicator.TYPE_UPDATED_COMPONENT_STATE -> {
                val entityId = _deserializer.readInt()
                val index = _deserializer.readInt()
                val componentModel = _componentTypes.get(index).model
                val valueTree = _deserializer.readObject(componentModel!!)

                _listener.updatedComponentState(entityId, index, valueTree)
            }

            else -> throw RuntimeException("Unknown packet type: " + packetType.toInt())
        }
    }

    private fun send(serializer: ClientNetworkSerializer) {
        val data = serializer.endPacket()
        output?.send(data.buffer, 0, data.size)
    }

    private fun beginPacket(packetType: Byte): ClientNetworkSerializer {
        return _serializer
            .beginPacket()
            .addRawByte(packetType)
    }

    override fun setSystemState(name: String, isOn: Boolean) {
        send(
            beginPacket(Communicator.TYPE_SET_SYSTEM_STATE)
                .addString(name)
                .addBoolean(isOn)
        )
    }

    override fun setManagerState(name: String, isOn: Boolean) {
        send(
            beginPacket(Communicator.TYPE_SET_MANAGER_STATE)
                .addString(name)
                .addBoolean(isOn)
        )
    }

    override fun requestComponentState(entityId: Int, componentIndex: Int) {
        send(
            beginPacket(Communicator.TYPE_REQUEST_COMPONENT_STATE)
                .addInt(entityId)
                .addInt(componentIndex)
        )
    }

    override fun setComponentFieldValue(entityId: Int, componentIndex: Int, treePath: IntArray, valueType: DataType, value: Any?) {
        val p = beginPacket(Communicator.TYPE_SET_COMPONENT_FIELD_VALUE)
            .addInt(entityId)
            .addInt(componentIndex)
            .addArray(treePath)
            .addFlatByType(valueType, value)

        send(p)
    }

    override fun setComponentStateWatcher(entityId: Int, componentIndex: Int, enabled: Boolean) {
        val p = beginPacket(Communicator.TYPE_SET_COMPONENT_STATE_WATCHER)
            .addInt(entityId)
            .addInt(componentIndex)
            .addBoolean(enabled)

        send(p)
    }
}
