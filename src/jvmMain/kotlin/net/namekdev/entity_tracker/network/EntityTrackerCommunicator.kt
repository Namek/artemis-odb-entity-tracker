package net.namekdev.entity_tracker.network

import com.artemis.utils.BitVector
import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.utils.AutoSizedArray
import net.namekdev.entity_tracker.utils.serialization.DataType
import net.namekdev.entity_tracker.utils.serialization.JvmDeserializer
import net.namekdev.entity_tracker.utils.serialization.JvmSerializer
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector

/**
 * Deserializes data from network and serializes data sent to the network.
 * Manages between logic events and pure network bytes.
 *
 * Used by EntityTracker manager (server).
 *
 * @author Namek
 */
open class EntityTrackerCommunicator(inspector: ObjectTypeInspector)
    : Communicator(), IWorldUpdateListener<BitVector>
{
    private lateinit var _worldController: IWorldController
    private val _componentTypes = AutoSizedArray<ComponentTypeInfo>()

    private val _serializer = JvmSerializer(inspector)
    private val _deserializer = JvmDeserializer()


    override fun bytesReceived(bytes: ByteArray, offset: Int, length: Int) {
        _deserializer.setSource(bytes, offset, length)

        val packetType = _deserializer.readRawByte()

        when (packetType) {
            Communicator.TYPE_SET_SYSTEM_STATE -> {
                val systemName = _deserializer.readString()!!
                val isSystemOn = _deserializer.readBoolean()
                _worldController.setSystemState(systemName, isSystemOn)
            }
            Communicator.TYPE_SET_MANAGER_STATE -> {
                val managerName = _deserializer.readString()!!
                val isManagerOn = _deserializer.readBoolean()
                _worldController.setManagerState(managerName, isManagerOn)
            }
            Communicator.TYPE_DELETE_ENTITY -> {
                val entityId = _deserializer.readInt()
                _worldController.deleteEntity(entityId)
            }
            Communicator.TYPE_REQUEST_COMPONENT_STATE -> {
                val entityId = _deserializer.readInt()
                val componentIndex = _deserializer.readInt()
                _worldController.requestComponentState(entityId, componentIndex)
            }
            Communicator.TYPE_SET_COMPONENT_FIELD_VALUE -> {
                val entityId = _deserializer.readInt()
                val componentIndex = _deserializer.readInt()
                val treePath = _deserializer.readPrimitiveIntArray()
                val valueType = _deserializer.readType()
                val value =
                    if (valueType != DataType.Null)
                        _deserializer.readFlatByType(valueType)
                    else null

                _worldController.setComponentFieldValue(entityId, componentIndex, treePath, valueType, value)
            }
            Communicator.TYPE_SET_COMPONENT_STATE_WATCHER -> {
                val entityId = _deserializer.readInt()
                val componentIndex = _deserializer.readInt()
                val enabled = _deserializer.readBoolean()

                // TODO we need to pass the client's id who wants to add the watcher. Thus, IWorldController interface does not fit here.
                _worldController.setComponentStateWatcher(entityId, componentIndex, enabled)
            }

            else -> throw RuntimeException("Unknown packet type: " + packetType.toInt())
        }
    }

    override fun injectWorldController(controller: IWorldController) {
        _worldController = controller
    }


    private fun send(serializer: JvmSerializer) {
        val data = serializer.endPacket()
        output?.send(data.buffer, 0, data.size)
    }

    private fun beginPacket(packetType: Byte): JvmSerializer {
        return _serializer
            .beginPacket()
            .addRawByte(packetType)
    }

    override fun addedSystem(index: Int, name: String, allTypes: BitVector?, oneTypes: BitVector?, notTypes: BitVector?) {
        send(
            beginPacket(Communicator.TYPE_ADDED_ENTITY_SYSTEM)
                .addInt(index)
                .addString(name)
                .addBitVectorOrNull(allTypes)
                .addBitVectorOrNull(oneTypes)
                .addBitVectorOrNull(notTypes)
        )
    }

    override fun addedManager(name: String) {
        send(
            beginPacket(Communicator.TYPE_ADDED_MANAGER)
                .addString(name)
        )
    }

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
        _componentTypes.set(index, info)

        val p = beginPacket(Communicator.TYPE_ADDED_COMPONENT_TYPE)
            .addInt(index)
            .addString(info.name)
            .addDataDescriptionOrRef(info.model)

        send(p)
    }

    override fun updatedEntitySystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int) {
        send(
            beginPacket(Communicator.TYPE_UPDATED_ENTITY_SYSTEM)
                .addInt(index)
                .addInt(entitiesCount)
                .addInt(maxEntitiesCount)
        )
    }

    override fun addedEntity(entityId: Int, components: BitVector) {
        send(
            beginPacket(Communicator.TYPE_ADDED_ENTITY)
                .addInt(entityId)
                .addBitVector(components)
        )
    }

    override fun deletedEntity(entityId: Int) {
        send(
            beginPacket(Communicator.TYPE_DELETED_ENTITY)
                .addInt(entityId)
        )
    }

    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
        val p = beginPacket(Communicator.TYPE_UPDATED_COMPONENT_STATE)
            .addInt(entityId)
            .addInt(componentIndex)
            .addObject(_componentTypes.get(componentIndex).model, valueTree)

        send(p)
    }
}