package net.namekdev.entity_tracker.utils.serialization;

import com.artemis.utils.BitVector;

public class NetworkDeserializer extends NetworkSerialization {
	private byte[] _source;
	private int _sourcePos, _sourceBeginPos;


	public NetworkDeserializer() {
	}

	public void setSource(byte[] bytes, int offset, int length) {
		_source = bytes;
		_sourcePos = offset;
		_sourceBeginPos = offset;
	}

	public int getConsumedBytesCount() {
		return _sourcePos - _sourceBeginPos;
	}

	public int beginArray(byte elementType) {
		checkType(TYPE_ARRAY);
		checkType(elementType);
		return readRawInt();
	}

	public int beginArray() {
		return beginArray(TYPE_UNKNOWN);
	}

	public byte readByte() {
		checkType(TYPE_BYTE);
		return readRawByte();
	}

	public short readShort() {
		checkType(TYPE_SHORT);
		return readRawShort();
	}

	public int readInt() {
		checkType(TYPE_INT);
		return readRawInt();
	}

	public long readLong() {
		checkType(TYPE_LONG);
		return readRawLong();
	}

	public long readRawLong() {
		long value = readRawInt();
		value <<= 32;
		value |= readRawInt();

		return value;
	}

	public String readString() {
		if (checkNull()) {
			return null;
		}

		checkType(TYPE_STRING);
		int length = readRawInt();

		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; ++i) {
			sb.append((char) (_source[_sourcePos++] & 0xFF));
		}

		return sb.toString();
	}

	public boolean readBoolean() {
		checkType(TYPE_BOOLEAN);

		byte value = readRawByte();
		return value != 0;
	}

	public float readFloat() {
		checkType(TYPE_FLOAT);
		return readRawFloat();
	}

	public float readRawFloat() {
		return Float.intBitsToFloat(readRawInt());
	}

	public double readDouble() {
		checkType(TYPE_DOUBLE);
		return readRawDouble();
	}

	public double readRawDouble() {
		return Double.longBitsToDouble(readRawLong());
	}

	public BitVector readBitVector() {
		if (checkNull()) {
			return null;
		}

		checkType(Type_BITVECTOR);

		final short allBitsCount = readRawShort();
		final BitVector bitVector = new BitVector(allBitsCount);

		int i = 0;
		while (i < allBitsCount) {
			int value = readRawInt();

			final boolean isLastPart = allBitsCount - i < Integer.SIZE;
			final int nBits = isLastPart ? allBitsCount % Integer.SIZE : Integer.SIZE;

			for (int j = 0; j < nBits; ++j, ++i) {
				if ((value & 1) == 1) {
					bitVector.set(i);
				}
				value >>= 1;
			}
		}

		return bitVector;
	}

	public byte readRawByte() {
		return _source[_sourcePos++];
	}

	public short readRawShort() {
		short value = (short) (_source[_sourcePos++] & 0xFF);
		value <<= 8;
		value |= _source[_sourcePos++] & 0xFF;

		return value;
	}

	public Object readSomething() {
		return readSomething(false);
	}

	public Object readSomething(boolean allowUnknown) {
		byte type = _source[_sourcePos];

		if (type == TYPE_NULL) {
			_sourcePos++;
			return null;
		}
		else if (type == TYPE_BYTE) {
			return readByte();
		}
		else if (type == TYPE_SHORT) {
			return readShort();
		}
		else if (type == TYPE_INT) {
			return readInt();
		}
		else if (type == TYPE_LONG) {
			return readLong();
		}
		else if (type == TYPE_STRING) {
			return readString();
		}
		else if (type == TYPE_BOOLEAN) {
			return readBoolean();
		}
		else if (type == TYPE_FLOAT) {
			return readFloat();
		}
		else if (type == TYPE_DOUBLE) {
			return readDouble();
		}
		else if (type == Type_BITVECTOR) {
			return readBitVector();
		}
		else if (allowUnknown) {
			_sourcePos++;
			return TYPE_UNKNOWN;
		}
		else {
			throw new IllegalArgumentException("Can't serialize type: " + type);
		}
	}

	protected int readRawInt() {
		int value = _source[_sourcePos++] & 0xFF;
		value <<= 8;
		value |= _source[_sourcePos++] & 0xFF;
		value <<= 8;
		value |= _source[_sourcePos++] & 0xFF;
		value <<= 8;
		value |= _source[_sourcePos++] & 0xFF;

		return value;
	}

	protected void checkType(byte type) {
		byte srcType = _source[_sourcePos++];

		if (srcType != type) {
			throw new RuntimeException("Types are divergent, expected: " + type + ", got: " + srcType);
		}
	}

	protected boolean checkNull() {
		if (_source[_sourcePos] == TYPE_NULL) {
			++_sourcePos;
			return true;
		}

		return false;
	}
}
