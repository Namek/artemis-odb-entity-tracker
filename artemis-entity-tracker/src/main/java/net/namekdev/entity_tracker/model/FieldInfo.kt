package net.namekdev.entity_tracker.model

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Type
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode

import com.artemis.utils.reflect.Field

data class FieldInfo(
    /** Only available on server side.  */
    var field: Field? = null,

    var isAccessible: Boolean = false,
    var fieldName: String,
    var classType: String,
    var isArray: Boolean = false,
    var valueType: Type,

    /** Available when type of field is not a simple type or array.  */
    var treeDesc: ObjectModelNode? = null
) {
    companion object {
        fun reflectField(field: Field): FieldInfo {
            val type = field.type

            return FieldInfo(
                field,
                field.isAccessible,
                field.name,
                type.simpleName,
                type.isArray,
                NetworkSerialization.determineType(type)
            )
        }
    }
}
