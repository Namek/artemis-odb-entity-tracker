package net.namekdev.entity_tracker.ui.model.value_tree;

import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode;
import net.namekdev.entity_tracker.utils.serialization.ValueTree;

public class ValueTreeNode {
	public ValueTree tree;
	public ObjectModelNode model;

	public ValueTreeNode(ValueTree tree, ObjectModelNode model) {
		this.model = model;
		this.tree = tree;
		join(model, tree);
	}

	private void join(ObjectModelNode model, ValueTree tree) {

	}
}
