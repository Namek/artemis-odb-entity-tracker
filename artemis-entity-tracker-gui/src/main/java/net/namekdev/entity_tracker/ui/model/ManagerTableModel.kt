package net.namekdev.entity_tracker.ui.model

class ManagerTableModel : BaseSystemTableModel() {
    init {
        columnIdentifiers[1] = "manager"
    }

    fun addManager(name: String) {
        addRow(arrayOf(true, name))
    }

    fun getManagerName(index: Int): String {
        return getValueAt(index, 1) as String
    }

    fun getManagerState(index: Int): Boolean {
        return getValueAt(index, 0) as Boolean
    }

    override fun clear() {
        rowCount = 0
    }
}