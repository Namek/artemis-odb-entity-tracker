package net.namekdev.entity_tracker.utils.serialization;

import java.util.BitSet;

import net.namekdev.entity_tracker.utils.ReflectionUtils;

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

	public NetworkSerializer addLong(long value) {
		_buffer[_pos++] = TYPE_LONG;
		addRawLong(value);
		return this;
	}

	protected void addRawLong(long value) {
		_buffer[_pos++] = (byte) ((value >> 56) & 0xFF);
		_buffer[_pos++] = (byte) ((value >> 48) & 0xFF);
		_buffer[_pos++] = (byte) ((value >> 40) & 0xFF);
		_buffer[_pos++] = (byte) ((value >> 32) & 0xFF);
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
		return addRawBoolean(value);
	}

	public NetworkSerializer addRawBoolean(boolean value) {
		return addRawByte((byte) (value ? 1 : 0));
	}

	public NetworkSerializer addFloat(float value) {
		_buffer[_pos++] = TYPE_FLOAT;
		return addRawFloat(value);
	}

	public NetworkSerializer addRawFloat(float value) {
		addRawInt(Float.floatToIntBits(value));
		return this;
	}

	public NetworkSerializer addDouble(double value) {
		_buffer[_pos++] = TYPE_DOUBLE;
		return addRawDouble(value);
	}

	public NetworkSerializer addRawDouble(double value) {
		addRawLong(Double.doubleToLongBits(value));
		return this;
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

	public NetworkSerializer addSomething(Object object) {
		return addSomething(object, false);
	}

	public NetworkSerializer addSomething(Object object, boolean allowUnknown) {
		if (object == null) {
			tryAddNullable(object);
		}
		else if (object instanceof Byte) {
			addByte(((Byte) object).byteValue());
		}
		else if (object instanceof Short) {
			addShort(((Short) object).shortValue());
		}
		else if (object instanceof Integer) {
			addInt(((Integer) object).intValue());
		}
		else if (object instanceof Long) {
			addLong(((Long) object).longValue());
		}
		else if (object instanceof String) {
			addString(((String) object));
		}
		else if (object instanceof Boolean) {
			addBoolean(((Boolean) object).booleanValue());
		}
		else if (object instanceof Float) {
			addFloat(((Float) object).floatValue());
		}
		else if (object instanceof Double) {
			addDouble(((Double) object).doubleValue());
		}
		else if (object instanceof BitSet) {
			addBitSet(((BitSet) object));
		}
		else if (allowUnknown) {
			_buffer[_pos++] = TYPE_UNKNOWN;
		}
		else {
			throw new IllegalArgumentException("Can't serialize type: " + object.getClass());
		}

		return this;
	}

	public NetworkSerializer addRawByType(byte valueType, Object value) {
		switch (valueType) {
			case TYPE_BYTE: addRawByte((Byte) value); break;
			case TYPE_SHORT: addRawShort((Short) value); break;
			case TYPE_INT: addRawInt((Integer) value); break;
			case TYPE_LONG: addRawLong((Long) value); break;
			case TYPE_STRING: addString((String) value); break;
			case TYPE_BOOLEAN: addRawBoolean((Boolean) value); break;
			case TYPE_FLOAT: addRawFloat((Float) value); break;
			case TYPE_DOUBLE: addRawDouble((Double) value); break;
			case TYPE_BITSET: addBitSet((BitSet) value); break;

			default: throw new RuntimeException("type not supported: " + valueType);
		}

		return this;
	}

	public NetworkSerializer addObjectDescription(ObjectModelNode model, int modelId) {
		_buffer[_pos++] = TYPE_TREE_DESCR;
		addRawInt(modelId);
		addRawObjectDescription(model);

		return this;
	}

	protected void addRawObjectDescription(ObjectModelNode model) {
		addString(model.name);

		if (model.children != null) {
			if (model.isArray) {
				addRawByte(TYPE_ARRAY);
				addRawByte(model.arrayType);

				if (model.arrayType != TYPE_TREE && !isSimpleType(model.arrayType)) {
					throw new RuntimeException("unsupported array type: " + model.arrayType);
				}
			}
			else {
				addRawByte(TYPE_TREE);
			}

			if (model.networkType == TYPE_TREE || model.arrayType == TYPE_TREE) {
				int n = model.children.size();
				addRawInt(n);

				for (int i = 0; i < n; ++i) {
					ObjectModelNode node = model.children.get(i);
					addRawObjectDescription(node);
				}
			}
		}
		else if (isSimpleType(model.networkType)) {
			addRawByte(model.networkType);
		}
		else {
			throw new RuntimeException("unsupported type: " + model.networkType);
		}
	}

	public NetworkSerializer addObject(ObjectModelNode model, Object object) {
		addRawByte(TYPE_TREE);
		addRawObject(model, object);

		return this;
	}

	protected void addRawObject(ObjectModelNode model, Object object) {
		if (!model.isArray && model.children != null) {
			addRawByte(TYPE_TREE);
			int n = model.children.size();

			for (int i = 0; i < n; ++i) {
				ObjectModelNode child = model.children.get(i);
				Object childObject = ReflectionUtils.getHiddenFieldValue(object.getClass(), child.name, object);

				addRawObject(child, childObject);
			}
		}
		else if (isSimpleType(model.networkType)) {
			addRawByType(model.networkType, object);
		}
		else if (model.isArray) {
			Object[] array = (Object[]) object;

			int n = array.length;
			beginArray(model.arrayType, n);

			if (model.arrayType == TYPE_TREE) {
				int fieldCount = model.children.size();

				for (int i = 0; i < n; ++i) {
					for (int j = 0; j < fieldCount; ++j) {
						ObjectModelNode field = model.children.get(j);
						Object fieldValue = ReflectionUtils.getHiddenFieldValue(array[i].getClass(), field.name, array[i]);

						addRawObject(field, fieldValue);
					}
				}
			}
			else if (isSimpleType(model.arrayType)) {
				for (int i = 0; i < n; ++i) {
					addRawByType(model.arrayType, array[i]);
				}
			}
			else {
				throw new RuntimeException("unsupported array type: " + model.arrayType);
			}
		}
		else {
			throw new RuntimeException("unsupported type: " + model.networkType);
		}
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
