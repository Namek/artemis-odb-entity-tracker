package net.namekdev.entity_tracker.utils.serialization;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;

import java.util.Vector;

import net.namekdev.entity_tracker.utils.ReflectionUtils;

public class ObjectModelNode {
	public int rootId = -1;
	public String name;
	public boolean isArray;
	public Vector<ObjectModelNode> children;
	public byte networkType, arrayType;


	public boolean isLeaf() {
		return !isArray && children == null && networkType != TYPE_TREE;
	}

	public void setValue(Object targetObj, int[] treePath, Object value) {
		assert treePath != null && treePath.length >= 1;
		assert value == null || isSimpleType(determineSimpleType(value.getClass()));

		int pathIndex = 0;
		ObjectModelNode node = this;

		while (pathIndex < treePath.length) {
			int index = treePath[pathIndex];

			if (!node.isArray && node.children != null) {
				node = node.children.get(index);
				String fieldName = node.name;

				if (node.isLeaf()) {
					ReflectionUtils.setHiddenFieldValue(targetObj.getClass(), fieldName, targetObj, value);
				}
				else {
					targetObj = ReflectionUtils.getHiddenFieldValue(targetObj.getClass(), fieldName, targetObj);
				}
			}
			else if (isSimpleType(node.networkType)) {
				node = node.children.get(index);
				assert node.isLeaf();

				String fieldName = node.name;
				ReflectionUtils.setHiddenFieldValue(targetObj.getClass(), fieldName, targetObj, value);
			}
			else if (node.isArray) {
				Object[] array = (Object[]) targetObj;

				if (node.arrayType == TYPE_TREE) {
					assert pathIndex < treePath.length;
					targetObj = array[index];
					index = treePath[++pathIndex];
					node = node.children.get(index);
					String fieldName = node.name;
					targetObj = ReflectionUtils.getHiddenFieldValue(targetObj.getClass(), fieldName, targetObj);
				}
				else {
					assert pathIndex+1 < treePath.length;
					array[pathIndex] = value;
				}
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

		if (rootId != model.rootId || isArray != model.isArray) {
			return false;
		}

		if (name == null && model.name != null || name != null && model.name == null) {
			return false;
		}

		if (name != null && !name.equals(model.name)) {
			return false;
		}

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
				if (!children.get(i).equals(model.children.get(i))) {
					return false;
				}
			}
		}

		return true;
	}
}
