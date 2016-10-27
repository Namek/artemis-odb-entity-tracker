package net.namekdev.entity_tracker.utils.serialization;

import com.artemis.utils.BitVector;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer.SerializeResult;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NetworkSerializationTest {
	NetworkSerializer serializer;
	NetworkDeserializer deserializer;


	@Before
	public void setup() {
		serializer = new NetworkSerializer();
		deserializer = new NetworkDeserializer();
	}

	@Test
	public void testSimpleTypes() {
		serializer.reset();
		serializer.addInt(124);
		serializer.addShort((short) 113);
		serializer.addInt(84);
		serializer.addByte((byte)4);
		serializer.addRawByte((byte)97);
		serializer.addRawByte((byte)222);
		serializer.addRawInt(-4);
		serializer.addRawInt(-2412424);
		serializer.addRawInt(1152);
		SerializeResult result = serializer.getResult();

		deserializer.setSource(result.buffer, 0, result.size);
		assertEquals(124, deserializer.readInt());
		assertEquals((short) 113, deserializer.readShort());
		assertEquals(84, deserializer.readInt());
		assertEquals((byte)4, deserializer.readByte());
		assertEquals((byte)97, deserializer.readRawByte());
		assertEquals((byte)222, deserializer.readRawByte());
		assertEquals(-4, deserializer.readRawInt());
		assertEquals(-2412424, deserializer.readRawInt());
		assertEquals(1152, deserializer.readRawInt());

		assertEquals(result.size, deserializer.getConsumedBytesCount());
	}

	@Test
	public void testBitVector() {
		BitVector bitVector1 = new BitVector();
		bitVector1.set(0);
		bitVector1.set(2);
		bitVector1.set(5);
		bitVector1.set(31);
		bitVector1.set(32);
		bitVector1.set(63);
		bitVector1.set(64);
		bitVector1.set(80);

		BitVector bitVector2 = new BitVector();
		bitVector2.set(1);
		bitVector2.set(4);
		bitVector2.set(74);

		BitVector bitVector3 = new BitVector(20);
		for (int index : new int[] { 2, 3, 4, 5, 6, 7, 10, 11, 14, 15, 16, 18, 19 }) {
			bitVector3.set(index);
		}

		BitVector bitVector4 = new BitVector();
		bitVector4.set(0);
		bitVector4.set(7);
		bitVector4.set(10);

		serializer.reset();
		serializer.addBitVector(bitVector1);
		serializer.addBitVector(bitVector2);
		serializer.addBitVector(bitVector1);
		serializer.addBitVector(bitVector3);
		serializer.addBitVector(bitVector4);
		SerializeResult result = serializer.getResult();

		deserializer.setSource(result.buffer, 0, result.size);
		assertEquals(bitVector1, deserializer.readBitVector());
		assertEquals(bitVector2, deserializer.readBitVector());
		assertEquals(bitVector1, deserializer.readBitVector());
		assertEquals(bitVector3, deserializer.readBitVector());
		assertEquals(bitVector4, deserializer.readBitVector());

		assertEquals(result.size, deserializer.getConsumedBytesCount());
	}

}
