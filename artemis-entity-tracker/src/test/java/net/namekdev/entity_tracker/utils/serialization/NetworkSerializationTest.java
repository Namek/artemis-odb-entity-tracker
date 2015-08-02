package net.namekdev.entity_tracker.utils.serialization;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;

import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer.SerializeResult;

import org.junit.Before;
import org.junit.Test;

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
	public void testBitsets() {
		BitSet bitset1 = new BitSet();
		bitset1.set(0);
		bitset1.set(2);
		bitset1.set(5);
		bitset1.set(31);
		bitset1.set(32);
		bitset1.set(63);
		bitset1.set(64);
		bitset1.set(80);

		BitSet bitset2 = new BitSet();
		bitset2.set(1);
		bitset2.set(4);
		bitset2.set(74);

		BitSet bitset3 = new BitSet(20);
		for (int index : new int[] { 2, 3, 4, 5, 6, 7, 10, 11, 14, 15, 16, 18, 19 }) {
			bitset3.set(index);
		}

		BitSet bitset4 = new BitSet();
		bitset4.set(0);
		bitset4.set(7);
		bitset4.set(10);

		serializer.reset();
		serializer.addBitSet(bitset1);
		serializer.addBitSet(bitset2);
		serializer.addBitSet(bitset1);
		serializer.addBitSet(bitset3);
		serializer.addBitSet(bitset4);
		SerializeResult result = serializer.getResult();

		deserializer.setSource(result.buffer, 0, result.size);
		assertEquals(bitset1, deserializer.readBitSet());
		assertEquals(bitset2, deserializer.readBitSet());
		assertEquals(bitset1, deserializer.readBitSet());
		assertEquals(bitset3, deserializer.readBitSet());
		assertEquals(bitset4, deserializer.readBitSet());

		assertEquals(result.size, deserializer.getConsumedBytesCount());
	}
}
