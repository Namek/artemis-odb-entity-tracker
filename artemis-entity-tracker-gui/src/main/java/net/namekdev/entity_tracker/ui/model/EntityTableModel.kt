package net.namekdev.entity_tracker.ui.model

import java.util.HashMap
import java.util.Vector

import javax.swing.table.DefaultTableModel

import com.artemis.utils.BitVector
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.utils.AutoSizedArray as AutoSizedArray


class EntityTableModel : DefaultTableModel(arrayOf<Array<Any>>(), arrayOf<Any>("  entity id  " as Any)) {
    private val _entityComponents = HashMap<Int, BitVector>()
    private val _componentTypes = AutoSizedArray<ComponentTypeInfo>(50)


    fun setComponentType(index: Int, info: ComponentTypeInfo) {
        _componentTypes.set(index, info)

        for (i in columnCount..index + 1) {
            addColumn("")
        }

        columnIdentifiers[index + 1] = info.name
        fireTableStructureChanged()
    }

    fun addEntity(entityId: Int, components: BitVector) {
        // TODO check if bitset isn't greater than before model header columns

        val row = Vector<Any>(components.length() + 1)
        row.add(entityId)

        var i = 0
        val n = components.length()
        while (i < n) {
            row.add(components.get(i))
            ++i
        }

        this.addRow(row)
        _entityComponents.put(entityId, components)
    }

    fun removeEntity(entityId: Int) {
        var i = 0
        val n = rowCount
        while (i < n) {
            val `val` = getValueAt(i, 0) as Int

            if (`val` === entityId) {
                removeRow(i)
                break
            }
            ++i
        }
        _entityComponents.remove(entityId)
    }

    fun getEntityComponents(entityId: Int): BitVector {
        return _entityComponents[entityId]!!
    }

    fun getComponentTypeInfo(index: Int): ComponentTypeInfo {
        return _componentTypes.get(index)
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return if (columnIndex == 0) Int::class.java else Boolean::class.java
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return false
    }

    fun clear() {
        _componentTypes.clear()
        _entityComponents.clear()
        rowCount = 0

        columnIdentifiers.setSize(1)
        fireTableStructureChanged()
    }
}
