package net.namekdev.entity_tracker.utils.serialization

import kotlin.reflect.KClass

interface ObjectModelsCollection {
    fun add(model: ObjectModelNode)
    operator fun get(index: Int): ObjectModelNode
    fun getById(id: Int): ObjectModelNode?
    fun size(): Int
}
