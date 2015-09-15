package net.namekdev.entity_tracker.utils.serialization;

public class ValueTree {
	public Object[] values;
	public ObjectModelNode model;
	public ValueTree parent;

	public ValueTree() {
	}

	public ValueTree(int length) {
		values = new Object[length];
	}
}
