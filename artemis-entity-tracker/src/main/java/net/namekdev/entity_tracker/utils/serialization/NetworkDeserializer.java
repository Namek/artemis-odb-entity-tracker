package net.namekdev.entity_tracker.utils.serialization;


import java.util.ArrayList;
import java.util.Vector;

import com.artemis.utils.BitVector;

public class NetworkDeserializer extends NetworkSerialization {
	private byte[] _source;
	private int _sourcePos, _sourceBeginPos;
	
	private ObjectModelsCollection _models = new ObjectModelsCollection() {
		private ArrayList<ObjectModelNode> models = new ArrayList<ObjectModelNode>();

		@Override
		public int size() {
			return models.size();
		}
		
		@Override
		public ObjectModelNode get(int index) {
			return models.get(index);
		}
		
		@Override
		public ObjectModelNode get(Class<?> type) {
			throw new RuntimeException("deserializer doesn't provide inspection");
		}
		
		@Override
		public void add(ObjectModelNode model) {
			models.add(model);
		}
	};


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

	public int beginArray(Type elementType) {
		checkType(Type.Array);
		checkType(elementType);
		return readRawInt();
	}

	public int beginArray() {
		return beginArray(Type.Unknown);
	}
	
	public Type readType() {
		return Type.values()[readRawByte()];
	}

	public byte readByte() {
		checkType(Type.Byte);
		return readRawByte();
	}

	public short readShort() {
		checkType(Type.Short);
		return readRawShort();
	}

	public int readInt() {
		checkType(Type.Int);
		return readRawInt();
	}

	public long readLong() {
		checkType(Type.Long);
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

		checkType(Type.String);
		int length = readRawInt();

		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; ++i) {
			sb.append((char) (_source[_sourcePos++] & 0xFF));
		}

		return sb.toString();
	}

	public boolean readBoolean() {
		checkType(Type.Boolean);
		return readRawBoolean();
	}

	public boolean readRawBoolean() {
		byte value = readRawByte();
		return value != 0;
	}

	public float readFloat() {
		checkType(Type.Float);
		return readRawFloat();
	}

	public float readRawFloat() {
		return Float.intBitsToFloat(readRawInt());
	}

	public double readDouble() {
		checkType(Type.Double);
		return readRawDouble();
	}

	public double readRawDouble() {
		return Double.longBitsToDouble(readRawLong());
	}

	public BitVector readBitVector() {
		if (checkNull()) {
			return null;
		}

		checkType(Type.BitVector);

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
		Type type = Type.values()[_source[_sourcePos]];

		if (type == Type.Null) {
			_sourcePos++;
			return null;
		}
		else if (type == Type.Byte) {
			return readByte();
		}
		else if (type == Type.Short) {
			return readShort();
		}
		else if (type == Type.Int) {
			return readInt();
		}
		else if (type == Type.Long) {
			return readLong();
		}
		else if (type == Type.String) {
			return readString();
		}
		else if (type == Type.Boolean) {
			return readBoolean();
		}
		else if (type == Type.Float) {
			return readFloat();
		}
		else if (type == Type.Double) {
			return readDouble();
		}
		else if (type == Type.BitVector) {
			return readBitVector();
		}
		else if (allowUnknown) {
			_sourcePos++;
			return Type.Unknown;
		}
		else {
			throw new IllegalArgumentException("Can't serialize type: " + type);
		}
	}

	public Object readRawByType(Type valueType) {
		switch (valueType) {
			case Byte: return readRawByte();
			case Short: return readRawShort();
			case Int: return readRawInt();
			case Long: return readRawLong();
			case String: return readString();
			case Boolean: return readRawBoolean();
			case Float: return readRawFloat();
			case Double: return readRawDouble();
			case BitVector: return readBitVector();

			default: throw new RuntimeException("type not supported" + valueType);
		}
	}

	public ObjectModelNode readDataDescription() {
		checkType(Type.Description);
		ObjectModelNode root = readRawDataDescription(null);

		return root;
	}

	private ObjectModelNode readRawDataDescription(ObjectModelNode parent) {
		int modelId = readRawInt();
		ObjectModelNode node = new ObjectModelNode(null, modelId, parent);
		this._models.add(node);
		node.name = readString();
		Type nodeType = readType();
		node.networkType = nodeType;

		if (nodeType == Type.Object) {
			int n = readRawInt();
			node.children = new Vector<>(n);

			for (int i = 0; i < n; ++i) {
				ObjectModelNode child = readRawDataDescription(node);
				node.children.addElement(child);
			}
		}
		else if (nodeType == Type.Array) {
			node.childType = readRawByte();
		}
		else if (nodeType == Type.Enum) {
			int enumModelId = readRawInt();
			ObjectModelNode enumModelRef = new ObjectModelNode(null, enumModelId, node);
			node.children = new Vector<>(1);
			node.children.addElement(enumModelRef);
			this._models.add(enumModelRef);
		}
		else if (nodeType == Type.EnumDescription) {
			// TODO TYPE_ENUM_DESCRIPTION
			throw new RuntimeException("TODO: TYPE_ENUM_DESCRIPTION");
		}
		else if (!isSimpleType(nodeType)) {
			throw new RuntimeException("unsupported type: " + nodeType);
		}

		return node;
	}

	public ValueTree readObject() {
		checkType(Type.MultipleDescriptions);
		int descrCount = readRawInt();
		
		ObjectModelNode rootModel = null;
		
		if (descrCount > 0) {
			rootModel = readDataDescription();
			_models.add(rootModel);
			
			ObjectModelNode previousModel = rootModel;
			for (int i = 0; i < descrCount-1; ++i) {
				ObjectModelNode model = readDataDescription();
				model.parent = previousModel; // TODO is that right???
	
				_models.add(model);
			}
		}
		else {
			checkType(Type.DescriptionRef);
			int modelId = readRawInt();
			for (int i = 0, n = _models.size(); i < n; ++i) {
				ObjectModelNode model = _models.get(i);

				if (model.id == modelId) {
					rootModel = model;
				}
			}
		}
		
		return readObject(rootModel);
	}

	public ValueTree readObject(ObjectModelNode model, boolean joinDataToModel) {
		checkType(Type.Object);
		ValueTree root = (ValueTree) readRawObject(model, null, joinDataToModel);

		return root;
	}
	
	public ValueTree readObject(ObjectModelNode model) {
		return readObject(model, false);
	}

	protected Object readRawObject(ObjectModelNode model, ValueTree parentTree, boolean joinModelToData) {
		final boolean isArray = model.isArray();

		if (!isArray && model.children != null) {
			checkType(Type.Object);
			int n = model.children.size();
			ValueTree tree = new ValueTree(n);
			tree.parent = parentTree;

			for (int i = 0; i < n; ++i) {
				ObjectModelNode child = model.children.get(i);
				tree.values[i] = readRawObject(child, tree, joinModelToData);
			}

			if (joinModelToData) {
				tree.model = model;
			}

			return tree;
		}
		else if (isSimpleType(model.networkType)) {
			Object value = readRawByType(model.networkType);

			return value;
		}
		else if (isArray) {
			Type arrayType = model.arrayType();
			int n = beginArray(arrayType);
			ValueTree tree = new ValueTree(n);
			tree.parent = parentTree;

			if (arrayType == Type.Object) {
				for (int i = 0; i < n; ++i) {
					ValueTree val = readObject();
					val.parent = tree;
					tree.values[i] = val;
				}
			}
			else if (isSimpleType(arrayType)) {
				for (int i = 0; i < n; ++i) {
					tree.values[i] = readRawByType(arrayType);
				}
			}
			else if (model.isEnum()) {
				for (int i = 0; i < n; ++i) {
					tree.values[i] = readRawInt();
				}
			}
			else {
				throw new RuntimeException("unsupported array type: " + arrayType);
			}

			if (joinModelToData) {
				tree.model = model;
			}

			return tree;
		}
		else {
			throw new RuntimeException("unsupported type: " + model.networkType);
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

	protected void checkType(Type type) {
		byte srcType = _source[_sourcePos++];

		if (srcType != type.ordinal()) {
			throw new RuntimeException("Types are divergent, expected: " + type + ", got: " + srcType);
		}
	}

	protected boolean checkNull() {
		if (_source[_sourcePos] == Type.Null.ordinal()) {
			++_sourcePos;
			return true;
		}

		return false;
	}
}
