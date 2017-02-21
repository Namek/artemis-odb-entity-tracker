package net.namekdev.entity_tracker.utils.serialization

interface ObjectModelsCollection {
    fun add(model: ObjectModelNode)
    operator fun get(type: Class<*>): ObjectModelNode
    operator fun get(index: Int): ObjectModelNode
    fun getById(id: Int): ObjectModelNode?
    fun size(): Int
}
