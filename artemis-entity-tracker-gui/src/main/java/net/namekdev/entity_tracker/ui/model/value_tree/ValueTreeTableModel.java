package net.namekdev.entity_tracker.ui.model.value_tree;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode;
import net.namekdev.entity_tracker.utils.serialization.ValueTree;

import org.jdesktop.swingx.treetable.TreeTableModel;

public class ValueTreeTableModel implements TreeTableModel {
	ObjectModelNode model;


	public ValueTreeTableModel(ObjectModelNode model) {
		this.model = model;
	}

	@Override
	public Object getRoot() {
		return model;
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (parent instanceof ObjectModelNode) {
			ObjectModelNode model = (ObjectModelNode) parent;

			if (model.isArray) {
				return ((ValueTree)model.data).values[index];
			}

			return model.children.get(index);
		}
		else if (parent instanceof ValueTree) {
			return ((ValueTree) parent).values[index];
		}

		return null;
	}

	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof ObjectModelNode) {
			ObjectModelNode model = (ObjectModelNode) parent;
			if (model.children != null) {
				if (model.isArray) {
					return ((ValueTree)model.data).values.length;
				}
				else {
					return model.children.size();
				}
			}
		}
		else if (parent instanceof ValueTree) {
			return ((ValueTree) parent).values.length;
		}

		return 0;
	}

	@Override
	public boolean isLeaf(Object node) {
		if (node instanceof ObjectModelNode) {
			ObjectModelNode model = (ObjectModelNode) node;
			return model.isLeaf();
		}
		else if (node instanceof ValueTree) {
			return false;
		}

		return true;
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		ObjectModelNode model = (ObjectModelNode) parent;

		for (int i = 0, n = model.children.size(); i < n; ++i) {
			if (model.children.get(i) == child) {
				return i;
			}
		}

		return -1;
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public String getColumnName(int column) {
		return column == 0 ? "field" : "value";
	}

	@Override
	public int getHierarchicalColumn() {
		return 0;
	}

	@Override
	public Object getValueAt(Object node, int column) {
		if (node instanceof ObjectModelNode) {
			ObjectModelNode model = (ObjectModelNode) node;

			if (column == 0) {
				return model.name;
			}
			else {
				if (model.isLeaf()) {
					return model.data;
				}
			}
		}
		else if (node instanceof ValueTree) {
			// TODO should not come here
//			assert false;
			return null;
		}
		else if (column == 1) {
			return node;
		}

		return null;
	}

	@Override
	public boolean isCellEditable(Object node, int column) {
		if (column == 0) {
			return false;
		}

		if (node instanceof ObjectModelNode) {
			ObjectModelNode model = (ObjectModelNode) node;
			return model.isLeaf();
		}
		else if (node instanceof ValueTree) {
			return false;
		}

		return true;
	}

	@Override
	public void setValueAt(Object value, Object node, int column) {
		if (node instanceof ObjectModelNode) {
			int a = 5;
		}
		else if (node instanceof ValueTree) {
			assert false;
		}
		else {
			assert false;
		}
	}

}
