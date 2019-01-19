package net.namekdev.entity_tracker.utils.serialization

import net.namekdev.entity_tracker.utils.CommonBitVector

class ClientNetworkDeserializer : NetworkDeserializer<CommonBitVector>() {
    override fun readBitVector(): CommonBitVector? {
        if (checkNull()) {
            return null
        }

        checkType(DataType.BitVector)
        return readRawBitVector()
    }

    override fun readRawBitVector(): CommonBitVector {
        val allBitsCount = readRawShort()
        val bitVector = CommonBitVector(allBitsCount.toInt())

        var i = 0
        while (i < allBitsCount) {
            var value = readRawInt()

            val isLastPart = allBitsCount - i < Int.SIZE_BITS
            val nBits = if (isLastPart) allBitsCount % Int.SIZE_BITS else Int.SIZE_BITS

            var j = 0
            while (j < nBits) {
                if (value and 1 == 1) {
                    bitVector.set(i)
                }
                value = value shr 1
                ++j
                ++i
            }
        }

        return bitVector
    }

}