package net.namekdev.entity_tracker.utils.serialization

class ValueTree {
    var id: Short = -1
    val values: Array<Any?>
    var model: ObjectModelNode? = null
    var parent: ValueTree? = null

    constructor(length: Int) {
        values = arrayOfNulls<Any>(length)
    }

    constructor(values: Array<Any?>) {
        this.values = values
    }
}
