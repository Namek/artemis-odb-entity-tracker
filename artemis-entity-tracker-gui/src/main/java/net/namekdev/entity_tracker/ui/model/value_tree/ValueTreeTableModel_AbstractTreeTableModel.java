package net.namekdev.entity_tracker.ui.model.value_tree;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode;
import net.namekdev.entity_tracker.utils.serialization.ValueTree;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;

public class ValueTreeTableModel_AbstractTreeTableModel extends AbstractTreeTableModel  {
	public ObjectModelNode modelRoot;
	public ValueTree tree;

	public ValueTreeTableModel_AbstractTreeTableModel(ObjectModelNode model, ValueTree tree) {
		super.root = this.modelRoot = model;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public Object getValueAt(Object node, int column) {
		if (node instanceof ValueTreeNode) {
			ValueTreeNode treeNode = (ValueTreeNode) node;
			ObjectModelNode model = treeNode.model;

			if (column == 0) {
				return model.name;
			}

			if (model.isArray) {
				// TODO
			}
			else if (model.children != null) {
				// TODO TYPE_TREE
			}
			else {
				return tree.values[0];
			}
		}
		return null;
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (parent instanceof ValueTreeNode) {
			ValueTreeNode node = (ValueTreeNode) parent;
			return node.tree.values[index];
		}

		return null;
	}

	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof ValueTree) {
			ValueTreeNode node = (ValueTreeNode) parent;
			return node.tree.values.length;
		}

		return 0;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof ValueTree) {
			ValueTreeNode node = (ValueTreeNode) parent;

			for (int i = 0; i < tree.values.length; ++i) {
				if (node.tree.values[i] == child) {
					return i;
				}
			}
		}

		return -1;
	}

}
