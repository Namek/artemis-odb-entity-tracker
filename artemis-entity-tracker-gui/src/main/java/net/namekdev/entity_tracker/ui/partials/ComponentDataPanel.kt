package net.namekdev.entity_tracker.ui.partials

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.ArrayList
import java.util.Vector

import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

import net.miginfocom.swing.MigLayout
import net.namekdev.entity_tracker.connectors.DummyWorldUpdateListener
import net.namekdev.entity_tracker.connectors.WorldUpdateListener
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.model.FieldInfo
import net.namekdev.entity_tracker.ui.Context
import net.namekdev.entity_tracker.ui.model.ValueTreeTableModel
import net.namekdev.entity_tracker.utils.serialization.NetworkDeserializer
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer.SerializationResult
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector
import net.namekdev.entity_tracker.utils.serialization.ValueTree

import org.jdesktop.swingx.DynamicJXTreeTable
import org.jdesktop.swingx.JXTreeTable
import org.jdesktop.swingx.treetable.AbstractTreeTableModel


class ComponentDataPanel(private val _appContext: Context, private val _info: ComponentTypeInfo, private val _entityId: Int) : JPanel() {
    private val _components: Vector<JComponent>? = null

    private var treeTableModel: ValueTreeTableModel? = null
    private var treeTable: JXTreeTable? = null


    init {

        initialize()
    }

    protected fun initialize() {
        val layout = MigLayout("", "[grow]", "")
        setLayout(layout)

        treeTableModel = testModel
        treeTable = DynamicJXTreeTable(treeTableModel, treeTableModel)
        add(treeTable!!, "growx")

        /*
		MigLayout layout = new MigLayout("wrap", "[right][0:pref,grow]", "");
		setLayout(layout);

		int size = _info.fields.size();
		_components = new Vector<JComponent>(size);

		for (int i = 0; i < size; ++i) {
			FieldInfo field = _info.fields.get(i);

			JLabel label = new JLabel(field.fieldName);
			JComponent value = null;

			if (field.valueType == TYPE_BOOLEAN) {
				JCheckBox checkbox = new JCheckBox();
				value = checkbox;
				setupCheckBoxListener(checkbox, i);
			}
			else if (field.valueType == TYPE_UNKNOWN) {
				value = new JLabel("<reference>");
			}
			else {
				final JTextField textField = new JTextField();
				value = textField;
				setupTextFieldListener(textField, i);
			}

			_components.add(value);

			add(label);
			add(value, "width min:50, grow");
		}

		// register listener for component data
		_appContext.eventBus.registerListener(worldListener);*/
    }

    private //		model.id = id; /??????? TODO
    val testModel: ValueTreeTableModel
        get() {
            val deserializer = NetworkDeserializer()
            val inspector = ObjectTypeInspector()

            val gameState = GameState()
            gameState.objects = arrayOf(GameObject(), GameObject(), GameObject(), GameObject())

            val serializer = NetworkSerializer().reset()
            val model = inspector.inspect(GameState::class.java)


            serializer.addDataDescriptionOrRef(model)
            serializer.addObject(model, gameState)

            val serialized = serializer.result
            deserializer.setSource(serialized.buffer, 0, serialized.size)

            val model2 = deserializer.readDataDescription()

            val result = deserializer.readObject(model2, true)

            return ValueTreeTableModel(result)
        }

    inner class GameState {
        var objects: Array<GameObject>? = null
        var omg: Boolean = false
    }

    inner class GameObject {
        var pos = Vector3(1f, 2f, 3f)
        var size = Vector2(10f, 5f)
    }

    inner class Vector3(var x: Float, var y: Float, var z: Float)
    inner class Vector2(var x: Float, var y: Float)
    internal inner class MyTreeNode {
        var name: String? = null
        var description: String? = null
        private val children = ArrayList<MyTreeNode>()

        constructor() {}

        constructor(name: String, description: String) {
            this.name = name
            this.description = description
        }

        fun getChildren(): MutableList<MyTreeNode> {
            return children
        }

        override fun toString(): String {
            return "MyTreeNode: $name, $description"
        }
    }

    inner class MyTreeTableModel : AbstractTreeTableModel() {
        private val myroot: MyTreeNode

        init {
            myroot = MyTreeNode("root", "Root of the tree")

            myroot.getChildren().add(MyTreeNode("Empty Child 1",
                "This is an empty child"))

            val subtree = MyTreeNode("Sub Tree",
                "This is a subtree (it has children)")
            subtree.getChildren().add(MyTreeNode("EmptyChild 1, 1",
                "This is an empty child of a subtree"))
            subtree.getChildren().add(MyTreeNode("EmptyChild 1, 2",
                "This is an empty child of a subtree"))
            myroot.getChildren().add(subtree)

            myroot.getChildren().add(MyTreeNode("Empty Child 2",
                "This is an empty child"))

        }

        override fun getColumnCount(): Int {
            return 3
        }

        override fun getColumnName(column: Int): String {
            when (column) {
                0 -> return "Name"
                1 -> return "Description"
                2 -> return "Number Of Children"
                else -> return "Unknown"
            }
        }

        override fun getValueAt(node: Any, column: Int): Any {
            println("getValueAt: $node, $column")
            val treenode = node as MyTreeNode
            when (column) {
                0 -> return treenode.name as Any
                1 -> return treenode.description as Any
                2 -> return treenode.getChildren().size
                else -> return "Unknown"
            }
        }

        override fun getChild(node: Any, index: Int): Any {
            val treenode = node as MyTreeNode
            return treenode.getChildren()[index]
        }

        override fun getChildCount(parent: Any): Int {
            val treenode = parent as MyTreeNode
            return treenode.getChildren().size
        }

        override fun getIndexOfChild(parent: Any, child: Any): Int {
            val treenode = parent as MyTreeNode
            var i = 0
            while (i > treenode.getChildren().size) {
                if (treenode.getChildren()[i] === child) {
                    return i
                }
                i++
            }

            return 0
        }

        override fun isLeaf(node: Any): Boolean {
            val treenode = node as MyTreeNode
            if (treenode.getChildren().size > 0) {
                return false
            }
            return true
        }

        override fun getRoot(): Any {
            return myroot
        }
    }

    private fun setupCheckBoxListener(checkbox: JCheckBox, treePath: IntArray) {
        checkbox.addActionListener {
            val value = checkbox.isSelected
            _appContext.worldController!!.setComponentFieldValue(_entityId, _info.index, treePath, value)
        }
    }

    private fun setupTextFieldListener(textField: JTextField, treePath: IntArray) {
        textField.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {

            }

            override fun keyReleased(e: KeyEvent) {

            }

            override fun keyPressed(e: KeyEvent) {
                // on enter
                if (e.keyCode == 10) {
                    val text = textField.text

                    // TODO convert string to appropriate field's type
                    _appContext.worldController!!.setComponentFieldValue(_entityId, _info.index, treePath, text)
                }
            }
        })
    }

    override fun removeNotify() {
        super.removeNotify()
        _appContext.eventBus.unregisterListener(worldListener)
    }


    private val worldListener = object : DummyWorldUpdateListener() {
        override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
            if (_info.index != componentIndex || _entityId != entityId) {
                return
            }

            treeTableModel!!.setRoot(valueTree as ValueTree)
            /*
			for (int i = 0, n = values.length; i < n; ++i) {
				Object value = values[i];

				if (value == null) {
					continue;
				}

				Component component = _components.get(i);
				FieldInfo info = _info.fields.get(i);

				if (info.valueType == TYPE_BOOLEAN) {
					((JCheckBox) component).setSelected((Boolean) values[i]);
				}
				else if (info.valueType != TYPE_UNKNOWN) {
					((JTextField) component).setText(values[i].toString());
				}
			}*/
        }
    }
}
