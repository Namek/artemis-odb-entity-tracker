package net.namekdev.entity_tracker.utils.serialization;

import com.artemis.utils.BitVector;

import net.namekdev.entity_tracker.utils.ReflectionUtils;

/**
 * This network serializer may seem indeterministic however it is not the case.
 * When given object is being serialized through {@code addObject()} call or any
 * descendant field is of custom type (class) then the definitions are implicitily 
 * added to serialization buffer.
 */
public class NetworkSerializer extends NetworkSerialization {
	private byte[] _ourBuffer;
	private byte[] _buffer;
	private int _pos;

	private final SerializationResult _serializeResult = new SerializationResult();
	
	public final ObjectTypeInspector inspector;
	private int _typeCountOnLastCheck = 0;


	public NetworkSerializer() {
		this(new ObjectTypeInspector());
	}
	
	public NetworkSerializer(ObjectTypeInspector inspector) {
		_ourBuffer = _buffer = new byte[10240];
		this.inspector = inspector;
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

	public int getNewInspectedTypeCountToBeManuallySent() {
		int count = inspector.getRegisteredModelsCount();
		int diff = count - _typeCountOnLastCheck;
		_typeCountOnLastCheck = count;
		return diff;
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

	public NetworkSerializer addBitVector(BitVector bitVector) {
		if (tryAddNullable(bitVector)) {
			return this;
		}

		_buffer[_pos++] = TYPE_BITVECTOR;

		int bitsCount = bitVector.length();
		addRawShort((short) bitsCount);

		int i = 0, value;
		while (i < bitsCount) {
			value = 0;
			for (int j = 0; j < Integer.SIZE && j < bitsCount; ++j, ++i) {
				boolean bit = bitVector.get(i);

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
		else if (object instanceof BitVector) {
			addBitVector((BitVector) object);
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
			case TYPE_BITVECTOR: addBitVector((BitVector) value); break;

			default: throw new RuntimeException("type not supported: " + valueType);
		}

		return this;
	}

	public NetworkSerializer addObjectDescription(ObjectModelNode model) {
		_buffer[_pos++] = TYPE_DESCRIPTION;
		addRawObjectDescription(model);

		return this;
	}

	private void addRawObjectDescription(ObjectModelNode model) {
		addRawInt(model.id);
		addString(model.name);

		if (model.children != null) {
			addRawByte(TYPE_OBJECT);

			if (model.networkType == TYPE_OBJECT || model.arrayType == TYPE_UNKNOWN) {
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
		else if (model.isArray()) {
			addRawByte(TYPE_ARRAY);
			addRawByte(model.arrayType);

			if (model.arrayType != TYPE_OBJECT && !isSimpleType(model.arrayType)) {
				if (model.arrayType == TYPE_UNKNOWN) {
					// TODO ? model id
				}
				else {
					throw new RuntimeException("unsupported array type: " + model.arrayType);
				}
			}
		}
		else {
			throw new RuntimeException("unsupported type: " + model.networkType);
		}
	}

	/**
	 * Inspects object, adds it's definition or cached ID if it was already inspected.
	 * Then serializes the object.
	 * 
	 * <p>It is not the same as manual subsequent calls
	 *  of {@code addObjectDescription()} and {@code addObject()}
	 *  because of the inspection cache.</p>
	 */
	public NetworkSerializer addObject(Object obj) {
		assert(!obj.getClass().isArray());
		int previousInspectionCount = inspector.getRegisteredModelsCount();
		ObjectModelNode model = inspector.inspect(obj.getClass());
		int inspectionCount = inspector.getRegisteredModelsCount();
		
		addRawByte(TYPE_MULTIPLE_DESCRIPTIONS);
		int diff = inspectionCount - previousInspectionCount;
		addRawInt(diff);
		
		if (diff > 0) {
			for (int i = previousInspectionCount; i < inspectionCount; ++i) {
				addObjectDescription(
					inspector.getRegisteredModelByIndex(i)
				);
			}
			_typeCountOnLastCheck = inspectionCount;
		}
		else {
			addRawByte(TYPE_DESCRIPTION_REF);
			addRawInt(model.id);
		}

		addObject(model, obj);
		
		return this;
	}
	
	public NetworkSerializer addArray(Object[] array) {
		// TODO inspect every element
		throw new RuntimeException("not implemented");
//		return this;
	}
	
	public NetworkSerializer addArrayOfSameType(Object[] array) {
		// TODO inspect first element
		throw new RuntimeException("not implemented");
//		return this;
	}
	
	public NetworkSerializer addArrayOfSameType(Object[] array, ObjectModelNode model) {
		// TODO
		// simpleType / TYPE_TREE ?
		// empty array?
		throw new RuntimeException("not implemented");
//		return this;
	}

	public NetworkSerializer addObject(ObjectModelNode model, Object object) {
		addRawByte(TYPE_OBJECT);
		addRawObject(model, object);

		return this;
	}

	protected void addRawObject(ObjectModelNode model, Object object) {
		final boolean isArray = model.isArray();

		if (!isArray && model.children != null) {
			addRawByte(TYPE_OBJECT);
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
		else if (isArray) {
			Object[] array = (Object[]) object;

			int n = array.length;
			beginArray(model.arrayType, n);

			if (model.arrayType == TYPE_UNKNOWN) {
				for (int i = 0; i < n; ++i) {
					addObject(array[i]);
				}
			}
			/*if (model.arrayType == TYPE_OBJECT || model.arrayType == TYPE_UNKNOWN) {
				int fieldCount = model.children.size();

				for (int i = 0; i < n; ++i) {
					for (int j = 0; j < fieldCount; ++j) {
						ObjectModelNode field = model.children.get(j);
						Object fieldValue = ReflectionUtils.getHiddenFieldValue(array[i].getClass(), field.name, array[i]);

						addRawObject(field, fieldValue);
					}
				}
			}*/
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

	public SerializationResult getResult() {
//		_buffer[_pos++] = PACKET_END;

		return _buffer == _ourBuffer
			? _serializeResult.setup(_buffer, _pos)
			: new SerializationResult(_buffer, _pos);
	}



	public static class SerializationResult {
		public byte[] buffer;
		public int size;

		private SerializationResult() {
		}

		private SerializationResult(byte[] buffer, int size) {
			this.buffer = buffer;
			this.size = size;
		}

		private SerializationResult setup(byte[] buffer, int size) {
			this.buffer = buffer;
			this.size = size;
			return this;
		}
	}
}
