package net.namekdev.entity_tracker.ui.partials

import java.awt.LayoutManager
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.border.EmptyBorder
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn

import net.namekdev.entity_tracker.ui.model.ComponentColumnModel
import net.namekdev.entity_tracker.ui.model.EntityTableModel
import net.namekdev.entity_tracker.ui.utils.VerticalTableHeaderCellRenderer

class EntityTable(private val tableModel: EntityTableModel) : JTable() {
    private var _columnModel: ComponentColumnModel
    private val btnComponentOrderingToggle: JButton


    init {
        autoCreateRowSorter = true
        setShowVerticalLines(false)
        fillsViewportHeight = true
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        val tableHeader = getTableHeader()
        tableHeader.defaultRenderer = VerticalTableHeaderCellRenderer()

        _columnModel = ComponentColumnModel()
        setColumnModel(_columnModel)
        model = tableModel

        // set max width for first column "entity id"
        val entityIdColumn = getColumnModel().getColumn(0)
        entityIdColumn.maxWidth = 10

        // add button that can toggle ordering of component columns
        btnComponentOrderingToggle = SortToggleButton()

        val l = BoxLayout(tableHeader, BoxLayout.Y_AXIS)
        tableHeader.layout = l
        tableHeader.add(btnComponentOrderingToggle)
    }


    private inner class SortToggleButton : JButton() {
        private val sortingToggleClickListener = ActionListener {
            _columnModel.toggleOrdering()
            refreshText()
        }

        private val arrowsUnicode = "\u21C6"

        init {
            border = null

            border = EmptyBorder(3, 5, 3, 5)
            isFocusable = true
            isFocusPainted = false

            refreshText()
            addActionListener(sortingToggleClickListener)
        }

        private fun refreshText() {
            text = arrowsUnicode + " " + _columnModel.currentOrderName
        }
    }
}
