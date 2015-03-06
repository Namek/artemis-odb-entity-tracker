package net.namekdev.entity_tracker.utils.serialization;

import java.util.BitSet;

public class NetworkSerializer extends NetworkSerialization {
	private byte[] _ourBuffer;
	private byte[] _buffer;
	private int _pos;

	private final SerializeResult _serializeResult = new SerializeResult();


	public NetworkSerializer() {
		this(new byte[10240]);
	}

	public NetworkSerializer(byte[] buffer) {
		_ourBuffer = buffer;
	}

	public NetworkSerializer reset() {
		return reset(_ourBuffer);
	}

	public NetworkSerializer reset(byte[] buffer) {
		_pos = 0;
		_buffer = buffer;
//		_buffer[_pos++] = PACKET_BEGIN;
		return this;
	}

	public NetworkSerializer beginArray(byte elementType, int length) {
		_buffer[_pos++] = TYPE_ARRAY;
		_buffer[_pos++] = elementType;
		addRawInt(length);

		return this;
	}

	public NetworkSerializer beginArray(int length) {
		return beginArray(TYPE_UNKNOWN, length);
	}

	public NetworkSerializer addByte(byte value) {
		_buffer[_pos++] = TYPE_BYTE;
		_buffer[_pos++] = value;
		return this;
	}

	public NetworkSerializer addRawByte(byte value) {
		_buffer[_pos++] = value;
		return this;
	}

	public NetworkSerializer addShort(short value) {
		_buffer[_pos++] = TYPE_SHORT;
		addRawShort(value);
		return this;
	}

	protected void addRawShort(short value) {
		_buffer[_pos++] = (byte) ((value >> 8) & 0xFF);
		_buffer[_pos++] = (byte) (value & 0xFF);
	}

	public NetworkSerializer addInt(int value) {
		_buffer[_pos++] = TYPE_INT;
		addRawInt(value);
		return this;
	}

	protected void addRawInt(int value) {
		_buffer[_pos++] = (byte) ((value >> 24) & 0xFF);
		_buffer[_pos++] = (byte) ((value >> 16) & 0xFF);
		_buffer[_pos++] = (byte) ((value >> 8) & 0xFF);
		_buffer[_pos++] = (byte) (value & 0xFF);
	}

	public NetworkSerializer addString(String value) {
		if (tryAddNullable(value)) {
			return this;
		}

		_buffer[_pos++] = TYPE_STRING;

		int n = value.length();
		addRawInt(n);

		for (int i = 0; i < n; ++i) {
			_buffer[_pos++] = (byte) (value.charAt(i) & 0xFF);
		}

		return this;
	}

	public NetworkSerializer addBoolean(boolean value) {
		_buffer[_pos++] = TYPE_BOOLEAN;
		return addRawByte((byte) (value ? 1 : 0));
	}

	public NetworkSerializer addBitSet(BitSet bitset) {
		if (tryAddNullable(bitset)) {
			return this;
		}

		_buffer[_pos++] = TYPE_BITSET;

		int bitsCount = bitset.length();
		addRawShort((short) bitsCount);

		int i = 0, value;
		while (i < bitsCount) {
			value = 0;
			for (int j = 0; j < Integer.SIZE && j < bitsCount; ++j, ++i) {
				boolean bit = bitset.get(i);

				if (bit) {
					value |= 1 << j;
				}
			}

			addRawInt(value);
		}

		return this;
	}

	protected boolean tryAddNullable(Object data) {
		if (data == null) {
			_buffer[_pos++] = TYPE_NULL;
			return true;
		}

		return false;
	}

	public SerializeResult getResult() {
//		_buffer[_pos++] = PACKET_END;

		return _buffer == _ourBuffer
			? _serializeResult.setup(_buffer, _pos)
			: new SerializeResult(_buffer, _pos);
	}


	public static class SerializeResult {
		public byte[] buffer;
		public int size;

		private SerializeResult() {
		}

		private SerializeResult(byte[] buffer, int size) {
			this.buffer = buffer;
			this.size = size;
		}

		private SerializeResult setup(byte[] buffer, int size) {
			this.buffer = buffer;
			this.size = size;
			return this;
		}
	}
}
