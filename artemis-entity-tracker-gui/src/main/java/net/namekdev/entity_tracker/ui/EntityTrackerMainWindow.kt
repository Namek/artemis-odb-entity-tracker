package net.namekdev.entity_tracker.ui

import java.awt.CardLayout
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.Enumeration

import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.UIManager.LookAndFeelInfo
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel

import com.artemis.utils.BitVector
import net.namekdev.entity_tracker.connectors.WorldController
import net.namekdev.entity_tracker.connectors.WorldUpdateInterfaceListener
import net.namekdev.entity_tracker.connectors.WorldUpdateListener
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.ui.listener.ChangingSystemEnabledStateListener
import net.namekdev.entity_tracker.ui.model.BaseSystemTableModel
import net.namekdev.entity_tracker.ui.model.EntitySystemTableModel
import net.namekdev.entity_tracker.ui.model.EntityTableModel
import net.namekdev.entity_tracker.ui.model.ManagerTableModel
import net.namekdev.entity_tracker.ui.partials.EntityDetailsPanel
import net.namekdev.entity_tracker.ui.partials.EntityTable
import net.namekdev.entity_tracker.ui.utils.AdjustableJTable
import net.namekdev.entity_tracker.ui.utils.VerticalTableHeaderCellRenderer

class EntityTrackerMainWindow(showWindowOnStart: Boolean, exitApplicationOnClose: Boolean) : WorldUpdateInterfaceListener {
    protected val context = Context()
    protected lateinit var frame: JFrame
    private var entitiesTable: JTable? = null
    private var tableScrollPane: JScrollPane? = null
    private var filtersScrollPane: JScrollPane? = null
    private var detailsPanelContainer: JScrollPane? = null
    private lateinit var entitiesTableModel: EntityTableModel
    private var entitySystemsTableModel: EntitySystemTableModel? = null
    private var baseSystemsTableModel: BaseSystemTableModel? = null
    private var managersTableModel: ManagerTableModel? = null
    private var mainSplitPane: JSplitPane? = null
    private var tableFiltersSplitPane: JSplitPane? = null
    private var systemsDetailsSplitPane: JSplitPane? = null
    private var filtersPanel: JPanel? = null
    private var systemsManagersPanel: JPanel? = null
    private var entitySystemsTable: JTable? = null
    private var baseSystemsTable: JTable? = null
    private var managersTable: JTable? = null
    private var tabbedPane: JTabbedPane? = null
    private var entityDetailsPanel: EntityDetailsPanel? = null

    private var _lastSelectedCol: Int = 0

    @JvmOverloads constructor(exitApplicationOnClose: Boolean = false) : this(true, exitApplicationOnClose) {}


    protected fun initialize(showWindowOnStart: Boolean, exitApplicationOnClose: Boolean) {
        frame = JFrame("Artemis Entity Tracker")
        frame.defaultCloseOperation = if (exitApplicationOnClose) JFrame.EXIT_ON_CLOSE else JFrame.DISPOSE_ON_CLOSE
        frame.setBounds(100, 100, 959, 823)
        frame.contentPane.layout = BoxLayout(frame.contentPane, BoxLayout.X_AXIS)

        entitiesTableModel = EntityTableModel()
        entitiesTable = EntityTable(entitiesTableModel)

        tableScrollPane = JScrollPane()
        tableScrollPane!!.setViewportView(entitiesTable)


        filtersPanel = JPanel()
        //		filtersPanel.add(new JLabel("TODO filters here"));

        filtersScrollPane = JScrollPane(filtersPanel)

        systemsManagersPanel = JPanel()
        systemsManagersPanel!!.layout = CardLayout(0, 0)
        entitySystemsTableModel = EntitySystemTableModel()
        baseSystemsTableModel = BaseSystemTableModel()
        managersTableModel = ManagerTableModel()

        tabbedPane = JTabbedPane(JTabbedPane.TOP)
        systemsManagersPanel!!.add(tabbedPane!!, "name_959362872326203")

        entitySystemsTable = AdjustableJTable()
        entitySystemsTable!!.autoCreateRowSorter = true
        entitySystemsTable!!.fillsViewportHeight = true
        entitySystemsTable!!.showVerticalLines = false
        entitySystemsTable!!.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        entitySystemsTable!!.model = entitySystemsTableModel!!
        val entitySystemsTableScrollPane = JScrollPane()
        entitySystemsTableScrollPane.setViewportView(entitySystemsTable)
        tabbedPane!!.addTab("Entity Systems", null, entitySystemsTableScrollPane, null)

        baseSystemsTable = AdjustableJTable()
        baseSystemsTable!!.autoCreateRowSorter = true
        baseSystemsTable!!.fillsViewportHeight = true
        baseSystemsTable!!.showVerticalLines = false
        baseSystemsTable!!.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        baseSystemsTable!!.model = baseSystemsTableModel!!
        val baseSystemsTableScrollPane = JScrollPane()
        baseSystemsTableScrollPane.setViewportView(baseSystemsTable)
        tabbedPane!!.addTab("Base Systems", null, baseSystemsTableScrollPane, null)

        managersTable = JTable()
        managersTable!!.autoCreateRowSorter = true
        managersTable!!.fillsViewportHeight = true
        managersTable!!.showVerticalLines = false
        managersTable!!.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        managersTable = AdjustableJTable()
        managersTable!!.model = managersTableModel!!
        val managersTableScrollPane = JScrollPane()
        managersTableScrollPane.setViewportView(managersTable)
        tabbedPane!!.addTab("Managers", null, managersTableScrollPane, null)

        detailsPanelContainer = JScrollPane()
        detailsPanelContainer!!.setViewportView(JLabel("Select entity from the table to inspect entity components."))

        systemsDetailsSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, systemsManagersPanel, detailsPanelContainer)

        tableFiltersSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, filtersScrollPane)
        tableFiltersSplitPane!!.resizeWeight = 1.0

        mainSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, tableFiltersSplitPane, systemsDetailsSplitPane)
        mainSplitPane!!.resizeWeight = 0.5
        frame.contentPane.add(mainSplitPane)

        frame.isVisible = showWindowOnStart

        entitiesTable!!.addMouseListener(entityRowCellSelectionListener)
        entitiesTable!!.addKeyListener(entityTableKeyListener)
        entityDetailsPanel = EntityDetailsPanel(context, entitiesTableModel)



        entitySystemsTableModel!!.addChangingSystemEnabledStateListener(systemEnableChangingListener)
        baseSystemsTableModel!!.addChangingSystemEnabledStateListener(systemEnableChangingListener)

        managersTableModel!!.addChangingSystemEnabledStateListener(object : ChangingSystemEnabledStateListener {
            override fun onChangingSystemEnabledState(model: BaseSystemTableModel, systemIndex: Int, managerName: String, enabled: Boolean) {
                context.worldController!!.setManagerState(managerName, enabled)
            }
        })
    }

    var isVisible: Boolean
        get() = frame.isVisible
        set(visible) {
            frame.isVisible = visible
        }

    private fun selectEntity(viewRow: Int, viewCol: Int) {
        if (viewRow < 0) {
            return
        }
        val modelRow = entitiesTable!!.convertRowIndexToModel(viewRow)
        val modelCol = entitiesTable!!.convertColumnIndexToModel(viewCol)

        val entityId = entitiesTableModel!!.getValueAt(modelRow, 0) as Int
        var componentIndex = modelCol - 1

        val entityComponents = entitiesTableModel!!.getEntityComponents(entityId)

        if (componentIndex >= 0 && !entityComponents.get(componentIndex)) {
            componentIndex = -1
        }

        showEntityDetails(entityId, componentIndex)
        _lastSelectedCol = modelCol
    }

    override fun injectWorldController(worldController: WorldController) {
        context.worldController = worldController
    }

    override val listeningBitset: Int
        get() = WorldUpdateListener.ENTITY_ADDED or WorldUpdateListener.ENTITY_DELETED or WorldUpdateListener.ENTITY_SYSTEM_STATS

    override fun addedSystem(index: Int, name: String, allTypes: BitVector, oneTypes: BitVector, notTypes: BitVector) {
        val hasAspect = allTypes != null || oneTypes != null || notTypes != null

        SwingUtilities.invokeLater {
            baseSystemsTableModel!!.setSystem(index, name)

            if (hasAspect) {
                entitySystemsTableModel!!.setSystem(index, name)
            }
        }
    }

    override fun addedManager(name: String) {
        SwingUtilities.invokeLater { managersTableModel!!.addManager(name) }
    }

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
        SwingUtilities.invokeLater {
            val columns = entitiesTable!!.columnModel
            val col = TableColumn(columns.columnCount)
            col.headerValue = info.name
            col.modelIndex = info.index
            columns.addColumn(col)

            entitiesTableModel!!.setComponentType(index, info)
            setupAllColumnHeadersVerticalRenderer()
        }
    }

    override fun updatedEntitySystem(systemIndex: Int, entitiesCount: Int, maxEntitiesCount: Int) {
        SwingUtilities.invokeLater { entitySystemsTableModel!!.updateSystem(systemIndex, entitiesCount, maxEntitiesCount) }
    }

    override fun addedEntity(entityId: Int, components: BitVector) {
        SwingUtilities.invokeLater { entitiesTableModel!!.addEntity(entityId, components) }
    }

    override fun deletedEntity(entityId: Int) {
        SwingUtilities.invokeLater { entitiesTableModel!!.removeEntity(entityId) }
    }

    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
        context.eventBus.updatedComponentState(entityId, componentIndex, valueTree)
    }

    override fun disconnected() {
        entitiesTableModel!!.clear()
        entitySystemsTableModel!!.clear()
        managersTableModel!!.clear()
        detailsPanelContainer!!.setViewportView(null)
    }

    private fun setupAllColumnHeadersVerticalRenderer() {
        val headerRenderer = VerticalTableHeaderCellRenderer()
        val columns = entitiesTable!!.columnModel
        val columnIter = columns.columns

        while (columnIter.hasMoreElements()) {
            val column = columnIter.nextElement()
            column.headerRenderer = headerRenderer
        }
    }

    protected fun showEntityDetails(entityId: Int, componentIndex: Int) {
        SwingUtilities.invokeLater {
            entityDetailsPanel!!.selectComponent(entityId, componentIndex)

            if (detailsPanelContainer!!.viewport.view !== entityDetailsPanel) {
                detailsPanelContainer!!.setViewportView(entityDetailsPanel)
                detailsPanelContainer!!.revalidate()
                detailsPanelContainer!!.repaint()
            }
        }
    }

    private val entityRowCellSelectionListener = object : MouseAdapter() {
        override fun mousePressed(evt: MouseEvent?) {
            val row = entitiesTable!!.rowAtPoint(evt!!.point)
            val col = entitiesTable!!.columnAtPoint(evt.point)
            selectEntity(row, col)
        }
    }

    private val entityTableKeyListener = object : KeyListener {
        override fun keyTyped(e: KeyEvent) {}

        override fun keyPressed(e: KeyEvent) {
            val key = e.keyCode

            when (key) {
                KeyEvent.VK_UP, KeyEvent.VK_DOWN -> {
                    val selection = entitiesTable!!.selectionModel
                    var currentIndex = selection.minSelectionIndex

                    if (key == KeyEvent.VK_UP && currentIndex > 0) {
                        currentIndex -= 1
                    }
                    else if (key == KeyEvent.VK_DOWN && currentIndex < entitiesTable!!.rowCount - 1) {
                        currentIndex += 1
                    }

                    selection.setSelectionInterval(currentIndex, currentIndex)
                    selectEntity(currentIndex, _lastSelectedCol)
                }
            }

            e.consume()
        }

        override fun keyReleased(e: KeyEvent) {}
    }

    private val systemEnableChangingListener = object : ChangingSystemEnabledStateListener {
        override fun onChangingSystemEnabledState(model: BaseSystemTableModel, systemIndex: Int, systemName: String, enabled: Boolean) {
            entitySystemsTableModel!!.updateSystemState(systemIndex, enabled)
            baseSystemsTableModel!!.updateSystemState(systemIndex, enabled)

            context.worldController!!.setSystemState(systemName, enabled)
        }
    }

    init {
        try {
            for (info in UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus" == info.name) {
                    UIManager.setLookAndFeel(info.className)
                    break
                }
            }
        }
        catch (exc: Exception) {
        }

        initialize(showWindowOnStart, exitApplicationOnClose)
    }
}
