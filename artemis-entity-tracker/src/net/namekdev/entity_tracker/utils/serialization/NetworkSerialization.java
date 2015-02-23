package net.namekdev.entity_tracker.utils.serialization;

public abstract class NetworkSerialization {
	protected final static byte TYPE_NULL = 3;
	protected final static byte TYPE_ARRAY = 6;

	public final static byte TYPE_BYTE = 10;
	public final static byte TYPE_SHORT = 11;
	public final static byte TYPE_INT = 12;
	public final static byte TYPE_LONG = 13;
	public final static byte TYPE_STRING = 14;
	public final static byte TYPE_BITSET = 17;


	public static NetworkSerializer createSerializer() {
		return new NetworkSerializer();
	}

	public static NetworkSerializer createSerializer(byte[] buffer) {
		return new NetworkSerializer(buffer);
	}

	public static NetworkDeserializer createDeserializer() {
		return new NetworkDeserializer();
	}
}
