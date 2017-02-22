package net.namekdev.entity_tracker.ui.partials

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component

import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.border.BevelBorder
import javax.swing.border.TitledBorder
import javax.swing.event.ListSelectionListener

import com.artemis.utils.BitVector
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.ui.Context
import net.namekdev.entity_tracker.ui.model.EntityTableModel
import net.namekdev.entity_tracker.ui.utils.SelectionListener
import net.namekdev.entity_tracker.utils.IndexBiMap

class EntityDetailsPanel(private val _appContext: Context, private val _entityTableModel: EntityTableModel) : JPanel() {
    var entityId = -1
        private set
    private var _currentComponentIndex = -1
    private val _componentIndices = IndexBiMap()

    private var _splitPane: JSplitPane? = null
    private var _entityPanel: JPanel? = null
    private var _componentsPanelContainer: JPanel? = null

    private var _entityTitledBorder: TitledBorder? = null
    private var _componentTitledBorder: TitledBorder? = null
    private var _componentList: JList<String>? = null
    private var _componentListModel: DefaultListModel<String>? = null


    init {

        initialize()
    }

    protected fun initialize() {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        _entityTitledBorder = TitledBorder(BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Entity 32", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, Color(0, 0, 0))
        _componentTitledBorder = TitledBorder(BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Renderable", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, Color(0, 0, 0))

        _componentListModel = DefaultListModel<String>()
        _componentList = JList(_componentListModel!!)
        _componentList!!.alignmentX = Component.LEFT_ALIGNMENT
        _componentList!!.layoutOrientation = JList.VERTICAL
        _componentList!!.border = TitledBorder("Components:")
        add(_componentList)

        _componentList!!.addListSelectionListener(_componentSelectionListener)

        // Things used to show component details
        _entityPanel = JPanel()
        _entityPanel!!.layout = BoxLayout(_entityPanel, BoxLayout.Y_AXIS)
        _entityPanel!!.border = _entityTitledBorder
        _componentsPanelContainer = JPanel()
        _componentsPanelContainer!!.border = _componentTitledBorder
        _componentsPanelContainer!!.layout = BorderLayout()
        _splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _entityPanel, _componentsPanelContainer)
        _splitPane!!.isOpaque = false
    }

    private fun setup(entityId: Int, componentTypeIndex: Int = -1) {
        if (componentTypeIndex >= 0) {
            // show component details
            removeAll()
            add(_splitPane)
            border = null
            _entityPanel!!.border = _entityTitledBorder
            _entityPanel!!.add(_componentList)
        }
        else if (componentTypeIndex < 0 && _currentComponentIndex >= 0) {
            // show only entity info
            removeAll()
            add(_componentList)
            border = _entityTitledBorder
        }

        if (entityId != this.entityId) {
            val entityComponents = _entityTableModel.getEntityComponents(entityId)

            _entityTitledBorder!!.title = "Entity #" + entityId

            _componentIndices.ensureSize(_entityTableModel.columnCount)
            _componentListModel!!.clear()
            var i = entityComponents.nextSetBit(0)
            var j = 0
            while (i >= 0) {
                val info = _entityTableModel.getComponentTypeInfo(i)

                _componentListModel!!.addElement(info.name)
                _componentIndices.set(j, i)
                i = entityComponents.nextSetBit(i + 1)
                ++j
            }

            this.entityId = entityId
        }

        if (componentTypeIndex >= 0) {
            val info = _entityTableModel.getComponentTypeInfo(componentTypeIndex)

            _componentTitledBorder!!.title = info.name
            _componentsPanelContainer!!.removeAll()
            _componentsPanelContainer!!.add(ComponentDataPanel(_appContext, info, entityId), BorderLayout.PAGE_START)

            _appContext.worldController!!.requestComponentState(this.entityId, componentTypeIndex)
        }
        _currentComponentIndex = componentTypeIndex

        revalidate()
        repaint(50)

    }

    fun selectComponent(entityId: Int, componentIndex: Int) {
        setup(entityId, componentIndex)
        val rowIndex = _componentIndices.getLocalIndex(componentIndex)
        _componentList!!.selectedIndex = rowIndex
    }

    private val _componentSelectionListener = object : SelectionListener() {
        override fun rowSelected(rowIndex: Int) {
            if (rowIndex >= 0) {
                val componentIndex = _componentIndices.getGlobalIndex(rowIndex)

                setup(entityId, componentIndex)
            }
        }
    }
}
