package net.namekdev.entity_tracker.utils.serialization

import net.namekdev.entity_tracker.utils.CommonBitVector

class ClientNetworkSerializer : NetworkSerializer<ClientNetworkSerializer, CommonBitVector>() {


    override fun isBitVector(obj: Any): Boolean {
        return false
    }

    override fun addBitVector(bitVector: CommonBitVector): ClientNetworkSerializer {
        // TODO

        return this
    }
}