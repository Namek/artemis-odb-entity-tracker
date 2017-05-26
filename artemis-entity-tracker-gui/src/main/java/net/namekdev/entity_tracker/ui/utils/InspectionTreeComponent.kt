package net.namekdev.entity_tracker.ui.utils

import net.miginfocom.swing.MigLayout
import net.namekdev.entity_tracker.utils.serialization.NetworkDeserializer
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.DataType
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ValueTree
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import javax.swing.AbstractButton
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JToggleButton


interface TreeDataProvider {
    fun getObjectId(): Int
    fun getNodeInfoByIndexChain(indices: Array<Int>): Promise<ValueTree,*,*>
}

object TreeDataProviderTestImpl : TreeDataProvider {
    val gameStateAsValueTree: ValueTree

    init {
        val gameState = GameState()
        gameState.objects = arrayOf(GameObject(), GameObject(), GameObject(), GameObject())

        val serializer = NetworkSerializer()
        val deserializer = NetworkDeserializer()

        serializer.addObject(gameState)
        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        gameStateAsValueTree = deserializer.readObject(true)
    }

    override fun getObjectId(): Int {
        return 1
    }

    override fun getNodeInfoByIndexChain(indices: Array<Int>): Promise<ValueTree, *, *> {
        val deferred = DeferredObject<ValueTree, Any, Any>()

        if (indices.size == 1 && indices.get(0) == 0) {
            deferred.resolve(gameStateAsValueTree)
        }

        return deferred.promise()
    }

}

/**
 *
 */
class InspectionTreeComponent(
    val dataProvider: TreeDataProvider = TreeDataProviderTestImpl
) : JPanel() {
    init {
        setLayout(BorderLayout())

        dataProvider.getNodeInfoByIndexChain(arrayOf(0)).then { rootNode ->
            val treeTable = InspectionTreeNode(dataProvider, rootNode, 0)
            add(treeTable)
        }
    }
}

class InspectionTreeNode(
    val dataProvider: TreeDataProvider,
    val node: ValueTree,
    val depth: Int
) : JPanel() {
    init {


        fun init_panel(panel: JPanel, isRoot: Boolean = false): JPanel {
            val leftGap = if (isRoot) "5" else "15"
            val layout = MigLayout(
                "fillx, wrap 3, debug",                           // layout constraints
                leftGap + "[]5[left]10[left, 0:pref:100%, grow]", // column constraints
                ""                                                // row constraints
            )
            panel.setLayout(layout)
            return panel
        }

        fun new_panel(parent: JPanel): JPanel {
            val panel = init_panel(JPanel())
            parent.add(panel, "span 3")
            return panel
        }

        fun add_row(i: Int, model: ObjectModelNode, parentPanel: JPanel) {
            if (node.values.get(i) == null) {
                parentPanel.add(JLabel(model.name), "span 2")
                parentPanel.add(JLabel("null"))
            }
            if (model.isArray) {
                val btnExpand = ExpandCollapseButton()
                parentPanel.add(btnExpand)

                val arrSuffix = (
                    if (model.isSubTypePrimitive)
                        '[' + model.dataSubType.toString() + ']'
                    else
                        "[]"
                )
                parentPanel.add(JLabel(model.name + ' ' + arrSuffix))
                parentPanel.add(JLabel("size=" + node.values.size.toString()))

                val panel = new_panel(parentPanel)

            }
            else if (model.dataType == DataType.Object) {
                val btnExpand = ExpandCollapseButton()
                parentPanel.add(btnExpand)

                parentPanel.add(JLabel(model.toString()), "span 2")

                val panel = new_panel(parentPanel)
                model.children!!.forEachIndexed { i, model ->
                    add_row(i, model, panel)
                }
            }
            else if (model.isLeaf) {
                parentPanel.add(JLabel(""))
                parentPanel.add(JLabel(model.name))
                val value = node.values.get(i)
                when (model.dataType) {
                    DataType.String -> {
                        parentPanel.add(JLabel(value.toString()))
                    }
                    DataType.Int, DataType.Short, DataType.Long -> {
                        parentPanel.add(JLabel(value.toString()))
                    }
                    DataType.Float, DataType.Double -> {
                        parentPanel.add(JLabel(value.toString()))
                    }
                    DataType.Boolean -> {
                        val el = JCheckBox()
                        el.isSelected = value as Boolean
                        parentPanel.add(el)
                    }
                    else -> {
                        // TODO
                    }
                }
            }
        }
        System.out.println(System.getProperty("file.encoding"));

        add_row(0, node.model!!, init_panel(this, true))

//        nodeModel.children!!.forEachIndexed { i, model ->
//            add_row(i, model)
//        }



//        val layout = MigLayout("fillx, wrap 3", "[]5[right]10[left, 0:pref:100%, grow]", "")
//        setLayout(layout)
//
//        repeat(10) {
//            val btnToggle = JButton("+")
//            val label = JLabel("fieldNam asd a asd e")
//            val value: JComponent = JTextField("valu dfg dfgfd g dfgdf fgde")
//
//            add(btnToggle)
//            add(label)
//            add(value, "wrap")
//
//            val childPanel = JPanel()
//            add(childPanel, "span 3")
//
//            // add children here and some left margin
//            childPanel.add(JLabel("asd asdas dsa dasd sad asdsad sd sdf dsfds sf asd as asd asdsa ads "))
//        }
    }
}

class ExpandCollapseButton : JButton() {
    var isExpanded: Boolean = false

    init {
        this.addActionListener { evt ->
            isExpanded = !isExpanded
            text = if (isExpanded) "▼" else "▶"
        }
        text = "▶"

        margin = Insets(0,0,0,0)
        border = null
        preferredSize = Dimension(15, 15)
        isOpaque = true
        isBorderPainted = false
    }
}


internal class GameState {
    var objects: Array<Any>? = null
    var omg = true
}

internal class GameObject {
    var pos = Vector3(1f, 2f, 3f)
    var size = Vector2(10f, 5f)
}

internal class Vector3(var x: Float, var y: Float, var z: Float)

internal class Vector2(var x: Float, var y: Float)


/*
class InspectionTreeNode(
    model: ObjectModelNode, value: ValueTree?
)
    : JComponent() {
    init {


    }
}
/*

class InspectionTreeTableModel : TreeTableModel {
    val modelListeners = ArrayList<TreeModelListener>()


    override fun removeTreeModelListener(l: TreeModelListener?) {
        modelListeners.remove(l)
    }
    override fun addTreeModelListener(l: TreeModelListener?) {
        if (l != null)
            modelListeners.add(l)
    }


    override fun getHierarchicalColumn(): Int = 0

    override fun getColumnClass(columnIndex: Int): Class<*> {
        when (columnIndex) {
            1 -> String.javaClass // TODO it depends on the value type!
        }
    }

    override fun getChild(parent: Any?, index: Int): Any {
        TODO("not implemented")
    }

    override fun getRoot(): Any {
        TODO("not implemented")
    }

    override fun isLeaf(node: Any?): Boolean {
        TODO("not implemented")
    }

    override fun getChildCount(parent: Any?): Int {
        TODO("not implemented")
    }

    override fun getColumnName(column: Int): String {
        TODO("not implemented")
    }

    override fun isCellEditable(node: Any?, column: Int): Boolean {
        TODO("not implemented")
    }

    override fun valueForPathChanged(path: TreePath?, newValue: Any?) {
        TODO("not implemented")
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        TODO("not implemented")
    }

    override fun setValueAt(value: Any?, node: Any?, column: Int) {
        TODO("not implemented")
    }

    override fun getColumnCount(): Int {
        TODO("not implemented")
    }

    override fun getValueAt(node: Any?, column: Int): Any {
        TODO("not implemented")
    }


}*/