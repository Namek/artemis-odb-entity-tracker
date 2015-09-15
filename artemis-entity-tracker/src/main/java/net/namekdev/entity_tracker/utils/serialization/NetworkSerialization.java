package net.namekdev.entity_tracker.utils.serialization;

import java.util.BitSet;

public abstract class NetworkSerialization {
	public final static byte TYPE_UNKNOWN = 1;
	protected final static byte TYPE_NULL = 3;
	public final static byte TYPE_ARRAY = 6;
	public final static byte TYPE_TREE_DESCR = 7;
	public final static byte TYPE_TREE = 9;

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

	public static byte determineSimpleType(Class<?> type) {
		byte netType = TYPE_UNKNOWN;

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

	public static Object convertStringToTypedValue(String value, byte valueType) {
		switch (valueType) {
			case TYPE_BYTE: return Byte.valueOf(value);
			case TYPE_SHORT: return Short.valueOf(value);
			case TYPE_INT: return Integer.valueOf(value);
			case TYPE_LONG: return Long.valueOf(value);
			case TYPE_STRING: return value;
			case TYPE_BOOLEAN: return Boolean.valueOf(value);
			case TYPE_FLOAT: return Float.valueOf(value);
			case TYPE_DOUBLE: return Double.valueOf(value);
			case TYPE_BITSET: return new BitSet(Integer.valueOf(value));
			case TYPE_ARRAY: throw new UnsupportedOperationException("arrays are not supported (yet?)");
			default: return null;
		}
	}

	public static boolean isSimpleType(byte valueType) {
		switch (valueType) {
			case TYPE_BYTE: return true;
			case TYPE_SHORT: return true;
			case TYPE_INT: return true;
			case TYPE_LONG: return true;
			case TYPE_STRING: return true;
			case TYPE_BOOLEAN: return true;
			case TYPE_FLOAT: return true;
			case TYPE_DOUBLE: return true;
			case TYPE_BITSET: return true;
			default: return false;
		}
	}
}
