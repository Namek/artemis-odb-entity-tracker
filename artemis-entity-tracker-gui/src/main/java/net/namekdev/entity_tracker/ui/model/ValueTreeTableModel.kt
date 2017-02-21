package net.namekdev.entity_tracker.ui.model

import javax.swing.event.TreeModelListener
import javax.swing.tree.TreePath

import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ValueTree

import org.jdesktop.swingx.DynamicJXTreeTable.ICellClassGetter
import org.jdesktop.swingx.TreeTableModelAdapterExt.ISimpleLeafsTreeModel
import org.jdesktop.swingx.treetable.TreeTableModel

class ValueTreeTableModel(private var root: ValueTree) : TreeTableModel, ICellClassGetter, ISimpleLeafsTreeModel {
    companion object {
        private val COL_OMG = 0
        private val COL_KEY = 1
        private val COL_VAL = 2
    }


    init {
        assert(root.model != null)
    }

    override fun getRoot(): Any {
        return root
    }

    fun setRoot(tree: ValueTree) {
        this.root = tree
    }

    override fun getChild(parent: Any, index: Int): Any {
        val tree = parent as ValueTree
        return tree.values[index]!!
    }

    override fun getChildCount(parent: Any): Int {
        val tree = parent as ValueTree
        val model = tree.model

        if (model == null) {
            if (tree.parent == null) {
                return 0
            }

            return tree.parent!!.model!!.children!!.size
        }

        if (model.isArray) {
            return tree.values.size
        }

        return if (model.children == null) 0 else model.children!!.size
    }

    override fun getValueAt(node: Any?, column: Int): Any? {
        if (node == null || column == COL_OMG) {
            return null
        }

        if (node !is ValueTree) {
            return if (column == COL_VAL) node else node.javaClass.simpleName
        }

        val model = node.model

        if (model != null) {
            return if (column == COL_KEY) model.name else null
        }
        else {
            assert(node.parent != null)

            if (node.parent!!.model!!.isArray) {
                return null
            }
            else {
                return node.values[0]
            }
        }
    }

    override fun getValueAt(node: Any?, parentNode: Any, nodeIndex: Int, column: Int): Any? {
        if (column == COL_OMG) {
            return null
        }

        if (node !is ValueTree) {
            if (parentNode is ValueTree) {

                if (column == COL_VAL) {
                    return parentNode.values[nodeIndex]
                }
                else if (node != null) {
                    val name = parentNode.model!!.children!![nodeIndex].name
                    val str = name + " (" + node.javaClass.simpleName + ")"

                    return str
                }
            }
        }

        return getValueAt(node, column)
    }

    override fun setValueAt(value: Any, node: Any, parentNode: Any, nodeIndex: Int, column: Int) {
        var value = value
        assert(column == COL_VAL)
        assert(node !is ValueTree)

        if (parentNode is ValueTree) {

            // TODO we shouldn't do this conversion. cellEditor should be prepared for it!
            if (value is Long) {
                if (node is Float) {
                    value = value.toFloat()
                }
            }

            parentNode.values[nodeIndex] = value
        }
    }

    override fun isCellEditable(node: Any, column: Int): Boolean {
        return column == COL_VAL && getCellClass(node, column) != null
    }

    override fun setValueAt(value: Any, node: Any, column: Int) {
        //		assert false;
    }

    override fun getCellClass(node: Any?, column: Int): Class<*>? {
        if (node != null && node !is ValueTree && column == COL_VAL) {
            return node.javaClass
        }

        return null
    }

    override fun getColumnClass(columnIndex: Int): Class<*>? {
        // should call getCellType() but can't `assert false` here
        // because of JXTreeTable internal hackery
        return null
    }

    override fun isLeaf(node: Any?): Boolean {
        if (node == null || node !is ValueTree) {
            return true
        }

        return node.model == null && (node.parent == null || node.parent!!.model!!.isLeaf)
    }

    override fun valueForPathChanged(path: TreePath, newValue: Any) {}

    override fun getIndexOfChild(parent: Any, child: Any): Int {
        val model = parent as ObjectModelNode

        var i = 0
        val n = model.children!!.size
        while (i < n) {
            if (model.children!![i] === child) {
                return i
            }
            ++i
        }

        return -1
    }

    override fun getColumnCount(): Int {
        return 3
    }

    override fun getColumnName(column: Int): String {
        if (column == COL_KEY)
            return "field"
        if (column == COL_VAL)
            return "value"

        return "omg"
    }

    override fun getHierarchicalColumn(): Int {
        return 0
    }

    override fun addTreeModelListener(listener: TreeModelListener) {}

    override fun removeTreeModelListener(listener: TreeModelListener) {}
}
