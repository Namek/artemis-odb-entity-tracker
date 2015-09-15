package org.jdesktop.swingx;

import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;

import net.namekdev.entity_tracker.utils.ReflectionUtils;

import org.jdesktop.swingx.JXTreeTable.TreeTableModelAdapter;
import org.jdesktop.swingx.treetable.TreeTableModel;

import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Method;
import com.artemis.utils.reflect.ReflectionException;

public class TreeTableModelAdapterExt extends TreeTableModelAdapter {
	protected final JTree tree;
	protected final Method TreeUi_getNode;


	public static interface ISimpleLeafsTreeModel {
		void setValueAt(Object value, Object node, Object parentNode, int nodeIndex, int column);
		Object getValueAt(Object node, Object parentNode, int nodeIndex, int column);
	}


	public TreeTableModelAdapterExt(JTree tree) {
		super(tree);
		this.tree = tree;

		Method method = null;
		try {
			Method[] methods = ClassReflection.getDeclaredMethods(VariableHeightLayoutCache.class);

			for (int i = 0; i < methods.length; ++i) {
				if (methods[i].getName().equals("getNode")) {
					method = methods[i];
					break;
				}
			}

			method.setAccessible(true);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		TreeUi_getNode = method;
	}

	@Override
	public void setValueAt(Object value, int row, int column) {
		TreePath nodePath = tree.getPathForRow(row);
		Object node = nodePath != null ? nodePath.getLastPathComponent() : null;
    	TreeTableModel tableModel = (TreeTableModel) tree.getModel();

    	if (tableModel instanceof ISimpleLeafsTreeModel) {
    		NodeAdditionalInfo nodeInfo = getNodeAdditionalInfo(row);

    		// finally, notify about the new value
    		ISimpleLeafsTreeModel simpleLeafTableModel = (ISimpleLeafsTreeModel) tableModel;
    		simpleLeafTableModel.setValueAt(value, node, nodeInfo.parentNode, nodeInfo.nodeIndex, column);
    	}
    	else {
    		tableModel.setValueAt(value, node, column);
    	}
	}

	@Override
	public Object getValueAt(int row, int column) {
		TreePath nodePath = tree.getPathForRow(row);
    	TreeTableModel tableModel = (TreeTableModel) tree.getModel();

    	if (tableModel instanceof ISimpleLeafsTreeModel) {
    		Object node = nodePath != null ? nodePath.getLastPathComponent() : null;
    		NodeAdditionalInfo nodeInfo = getNodeAdditionalInfo(row);

    		ISimpleLeafsTreeModel simpleLeafTableModel = (ISimpleLeafsTreeModel) tableModel;
    		return simpleLeafTableModel.getValueAt(node, nodeInfo.parentNode, nodeInfo.nodeIndex, column);
    	}

    	return super.getValueAt(row, column);
	}

	private NodeAdditionalInfo getNodeAdditionalInfo(int row) {
		BasicTreeUI treeUi = (BasicTreeUI) tree.getUI();
		VariableHeightLayoutCache treeState = (VariableHeightLayoutCache) ReflectionUtils.getHiddenFieldValue(BasicTreeUI.class, "treeState", treeUi);
		DefaultMutableTreeNode nodeState = null;

		NodeAdditionalInfo nodeInfo = new NodeAdditionalInfo();
		try {
			nodeState = (DefaultMutableTreeNode) TreeUi_getNode.invoke(treeState, row);
			DefaultMutableTreeNode parentNodeState = (DefaultMutableTreeNode) nodeState.getParent();
			nodeInfo.nodeIndex = parentNodeState.getIndex(nodeState);
			nodeInfo.parentNode = parentNodeState.getUserObject();
		}
		catch (ReflectionException e) {
			e.printStackTrace();
		}

		return nodeInfo;
	}

	private static class NodeAdditionalInfo {
		int nodeIndex = -1;
		Object parentNode = null;
	}
}
