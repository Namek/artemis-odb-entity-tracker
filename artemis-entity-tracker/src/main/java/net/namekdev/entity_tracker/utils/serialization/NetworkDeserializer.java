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
		return readRawBoolean();
	}

	public boolean readRawBoolean() {
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

		checkType(TYPE_BITVECTOR);

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
		else if (type == TYPE_BITVECTOR) {
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

	public Object readRawByType(byte valueType) {
		switch (valueType) {
			case TYPE_BYTE: return readRawByte();
			case TYPE_SHORT: return readRawShort();
			case TYPE_INT: return readRawInt();
			case TYPE_LONG: return readRawLong();
			case TYPE_STRING: return readString();
			case TYPE_BOOLEAN: return readRawBoolean();
			case TYPE_FLOAT: return readRawFloat();
			case TYPE_DOUBLE: return readRawDouble();
			case TYPE_BITVECTOR: return readBitVector();

			default: throw new RuntimeException("type not supported" + valueType);
		}
	}

	public ObjectModelNode readObjectDescription() {
		checkType(TYPE_DESCRIPTION);
		ObjectModelNode root = readRawObjectDescription(null);

		return root;
	}

	private ObjectModelNode readRawObjectDescription(ObjectModelNode parent) {
		int modelId = readRawInt();
		ObjectModelNode node = new ObjectModelNode(null, modelId, parent);
		this._models.add(node);
		node.name = readString();
		byte nodeType = readRawByte();
		node.networkType = nodeType;

		if (nodeType == TYPE_OBJECT) {
			int n = readRawInt();
			node.children = new Vector<>(n);

			for (int i = 0; i < n; ++i) {
				ObjectModelNode child = readRawObjectDescription(node);
				node.children.addElement(child);
			}
		}
		else if (nodeType == TYPE_ARRAY) {
			node.arrayType = readRawByte();
		}
		else if (!isSimpleType(nodeType)) {
			throw new RuntimeException("unsupported type: " + nodeType);
		}

		return node;
	}

	public ValueTree readObject() {
		checkType(TYPE_MULTIPLE_DESCRIPTIONS);
		int descrCount = readRawInt();
		
		ObjectModelNode rootModel = null;
		
		if (descrCount > 0) {
			rootModel = readObjectDescription();
			_models.add(rootModel);
			
			ObjectModelNode previousModel = rootModel;
			for (int i = 0; i < descrCount-1; ++i) {
				ObjectModelNode model = readObjectDescription();
				model.parent = previousModel; // TODO is that right???
	
				_models.add(model);
			}
		}
		else {
			checkType(TYPE_DESCRIPTION_REF);
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
		checkType(TYPE_OBJECT);
		ValueTree root = (ValueTree) readRawObject(model, null, joinDataToModel);

		return root;
	}
	
	public ValueTree readObject(ObjectModelNode model) {
		return readObject(model, false);
	}

	protected Object readRawObject(ObjectModelNode model, ValueTree parentTree, boolean joinModelToData) {
		final boolean isArray = model.isArray();

		if (!isArray && model.children != null) {
			checkType(TYPE_OBJECT);
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
			int n = beginArray(model.arrayType);
			ValueTree tree = new ValueTree(n);
			tree.parent = parentTree;

			if (model.arrayType == TYPE_UNKNOWN) {
				for (int i = 0; i < n; ++i) {
					ValueTree val = readObject();
					val.parent = tree;
					tree.values[i] = val;
				}
			}
			else if (isSimpleType(model.arrayType)) {
				for (int i = 0; i < n; ++i) {
					tree.values[i] = readRawByType(model.arrayType);
				}
			}
			else {
				throw new RuntimeException("unsupported array type: " + model.arrayType);
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
