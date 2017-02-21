package net.namekdev.entity_tracker.utils.serialization

class ValueTree(length: Int) {
    val values: Array<Any?>
    var model: ObjectModelNode? = null
    var parent: ValueTree? = null

    init {
        values = arrayOfNulls<Any>(length)
    }
}
