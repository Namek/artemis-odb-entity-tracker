package net.namekdev.entity_tracker.network.communicator

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*

import java.net.SocketAddress

import com.artemis.utils.BitVector
import net.namekdev.entity_tracker.connectors.WorldController
import net.namekdev.entity_tracker.connectors.WorldUpdateInterfaceListener
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.model.FieldInfo
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener
import net.namekdev.entity_tracker.utils.AutoSizedArray
import net.namekdev.entity_tracker.utils.ArrayPool
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ValueTree

/**
 * Communicator used by UI (client).

 * @author Namek
 */
class ExternalInterfaceCommunicator(
    private val _listener: WorldUpdateInterfaceListener
) : Communicator(), WorldController {
    //	private final ArrayPool<Object> _objectArrayPool = new ArrayPool<>(Object.class);
    private val _componentTypes = AutoSizedArray<ComponentTypeInfo>()

    override fun connected(remoteAddress: SocketAddress, output: RawConnectionOutputListener) {
        super.connected(remoteAddress, output)
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

    override fun setComponentFieldValue(entityId: Int, componentIndex: Int, treePath: IntArray, value: Any) {
        val p = beginPacket(Communicator.TYPE_SET_COMPONENT_FIELD_VALUE)
            .addInt(entityId)
            .addInt(componentIndex)
            .addSomething(value)

        p.beginArray(DataType.Int, treePath.size, true)
        for (i in treePath.indices) {
            p.addInt(treePath[i])
        }

        send(p)
    }
}
