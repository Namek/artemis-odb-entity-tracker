package net.namekdev.entity_tracker.utils.serialization;

import java.util.BitSet;

public abstract class NetworkSerialization {
	public final static byte TYPE_UNKNOWN = 1;
	protected final static byte TYPE_NULL = 3;
	protected final static byte TYPE_ARRAY = 6;

	public final static byte TYPE_BYTE = 10;
	public final static byte TYPE_SHORT = 11;
	public final static byte TYPE_INT = 12;
	public final static byte TYPE_LONG = 13;
	public final static byte TYPE_STRING = 14;
	public final static byte TYPE_BOOLEAN = 15;//takes 1 byte
	public final static byte TYPE_FLOAT = 16;
	public final static byte TYPE_DOUBLE = 17;
	public final static byte TYPE_BITSET = 20;//takes minimum 4 bytes


	public static NetworkSerializer createSerializer() {
		return new NetworkSerializer();
	}

	public static NetworkSerializer createSerializer(byte[] buffer) {
		return new NetworkSerializer(buffer);
	}

	public static NetworkDeserializer createDeserializer() {
		return new NetworkDeserializer();
	}

	public static int determineNetworkType(Class<?> type) {
		int netType = TYPE_UNKNOWN;

		if (type.equals(byte.class)) {
			netType = TYPE_BYTE;
		}
		else if (type.equals(short.class)) {
			netType = TYPE_SHORT;
		}
		else if (type.equals(int.class)) {
			netType = TYPE_INT;
		}
		else if (type.equals(long.class)) {
			netType = TYPE_LONG;
		}
		else if (type.equals(String.class)) {
			netType = TYPE_STRING;
		}
		else if (type.equals(boolean.class)) {
			netType = TYPE_BOOLEAN;
		}
		else if (type.equals(float.class)) {
			netType = TYPE_FLOAT;
		}
		else if (type.equals(float.class)) {
			netType = TYPE_DOUBLE;
		}
		else if (type.equals(BitSet.class)) {
			netType = TYPE_BITSET;
		}

		return netType;
	}
}
