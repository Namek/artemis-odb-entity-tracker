package net.namekdev.entity_tracker.utils.serialization

class ValueTree(length: Int) {
    val values: Array<Any?> = arrayOfNulls<Any>(length)
    var model: ObjectModelNode? = null
    var parent: ValueTree? = null
}
