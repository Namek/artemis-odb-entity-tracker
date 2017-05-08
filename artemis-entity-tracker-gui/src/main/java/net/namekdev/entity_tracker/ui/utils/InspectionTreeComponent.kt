package net.namekdev.entity_tracker.ui.utils

import net.miginfocom.swing.MigLayout
import net.namekdev.entity_tracker.utils.serialization.NetworkDeserializer
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.DataType
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector
import net.namekdev.entity_tracker.utils.serialization.ValueTree
import org.jdeferred.Promise
import org.jdeferred.impl.DeferredObject
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField


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
        val layout = MigLayout("fillx, wrap 3", "[]5[right]10[left, 0:pref:100%, grow]", "")
        setLayout(layout)



        val nodeModel = node.model!!

        if (nodeModel.isArray) {

        }
        else if (nodeModel.dataType == DataType.Object) {
            nodeModel.children!!.forEachIndexed { i, field ->
                add(JLabel(field.name), "span 2")

                if (node.values.get(i) == null) {
                    add(JLabel("null"))
                }
                else if (field.isArray) {

                }
                else if (field.isLeaf) {
                    val value = node.values.get(i)
                    when (field.dataType) {
                        DataType.String -> {
                            add(JTextField(value.toString()))
                        }
                        DataType.Int, DataType.Short, DataType.Long -> {

                        }
                        DataType.Float, DataType.Double -> {

                        }
                        DataType.Boolean -> {
                            val el = JCheckBox()
                            el.isSelected = value as Boolean
                            add(el)
                        }
                        else -> {
                            // TODO
                        }
                    }
                }
            }
        }


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