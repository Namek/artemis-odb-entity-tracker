package net.namekdev.entity_tracker.utils.serialization;

import java.util.Vector;

public class ObjectModelNode {
	public int rootId = -1;
	public String name;
	public boolean isArray;
	public Vector<ObjectModelNode> children;
	public byte networkType, arrayType;

	public boolean isLeaf() {
		return children == null;
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
