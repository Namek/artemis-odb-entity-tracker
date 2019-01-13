package net.namekdev.entity_tracker.connectors

interface IWorldController {
    fun setSystemState(name: String, isOn: Boolean)
    fun setManagerState(name: String, isOn: Boolean)
    fun requestComponentState(entityId: Int, componentIndex: Int)
    fun setComponentFieldValue(entityId: Int, componentIndex: Int, treePath: IntArray, newValue: Any?)
}

interface IWorldControlListener {
    fun onComponentFieldValueChanged(entityId: Int, componentIndex: Int, treePath: IntArray, newValue: Any?)
}
