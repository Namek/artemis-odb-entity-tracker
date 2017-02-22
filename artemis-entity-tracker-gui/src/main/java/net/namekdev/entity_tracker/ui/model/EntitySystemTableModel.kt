package net.namekdev.entity_tracker.ui.model

import net.namekdev.entity_tracker.utils.IndexBiMap

class EntitySystemTableModel : BaseSystemTableModel() {
    protected var _systemIndexMap = IndexBiMap(100)
    protected var _entitySystemsCount = 0

    init {
        addColumn("entities")
        addColumn("max entities")
    }


    override fun getColumnClass(columnIndex: Int): Class<*>? {
        when (columnIndex) {
            2, 3 -> return Int::class.javaObjectType
            else -> return super.getColumnClass(columnIndex)
        }
    }

    override fun getSystemIndex(rowIndex: Int): Int {
        return _systemIndexMap.getGlobalIndex(rowIndex)
    }

    override fun setSystem(systemIndex: Int, name: String) {
        // we assume here that this system is called
        // by order of system indices.

        _systemIndexMap.ensureSize(systemIndex + 1)
        val rowIndex = _entitySystemsCount++
        _systemIndexMap.set(rowIndex, systemIndex)

        super.setSystem(rowIndex, name)
        setValueAt(0, rowIndex, 2)
        setValueAt(0, rowIndex, 3)
    }

    /**
     * Update system state without firing events.
     */
    override fun updateSystemState(systemIndex: Int, enabled: Boolean) {
        val rowIndex = _systemIndexMap.getLocalIndex(systemIndex)

        if (rowIndex >= 0) {
            updateValueAt(enabled, rowIndex, 0)
        }
    }

    fun updateSystem(systemIndex: Int, entitiesCount: Int, maxEntitiesCount: Int) {
        val rowIndex = _systemIndexMap.getLocalIndex(systemIndex)
        setValueAt(entitiesCount, rowIndex, 2)
        setValueAt(maxEntitiesCount, rowIndex, 3)
    }

    override fun getSystemName(systemIndex: Int): String {
        val rowIndex = _systemIndexMap.getLocalIndex(systemIndex)
        return super.getSystemName(rowIndex)
    }

    override fun getSystemState(systemIndex: Int): Boolean {
        val rowIndex = _systemIndexMap.getLocalIndex(systemIndex)
        return super.getSystemState(rowIndex)
    }
}
