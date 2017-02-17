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

	public NetworkSerializer beginArray(Type elementType, int length) {
		addType(Type.Array);
		addType(elementType);
		addRawInt(length);

		return this;
	}

	public NetworkSerializer beginArray(int length) {
		return beginArray(Type.Unknown, length);
	}
	
	public NetworkSerializer addType(Type type) {
		return addRawByte((byte) type.ordinal());
	}

	public NetworkSerializer addByte(byte value) {
		addType(Type.Byte);
		_buffer[_pos++] = value;
		return this;
	}

	public NetworkSerializer addRawByte(byte value) {
		_buffer[_pos++] = value;
		return this;
	}

	public NetworkSerializer addShort(short value) {
		addType(Type.Short);
		addRawShort(value);
		return this;
	}

	protected void addRawShort(short value) {
		_buffer[_pos++] = (byte) ((value >> 8) & 0xFF);
		_buffer[_pos++] = (byte) (value & 0xFF);
	}

	public NetworkSerializer addInt(int value) {
		addType(Type.Int);
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
		addType(Type.Long);
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

		addType(Type.String);

		int n = value.length();
		addRawInt(n);

		for (int i = 0; i < n; ++i) {
			_buffer[_pos++] = (byte) (value.charAt(i) & 0xFF);
		}

		return this;
	}

	public NetworkSerializer addBoolean(boolean value) {
		addType(Type.Boolean);
		return addRawBoolean(value);
	}

	public NetworkSerializer addRawBoolean(boolean value) {
		return addRawByte((byte) (value ? 1 : 0));
	}

	public NetworkSerializer addFloat(float value) {
		addType(Type.Float);
		return addRawFloat(value);
	}

	public NetworkSerializer addRawFloat(float value) {
		addRawInt(Float.floatToIntBits(value));
		return this;
	}

	public NetworkSerializer addDouble(double value) {
		addType(Type.Double);
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

		addType(Type.BitVector);

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
			addType(Type.Null);
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
			addType(Type.Unknown);
		}
		else {
			throw new IllegalArgumentException("Can't serialize type: " + object.getClass());
		}

		return this;
	}

	public NetworkSerializer addRawByType(Type valueType, Object value) {
		switch (valueType) {
			case Byte: addRawByte((Byte) value); break;
			case Short: addRawShort((Short) value); break;
			case Int: addRawInt((Integer) value); break;
			case Long: addRawLong((Long) value); break;
			case String: addString((String) value); break;
			case Boolean: addRawBoolean((Boolean) value); break;
			case Float: addRawFloat((Float) value); break;
			case Double: addRawDouble((Double) value); break;
			case BitVector: addBitVector((BitVector) value); break;

			default: throw new RuntimeException("type not supported: " + valueType);
		}

		return this;
	}

	public NetworkSerializer addDataDescription(ObjectModelNode model) {
		addType(Type.Description);
		addRawDataDescription(model);

		return this;
	}

	private void addRawDataDescription(ObjectModelNode model) {
		addRawInt(model.id);
		addString(model.name);

		if (model.networkType == Type.Object || model.networkType == Type.Unknown) {
			addType(Type.Object);
			int n = model.children.size();
			addRawInt(n);

			for (int i = 0; i < n; ++i) {
				ObjectModelNode node = model.children.get(i);
				addRawDataDescription(node);
			}
		}
		else if (isSimpleType(model.networkType)) {
			addType(model.networkType);
		}
		else if (model.isEnum()) {
			addType(Type.Enum);
			addRawInt(model.enumModelId());
		}
		else if (model.networkType == Type.EnumValue) {
			addType(Type.EnumValue);
			addRawInt(model.childType);
			addString(model.name);
		}
		else if (model.networkType == Type.EnumDescription) {
			// TODO is it alright?!?!
			
			addType(Type.EnumDescription);
			
			int enumModelId = model.id;
			addRawInt(enumModelId);

			addRawInt(model.children.size());
			
			for (ObjectModelNode enumValueModel : model.children) {
				addRawInt(enumValueModel.id);
				
				// TODO FIXME the line below does not fit the types.
				addRawInt(enumValueModel.childType); // this is enum's value
				
				addString(enumValueModel.name);
			}
		}
		else if (model.isArray()) {
			Type arrayType = model.arrayType();
			addType(Type.Array);
			addType(arrayType == Type.Unknown ? Type.Object : arrayType);

			if (isSimpleType(arrayType)) {
				// do nothing
			}
			else if (arrayType == Type.Object || arrayType == Type.Unknown) {
//				int modelId = model.children.get(0).id;
//				addRawInt(modelId);
			}
			else if (arrayType == Type.Enum) {
				// Note: if we treat array of enums the same way as array of objects
				// then we do not have to write anything here.
				/*
				int enumModelId = model.enumModelId();
				addRawInt(enumModelId); // id of Enum
				ObjectModelNode enumModel = inspector.getModelById(enumModelId);
				addRawInt(enumModel.children.get(0).id); // id of EnumDescription
				*/
			}
			else {
				throw new RuntimeException("unsupported array type: " + arrayType);
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
		
		addType(Type.MultipleDescriptions);
		int diff = inspectionCount - previousInspectionCount;
		addRawInt(diff);
		
		if (diff > 0) {
			for (int i = previousInspectionCount+1; i < inspectionCount; ++i) {
				addDataDescription(
					inspector.getRegisteredModelByIndex(i)
				);
			}
			addDataDescription(
				inspector.getRegisteredModelByIndex(previousInspectionCount)
			);
			
			_typeCountOnLastCheck = inspectionCount;
		}
		else {
			addType(Type.DescriptionRef);
			addRawInt(model.id);
		}

		// Note: even though we have inspected as much as we could up to this point,
		// there could be added more types because of Object Arrays.
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
		addType(Type.Object);
		addRawObject(model, object);

		return this;
	}

	protected void addRawObject(ObjectModelNode model, Object object) {
		final boolean isArray = model.isArray();

		if (tryAddNullable(object)) {
			// well, null is added here.
		}
		else if (!isArray && (model.networkType == Type.Object || model.networkType == Type.Unknown)) {
			addType(Type.Object);
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
		else if (model.isEnum()) {
			addType(Type.Enum);
			
			int enumVal = ((Enum<Type>) object).ordinal();
			addRawInt(enumVal);
		}
		else if (isArray) {
			// TODO probably this case will be moved to `addArray()`
			
			Object[] array = (Object[]) object;
			int n = array.length;
			Type arrayType = model.arrayType();
			beginArray(arrayType, n);

			if (arrayType == Type.Unknown || arrayType == Type.Object) {
				for (int i = 0; i < n; ++i) {
					addObject(array[i]);
				}
			}
			else if (isSimpleType(arrayType)) {
				for (int i = 0; i < n; ++i) {
					addRawByType(arrayType, array[i]);
				}
			}
			else if (arrayType == Type.Enum) {
				for (int i = 0; i < n; ++i) {
					int enumVal = ((Enum<Type>) array[i]).ordinal();
					addRawInt(enumVal);
				}
			}
			else {
				throw new RuntimeException("unsupported array type: " + arrayType);
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
