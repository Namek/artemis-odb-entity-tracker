package net.namekdev.entity_tracker.utils.serialization

import com.artemis.utils.BitVector
import org.junit.Assert.*

import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer.SerializationResult
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.determineType
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.isSimpleType
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.DataType
import org.junit.Before
import org.junit.Test

import org.junit.Assert.assertEquals

class NetworkSerializationTest {
    lateinit var deserializer: NetworkDeserializer


    @Before
    fun setup() {
        deserializer = NetworkDeserializer()
    }

    @Test
    fun deserialize_simple_types() {
        val serializer = NetworkSerializer().reset()

        serializer.addInt(124)
        serializer.addShort(113.toShort())
        serializer.addInt(84)
        serializer.addByte(4.toByte())
        serializer.addRawByte(97.toByte())
        serializer.addRawByte(222.toByte())
        serializer.addRawInt(-4)
        serializer.addRawInt(-2412424)
        serializer.addRawInt(1152)
        val result = serializer.result

        deserializer.setSource(result.buffer, 0, result.size)
        assertEquals(124, deserializer.readInt().toLong())
        assertEquals(113.toShort().toLong(), deserializer.readShort().toLong())
        assertEquals(84, deserializer.readInt().toLong())
        assertEquals(4.toByte().toLong(), deserializer.readByte().toLong())
        assertEquals(97.toByte().toLong(), deserializer.readRawByte().toLong())
        assertEquals(222.toByte().toLong(), deserializer.readRawByte().toLong())
        assertEquals(-4, deserializer.readRawInt().toLong())
        assertEquals(-2412424, deserializer.readRawInt().toLong())
        assertEquals(1152, deserializer.readRawInt().toLong())

        assertEquals(result.size.toLong(), deserializer.consumedBytesCount.toLong())
    }

    @Test
    fun testBitVector() {
        val serializer = NetworkSerializer().reset()

        val bitVector1 = BitVector()
        bitVector1.set(0)
        bitVector1.set(2)
        bitVector1.set(5)
        bitVector1.set(31)
        bitVector1.set(32)
        bitVector1.set(63)
        bitVector1.set(64)
        bitVector1.set(80)

        val bitVector2 = BitVector()
        bitVector2.set(1)
        bitVector2.set(4)
        bitVector2.set(74)


        // just make sure that bitsets are comparable
        assertNotEquals(bitVector1, bitVector2)
        assertEquals(bitVector1, bitVector1)

        val bitVector3 = BitVector(20)
        for (index in intArrayOf(2, 3, 4, 5, 6, 7, 10, 11, 14, 15, 16, 18, 19)) {
            bitVector3.set(index)
        }

        val bitVector4 = BitVector()
        bitVector4.set(0)
        bitVector4.set(7)
        bitVector4.set(10)

        serializer.addBitVector(bitVector1)
        serializer.addBitVector(bitVector2)
        serializer.addBitVector(bitVector1)
        serializer.addBitVector(bitVector3)
        serializer.addBitVector(bitVector4)

        val result = serializer.result

        deserializer.setSource(result.buffer, 0, result.size)
        assertEquals(bitVector1, deserializer.readBitVector())
        assertEquals(bitVector2, deserializer.readBitVector())
        assertEquals(bitVector1, deserializer.readBitVector())
        assertEquals(bitVector3, deserializer.readBitVector())
        assertEquals(bitVector4, deserializer.readBitVector())

        assertEquals(result.size.toLong(), deserializer.consumedBytesCount.toLong())
    }

    @Test
    fun determine_simple_types() {
        assertEquals(DataType.Byte, determineType(2.toByte().javaClass).first)
        assertEquals(DataType.Short, determineType(2.toShort().javaClass).first)
        assertEquals(DataType.Int, determineType(2.javaClass).first)
        assertEquals(DataType.Long, determineType(2.toLong().javaClass).first)
        assertEquals(DataType.Boolean, determineType(true.javaClass).first)
        assertEquals(DataType.Double, determineType(2.toDouble().javaClass).first)
        assertEquals(DataType.Float, determineType(2.toFloat().javaClass).first)

    }
}
