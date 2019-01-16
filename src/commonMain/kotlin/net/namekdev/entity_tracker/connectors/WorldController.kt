package net.namekdev.entity_tracker.connectors

import net.namekdev.entity_tracker.utils.serialization.DataType

interface IWorldController {
    fun setSystemState(name: String, isOn: Boolean)
    fun setManagerState(name: String, isOn: Boolean)
    fun requestComponentState(entityId: Int, componentIndex: Int)
    fun setComponentFieldValue(entityId: Int, componentIndex: Int, treePath: IntArray, newValueType: DataType, newValue: Any?)
}

interface IWorldControlListener {
    fun onComponentFieldValueChanged(entityId: Int, componentIndex: Int, treePath: IntArray, newValue: Any?)
}
