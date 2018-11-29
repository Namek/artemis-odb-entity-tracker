package net.namekdev.entity_tracker.network.communicator

import net.namekdev.entity_tracker.network.base.RawConnectionCommunicator
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer

/**
 * Defines basics of network protocol for communication between EntityTracker Manager and external UI.

 * @author Namek
 */
abstract class Communicator : RawConnectionCommunicator {
    protected lateinit var _output: RawConnectionOutputListener
    protected val _serializer = NetworkSerialization.createSerializer()
    protected val _deserializer = NetworkSerialization.createDeserializer()


    override fun connected(identifier: String, output: RawConnectionOutputListener) {
        _output = output
    }

    override fun disconnected() {}

    protected fun send(serializer: NetworkSerializer) {
        val data = serializer.result
        _output.send(data.buffer, 0, data.size)
    }

    protected fun beginPacket(packetType: Byte): NetworkSerializer {
        return _serializer.reset().addRawByte(packetType)
    }

    companion object {

        // tracker events
        const val TYPE_ADDED_ENTITY_SYSTEM: Byte = 60
        const val TYPE_ADDED_MANAGER: Byte = 61
        const val TYPE_ADDED_COMPONENT_TYPE: Byte = 63
        const val TYPE_UPDATED_ENTITY_SYSTEM: Byte = 64
        const val TYPE_ADDED_ENTITY: Byte = 68
        const val TYPE_DELETED_ENTITY: Byte = 73
        const val TYPE_UPDATED_COMPONENT_STATE: Byte = 104

        // UI requests
        const val TYPE_SET_SYSTEM_STATE: Byte = 90
        const val TYPE_SET_MANAGER_STATE: Byte = 94
        const val TYPE_REQUEST_COMPONENT_STATE: Byte = 103
        const val TYPE_SET_COMPONENT_FIELD_VALUE: Byte = 113
    }
}
