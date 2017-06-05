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
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel


interface TreeDataProvider {
    fun getObjectId(): Int
    fun getNodeInfoByIndexChain(indices: Array<Int>): Promise<ValueTree,*,*>
}

object TreeDataProviderTestImpl : TreeDataProvider {
    val gameStateAsValueTree: ValueTree

    init {
        val gameState = GameState()
        val cyclicNode = GameNode()
        val cyclicNodeChildren = arrayOf(cyclicNode)
        cyclicNode.children = cyclicNodeChildren
        gameState.objects = arrayOf(GameNode(), GameNode(arrayOf(GameNode())), cyclicNode)

        val serializer = NetworkSerializer()
        val deserializer = NetworkDeserializer()

        serializer.addObject(gameState)
        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        gameStateAsValueTree = deserializer.readObject(true)!!
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
        val toggleBtns = mutableListOf<ExpandCollapseButton>()


        fun init_panel(panel: JPanel, isRoot: Boolean = false): JPanel {
            val leftGap = if (isRoot) "5" else "15"
            val layout = MigLayout(
                "fillx, wrap 3",                                  // layout constraints
                leftGap + "[]5[left]10[left, 0:pref:100%, grow]", // column constraints
                ""                                                // row constraints
            )
            panel.layout = layout
            return panel
        }

        fun new_collapsable_panel(parent: JPanel, btnExpand: ExpandCollapseButton): JPanel {
            val panel = init_panel(JPanel())
            parent.add(panel, "span 3, growx")

            val origMaxSize = panel.maximumSize
            val invisibleSize = Dimension(0, 0)

            fun refresh() {
                val isExpanded = btnExpand.isExpanded
                panel.maximumSize = if (isExpanded) origMaxSize else invisibleSize
                panel.isVisible = isExpanded
                panel.repaint()
            }

            refresh()
            btnExpand.addActionListener { refresh() }

            return panel
        }

        fun add_row(i: Int, node: Any?, model: ObjectModelNode?, parentPanel: JPanel, visitedObjIds: Stack<Short>) {
            val visitedObjIdsBeginSize = visitedObjIds.size

            if (node !is ValueTree) {
                if (model != null && model.isLeaf) {
                    parentPanel.add(JLabel(""))
                    parentPanel.add(JLabel(model.name))
                    val value = node
                    when (model.dataType) {
                        DataType.String -> {
                            parentPanel.add(JLabel(value.toString()))
                        }
                        DataType.Int, DataType.Short, DataType.Long -> {
                            parentPanel.add(JLabel(value.toString()))
                        }
                        DataType.Byte -> {
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
                        DataType.BitVector -> {
                            parentPanel.add(JLabel(value.toString()))
                        }
                        DataType.Enum -> {
                            parentPanel.add(JLabel("enum: " + value.toString()))
                        }
                    }
                }
            }
            else if (model != null) {
                if (node != null && node.id >= 1) {
                    if (visitedObjIds.contains(node.id)) {
                        // we have a cyclic reference here!
                        // TODO add a button that would dynamically open/expand the object
                        parentPanel.add(JLabel(model.name), "span 2")
                        parentPanel.add(JLabel("<cyclic>"))
                        return
                    }
                    else visitedObjIds.add(node.id)
                }

                if (node == null) {
                    parentPanel.add(JLabel(model.name), "span 2")
                    parentPanel.add(JLabel("null"))
                }
                else if (model.isArray) {
                    val btnExpand = ExpandCollapseButton()
                    toggleBtns.add(btnExpand)
                    parentPanel.add(btnExpand)

                    val arrSuffix = (
                        if (model.isSubTypePrimitive)
                            '[' + model.dataSubType.toString() + ']'
                        else
                            "[]"
                    )
                    parentPanel.add(JLabel(model.name + ' ' + arrSuffix))
                    parentPanel.add(JLabel("size=" + node.values.size.toString()))

                    val panel = new_collapsable_panel(parentPanel, btnExpand)

                    for (j in node.values.indices) {
                        val subnode = node.values[j]
                        val subnodeModel = if (subnode is ValueTree) subnode.model else null
                        add_row(j, subnode, subnodeModel, panel, visitedObjIds)
                    }
                }
                else if (model.dataType == DataType.Object) {
                    val btnExpand = ExpandCollapseButton()
                    toggleBtns.add(btnExpand)
                    parentPanel.add(btnExpand)

                    parentPanel.add(JLabel(model.toString()), "span 2")

                    val panel = new_collapsable_panel(parentPanel, btnExpand)
                    for (j in node.values.indices) {
                        add_row(j, node.values[j], model.children!![j], panel, visitedObjIds)
                    }
                }
            }
            else {
                throw RuntimeException("why is the model == null?")
            }

            val diff = visitedObjIds.size - visitedObjIdsBeginSize
            for (i in 0..diff-1) {
                visitedObjIds.pop()
            }
        }

        add_row(0, node, node.model!!, init_panel(this, true), Stack<Short>())
    }
}

class ExpandCollapseButton : JButton() {
    var isExpanded: Boolean = true

    init {
        margin = Insets(0,0,0,0)
        border = null
        preferredSize = Dimension(15, 15)
        isOpaque = true
        isBorderPainted = false
        refresh()
    }

    private fun refresh() {
        text = if (isExpanded) "▼" else "▶"
    }

    override fun processMouseEvent(evt: MouseEvent?) {
        if (evt!!.id == MouseEvent.MOUSE_RELEASED) {
            isExpanded = !isExpanded
            refresh()
        }
        super.processMouseEvent(evt)
    }
}


internal class GameState {
    var objects: Array<Any>? = null
    var bool = true
    var float = 0.5f
    var double = 0.5
    var byte = 5.toByte()
    var short = 5.toShort()
    var int = 5.toInt()
    var long = 5.toLong()
    var string = "5"
    var nullableObj: Object? = null
    var enum: Direction = Direction.SOUTH
    var enumNullable: Direction? = null

    // TODO: array of arrays, enums, cyclic references
}

internal class GameNode(var children: Array<GameNode>? = null) {
    var pos = Vector3(1f, 2f, 3f)
    var size = Vector2(10f, 5f)
}

internal class Vector3(var x: Float, var y: Float, var z: Float)

internal class Vector2(var x: Float, var y: Float)

internal enum class Direction {
    NORTH, SOUTH, WEST, EAST
}