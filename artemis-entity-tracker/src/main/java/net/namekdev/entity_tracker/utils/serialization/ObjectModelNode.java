package net.namekdev.entity_tracker.utils.serialization;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;

import java.util.Vector;

import net.namekdev.entity_tracker.utils.ReflectionUtils;

/**
 * Describes a structure of class or class field.
 * Allows to hierarchically get or set a value.
 * 
 * <p>It does not describe a structure of a specific object hierarchy.
 * Array fields will be just described as a definition,
 * independently of the array's content.</p>
 */
public final class ObjectModelNode {
	private final ObjectModelsCollection _models;
	
	public int id = -1;
	public String name;

	public Vector<ObjectModelNode> children;

	
	// when it's null it defines a class, otherwise it's field
	public ObjectModelNode parent;

	public Type networkType;
	public short childType;
	

	public ObjectModelNode(ObjectModelsCollection models, int id, ObjectModelNode parent) {
		this._models = models;
		this.id = id;
		this.parent = parent;
	}

	public boolean isLeaf() {
		return !isArray() && networkType != Type.Object;
	}
	
	public boolean isArray() {
		return networkType == Type.Array;
	}
	
	public boolean isEnum() {
		return networkType == Type.Enum;
	}
	
	public Type arrayType() {
		if (!isArray()) {
			throw new RuntimeException("this is not array!");
		}
		
		return Type.values()[childType];
	}
	
	public int enumModelId() {
		if (!isEnum()) {
			throw new RuntimeException("this is not enum field!");
		}
		
		return children.elementAt(0).id;
	}

	public void setValue(Object targetObj, int[] treePath, Object value) {
		assert treePath != null && treePath.length >= 1;
		
		Class<?> valueType = value.getClass();
		assert value == null || isSimpleType(determineType(valueType)) || valueType.isEnum();

		int pathIndex = 0;
		ObjectModelNode node = this;

		while (pathIndex < treePath.length) {
			int index = treePath[pathIndex];

			if (!node.isArray() && node.children != null) {
				node = node.children.get(index);
				String fieldName = node.name;

				if (node.isLeaf()) {
					ReflectionUtils.setHiddenFieldValue(targetObj.getClass(), fieldName, targetObj, value);
				}
				else {
					targetObj = ReflectionUtils.getHiddenFieldValue(targetObj.getClass(), fieldName, targetObj);
				}
			}
			else if (isSimpleType(node.networkType) || node.isEnum()) {
				node = node.children.get(index);
				assert node.isLeaf();

				String fieldName = node.name;
				ReflectionUtils.setHiddenFieldValue(targetObj.getClass(), fieldName, targetObj, value);
			}
			else if (node.isArray()) {
				Object[] array = (Object[]) targetObj;
				Type arrayType = node.arrayType();
				
				if (arrayType == Type.Unknown || arrayType == Type.Object) {
					assert(pathIndex < treePath.length-1);
					assert(node.children == null);
					++pathIndex;

					Object arrayEl = array[pathIndex];
					ObjectModelNode arrayElModel = _models.get(arrayEl.getClass());
					
					targetObj = arrayEl;
					node = arrayElModel;
				}
				else if (isSimpleType(arrayType)) {
					assert(pathIndex == treePath.length-1);
					++pathIndex;
					
					array[pathIndex] = value;
				}
				else {
					throw new RuntimeException("unsupported operation");
				}

//				if (node.arrayType == Type.Object || node.arrayType == TYPE_UNKNOWN) {
//					assert pathIndex < treePath.length;
//					targetObj = array[index];
//					index = treePath[++pathIndex];
//					node = node.children.get(index);
//					String fieldName = node.name;
//					targetObj = ReflectionUtils.getHiddenFieldValue(targetObj.getClass(), fieldName, targetObj);
//				}
//				else {
//					assert pathIndex+1 < treePath.length;
//					array[pathIndex] = value;
//				}
			}
			else {
				throw new RuntimeException("oops, logical error");
			}

			pathIndex += 1;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ObjectModelNode)) {
			return false;
		}

		ObjectModelNode model = (ObjectModelNode) obj;
		
		if (id != model.id || isArray() != model.isArray()) {
			return false;
		}

		if (name == null && model.name != null || name != null && model.name == null) {
			return false;
		}

		if (name != null && !name.equals(model.name)) {
			return false;
		}

		/*
		if (networkType != model.networkType || arrayType != model.arrayType) {
			return false;
		}

		if (children == null && model.children != null || children != null && model.children == null) {
			return false;
		}

		if (children != null) {
			if (children.size() != model.children.size()) {
				return false;
			}

			for (int i = 0, n = children.size(); i < n; ++i) {
				// TODO handle cyclic checks here!
//				if (children.get(i) != model.children.get(i)) {
//				if (!children.get(i).equals(model.children.get(i))) {
				if (children.get(i).id != model.children.get(i).id) {
					return false;
				}
			}
		}*/

		return true;
	}

	public ObjectModelNode copyFrom(ObjectModelNode other) {
		this.id = other.id;
		this.name = other.name;
		this.networkType = other.networkType;
		this.childType = other.childType;
		this.children = new Vector<ObjectModelNode>(other.children);
		return this;
	}
}
