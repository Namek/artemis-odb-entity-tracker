package net.namekdev.entity_tracker.utils.serialization

import kotlin.reflect.KClass


interface ObjectModelsCollection_Typed : ObjectModelsCollection {
    operator fun get(type: KClass<*>): ObjectModelNode
    fun getTypeByModelId(id: Int): Class<*>?
}