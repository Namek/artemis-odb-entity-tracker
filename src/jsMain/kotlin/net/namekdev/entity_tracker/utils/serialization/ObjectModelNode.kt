package net.namekdev.entity_tracker.utils.serialization

actual fun ObjectModelNode.setValue(targetObj: Any, treePath: IntArray?, value: Any?) {
    throw NotImplementedError("it won't be! for JS.")
}