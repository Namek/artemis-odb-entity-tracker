package net.namekdev.entity_tracker.utils.serialization;

import com.artemis.utils.BitVector;

public abstract class NetworkSerialization {
	public static enum Type {
		Unknown,
		Null,
		Array,
		Description,
		MultipleDescriptions,
		DescriptionRef,
		Object,
		Enum,
		EnumDescription,
		EnumValue,
		
		Byte,
		Short,
		Int,
		Long,
		String,
		Boolean, //takes 1 byte
		Float,
		Double,
		BitVector //takes minimum 4 bytes
	}

	public static NetworkSerializer createSerializer() {
		return new NetworkSerializer();
	}

	public static NetworkDeserializer createDeserializer() {
		return new NetworkDeserializer();
	}

	public static Type determineType(Class<?> type) {
		Type netType = Type.Unknown;

		if (type.equals(byte.class) || type.equals(Byte.class))
			netType = Type.Byte;
		else if (type.equals(short.class) || type.equals(Short.class))
			netType = Type.Short;
		else if (type.equals(int.class) || type.equals(Integer.class))
			netType = Type.Int;
		else if (type.equals(long.class) || type.equals(Long.class))
			netType = Type.Long;
		else if (type.equals(String.class))
			netType = Type.String;
		else if (type.equals(boolean.class) || type.equals(Boolean.class))
			netType = Type.Boolean;
		else if (type.equals(float.class) || type.equals(Float.class))
			netType = Type.Float;
		else if (type.equals(double.class) || type.equals(Double.class))
			netType = Type.Double;
		else if (type.equals(BitVector.class))
			netType = Type.BitVector;
		else if (type.isEnum())
			netType = Type.Enum;

		return netType;
	}

	public static Object convertStringToTypedValue(String value, Type valueType) {
		switch (valueType) {
			case Byte: return Byte.valueOf(value);
			case Short: return Short.valueOf(value);
			case Int: return Integer.valueOf(value);
			case Long: return Long.valueOf(value);
			case String: return value;
			case Boolean: return Boolean.valueOf(value);
			case Float: return Float.valueOf(value);
			case Double: return Double.valueOf(value);
			case Enum: throw new UnsupportedOperationException("probably unsupported, not sure");
			case BitVector: return new BitVector(Integer.valueOf(value));
			case Array: throw new UnsupportedOperationException("arrays are not supported (yet?)");
			default: return null;
		}
	}

	public static boolean isSimpleType(Type valueType) {
		if (valueType == null)
			return false;

		switch (valueType) {
			case Byte: return true;
			case Short: return true;
			case Int: return true;
			case Long: return true;
			case String: return true;
			case Boolean: return true;
			case Float: return true;
			case Double: return true;
			case BitVector: return true;
			default: return false;
		}
	}
}
