package net.namekdev.entity_tracker.model

import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode

open class ComponentTypeInfo(var name: String) {
    var index: Int = 0
    lateinit var model: ObjectModelNode
}
