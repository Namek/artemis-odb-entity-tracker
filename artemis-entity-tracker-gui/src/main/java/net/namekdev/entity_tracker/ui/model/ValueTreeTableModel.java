package net.namekdev.entity_tracker.ui.model;

import java.util.ArrayList;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode;
import net.namekdev.entity_tracker.utils.serialization.ValueTree;

import org.jdesktop.swingx.DynamicJXTreeTable.ICellClassGetter;
import org.jdesktop.swingx.TreeTableModelAdapterExt.ISimpleLeafsTreeModel;
import org.jdesktop.swingx.treetable.TreeTableModel;

public class ValueTreeTableModel implements TreeTableModel, ICellClassGetter, ISimpleLeafsTreeModel {
	private ValueTree root;

	private static final int COL_OMG = 0;
	private static final int COL_KEY = 1;
	private static final int COL_VAL = 2;


	public ValueTreeTableModel(ValueTree tree) {
		assert tree.model != null;
		root = tree;
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public Object getChild(Object parent, int index) {
		ValueTree tree = (ValueTree) parent;
		return tree.values[index];
	}

	@Override
	public int getChildCount(Object parent) {
		ValueTree tree = (ValueTree) parent;
		ObjectModelNode model = tree.model;

		if (model == null) {
			if (tree.parent == null) {
				return 0;
			}

			return tree.parent.model.children.size();
		}

		if (model.isArray) {
			return tree.values.length;
		}

		return model.children == null ? 0 : model.children.size();
	}

	@Override
	public Object getValueAt(Object node, int column) {
		if (node == null || column == COL_OMG) {
			return null;
		}

		if (!(node instanceof ValueTree)) {
			return column == COL_VAL ? node : node.getClass().getSimpleName();
		}

		ValueTree tree = (ValueTree) node;
		ObjectModelNode model = tree.model;

		if (model != null) {
			return column == COL_KEY ? model.name : null;
		}
		else {
			assert tree.parent != null;

			if (tree.parent.model.isArray) {
				return null;
			}
			else {
				return tree.values[0];
			}
		}
	}

	@Override
	public Object getValueAt(Object node, Object parentNode, int nodeIndex, int column) {
		if (column == COL_OMG) {
			return null;
		}

		if (!(node instanceof ValueTree)) {
			if (parentNode instanceof ValueTree) {
				ValueTree parentTree = (ValueTree) parentNode;

				if (column == COL_VAL) {
					return parentTree.values[nodeIndex];
				}
				else if (node != null) {
					String name = parentTree.model.children.get(nodeIndex).name;
					String str = name + " (" + node.getClass().getSimpleName() + ")";

					return str;
				}
			}
		}

		return getValueAt(node, column);
	}

	@Override
	public void setValueAt(Object value, Object node, Object parentNode, int nodeIndex, int column) {
		assert column == COL_VAL;
		assert !(node instanceof ValueTree);

		if (parentNode instanceof ValueTree) {
			ValueTree parentTree = (ValueTree) parentNode;

			// TODO we shouldn't do this conversion. cellEditor should be prepared for it!
			if (value instanceof Long) {
				if (node instanceof Float) {
					value = (float)(long) value;
				}
			}

			parentTree.values[nodeIndex] = value;
		}
	}

	@Override
	public boolean isCellEditable(Object node, int column) {
		return column == COL_VAL && getCellClass(node, column) != null;
	}

	@Override
	public void setValueAt(Object value, Object node, int column) {
//		assert false;
	}

	@Override
	public Class<?> getCellClass(Object node, int column) {
		if (node != null && !(node instanceof ValueTree) && column == COL_VAL) {
			return node.getClass();
		}

		return null;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		// should call getCellType() but can't `assert false` here
		// because of JXTreeTable internal hackery
		return null;
	}

	@Override
	public boolean isLeaf(Object node) {
		if (node == null || !(node instanceof ValueTree)) {
			return true;
		}

		ValueTree tree = (ValueTree) node;
		return tree.model == null
			&& (tree.parent == null || tree.parent.model.isLeaf());
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
	public int getColumnCount() {
		return 3;
	}

	@Override
	public String getColumnName(int column) {
		if (column == COL_KEY)
			return "field";
		if (column == COL_VAL)
			return "value";

		return "omg";
	}

	@Override
	public int getHierarchicalColumn() {
		return 0;
	}

	@Override
	public void addTreeModelListener(TreeModelListener listener) {
	}

	@Override
	public void removeTreeModelListener(TreeModelListener listener) {
	}
}
