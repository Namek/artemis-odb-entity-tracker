package net.namekdev.entity_tracker.ui.model

import javax.swing.event.TableColumnModelEvent
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.TableColumn


class ComponentColumnModel : DefaultTableColumnModel() {

    protected var _currentOrder = ORDER_ASC
    protected var wasOrderManuallyChanged = false

    /**
     * Sets order type and sorts entities due to this order type.

     * @param orderType [.ORDER_ASC], [.ORDER_DESC] or [.ORDER_MODEL].
     */
    fun setCurrentOrder(orderType: Int) {
        _currentOrder = orderType
        sortColumns()
    }

    /**
     * Get current order type.

     * @see .setCurrentOrder
     */
    fun getCurrentOrder(): Int {
        return _currentOrder
    }

    val currentOrderName: String?
        get() {
            val o = getCurrentOrder()
            when (o) {
                ORDER_ASC -> return "ASC"
                ORDER_DESC -> return "DESC"
                ORDER_MODEL -> return "MODEL"
            }

            return null
        }

    /**
     * Adds column as always and runs column sorting for it.
     */
    override fun addColumn(aColumn: TableColumn) {
        super.addColumn(aColumn)
        sortColumns()
    }

    fun toggleOrdering() {
        if (!wasOrderManuallyChanged) {
            var order = getCurrentOrder() + 1
            if (order > ORDER_LAST) {
                order = ORDER_FIRST
            }
            setCurrentOrder(order)
        }

        sortColumns()
    }

    override fun fireColumnMoved(evt: TableColumnModelEvent) {
        if (evt.fromIndex != evt.toIndex) {
            wasOrderManuallyChanged = true
        }

        super.fireColumnMoved(evt)
    }

    private fun sortColumns() {
        val n = columnCount

        // start at i=1, because i=0 is "entity id" column, which should not be moved
        for (i in 1..n - 1) {
            var mostLeftColIndex = i
            val col1 = getColumn(i)
            var mostLeftColName = col1.headerValue.toString()

            for (j in i + 1..n - 1) {
                val col2 = getColumn(j)
                val col2Name = col2.headerValue.toString()

                var performSwitch = false

                if (_currentOrder == ORDER_MODEL) {
                    performSwitch = col2.modelIndex < col1.modelIndex
                }
                else {
                    val cmp = mostLeftColName.compareTo(col2Name)
                    performSwitch = _currentOrder == ORDER_DESC && cmp < 0 || _currentOrder == ORDER_ASC && cmp > 0
                }

                if (performSwitch) {
                    mostLeftColIndex = j
                    mostLeftColName = col2Name
                }
            }

            if (mostLeftColIndex != i) {
                switchColumns(mostLeftColIndex, i)
            }
        }

        wasOrderManuallyChanged = false
    }

    protected fun switchColumns(index1: Int, index2: Int) {
        val col1 = tableColumns.elementAt(index1)
        val col2 = tableColumns.elementAt(index2)

        tableColumns[index1] = col2
        tableColumns[index2] = col1

        val evt = TableColumnModelEvent(this, index1, index2)
        super.fireColumnMoved(evt)
    }

    companion object {
        val ORDER_ASC = 1
        val ORDER_DESC = 2
        val ORDER_MODEL = 3
        protected val ORDER_FIRST = ORDER_ASC
        protected val ORDER_LAST = ORDER_MODEL
    }
}
