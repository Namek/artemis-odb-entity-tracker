package net.namekdev.entity_tracker.model

import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode


class ComponentTypeInfo {
    /** Only available on server side.  */
    var type: Class<*>? = null

    var name: String
    var index: Int = 0
    lateinit var model: ObjectModelNode


    constructor(name: String) {
        this.name = name
    }

    constructor(type: Class<*>) {
        this.type = type
        this.name = type.simpleName
    }
}
