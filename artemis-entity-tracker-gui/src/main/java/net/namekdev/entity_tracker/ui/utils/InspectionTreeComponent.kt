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

        fun add_row(i: Int, node: Any?, model: ObjectModelNode?, parentPanel: JPanel) {
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

                return
            }

            val model = model!!

            if (node == null/* || node.values[i] == null*/) {
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
                    add_row(j, subnode, subnodeModel, panel)
                }
            }
            else if (model.dataType == DataType.Object) {
                val btnExpand = ExpandCollapseButton()
                toggleBtns.add(btnExpand)
                parentPanel.add(btnExpand)

                parentPanel.add(JLabel(model.toString()), "span 2")

                val panel = new_collapsable_panel(parentPanel, btnExpand)
                for (j in node.values.indices) {
                    add_row(j, node.values[j], model.children!![j], panel)
                }
            }
        }

        add_row(0, node, node.model!!, init_panel(this, true))
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
    var omg = true
}

internal class GameObject {
    var pos = Vector3(1f, 2f, 3f)
    var size = Vector2(10f, 5f)
}

internal class Vector3(var x: Float, var y: Float, var z: Float)

internal class Vector2(var x: Float, var y: Float)
