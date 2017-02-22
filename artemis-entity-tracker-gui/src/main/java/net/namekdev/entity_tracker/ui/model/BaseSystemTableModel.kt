package net.namekdev.entity_tracker.ui.model

import java.util.ArrayList

import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener

import net.namekdev.entity_tracker.ui.listener.ChangingSystemEnabledStateListener
import net.namekdev.entity_tracker.ui.utils.ExtendedTableModel

open class BaseSystemTableModel : ExtendedTableModel() {
    private val _listeners = ArrayList<ChangingSystemEnabledStateListener>()

    init {
        addColumn("")
        addColumn("system")

        addTableModelListener(TableModelListener { e ->
            if (e.column != 0) {
                return@TableModelListener
            }

            val rowIndex = e.firstRow
            val systemIndex = getSystemIndex(rowIndex)
            val systemName = getSystemName(systemIndex)
            val enabled = getSystemState(systemIndex)

            for (l in _listeners) {
                l.onChangingSystemEnabledState(this@BaseSystemTableModel, systemIndex, systemName, enabled)
            }
        })
    }

    fun addChangingSystemEnabledStateListener(listener: ChangingSystemEnabledStateListener) {
        _listeners.add(listener)
    }

    override fun getColumnClass(columnIndex: Int): Class<*>? {
        when (columnIndex) {
            0 -> return Boolean::class.java
            1 -> return String::class.java
            else -> return null
        }
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return column == 0
    }

    open fun getSystemIndex(rowIndex: Int): Int {
        return rowIndex
    }

    open fun setSystem(systemIndex: Int, name: String) {
        for (i in rowCount..systemIndex) {
            addRow(arrayOf(true, ""))
        }

        setValueAt(name, systemIndex, 1)
    }

    /**
     * Update system state without firing events.
     */
    open fun updateSystemState(systemIndex: Int, enabled: Boolean) {
        updateValueAt(enabled, systemIndex, 0)
    }

    open fun getSystemName(systemIndex: Int): String {
        return getValueAt(systemIndex, 1) as String
    }

    open fun getSystemState(systemIndex: Int): Boolean {
        return getValueAt(systemIndex, 0) as Boolean
    }

    open fun clear() {
        rowCount = 0
    }
}
