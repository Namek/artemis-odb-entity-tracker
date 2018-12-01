package net.namekdev.entity_tracker.utils.serialization

import com.artemis.utils.BitVector

class JvmDeserializer : NetworkDeserializer<BitVector>() {
    override fun readBitVector(): BitVector? {
        if (checkNull()) {
            return null
        }

        checkType(DataType.BitVector)

        val allBitsCount = readRawShort()
        val bitVector = BitVector(allBitsCount.toInt())

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