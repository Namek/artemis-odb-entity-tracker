package net.namekdev.entity_tracker.utils.serialization;

import com.artemis.utils.BitVector;

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
	public final static byte Type_BITVECTOR = 20;//takes minimum 4 bytes


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
		else if (type.equals(BitVector.class)) {
			netType = Type_BITVECTOR;
		}

		return netType;
	}

	public static Object convertStringToTypedValue(String value, int valueType) {
		switch (valueType) {
			case TYPE_BYTE: return Byte.valueOf(value);
			case TYPE_SHORT: return Short.valueOf(value);
			case TYPE_INT: return Integer.valueOf(value);
			case TYPE_LONG: return Long.valueOf(value);
			case TYPE_STRING: return value;
			case TYPE_BOOLEAN: return Boolean.valueOf(value);
			case TYPE_FLOAT: return Float.valueOf(value);
			case TYPE_DOUBLE: return Double.valueOf(value);
			case Type_BITVECTOR: return new BitVector(Integer.valueOf(value));
			case TYPE_ARRAY: throw new UnsupportedOperationException("arrays are not supported (yet?)");
			default: return null;
		}
	}
}
