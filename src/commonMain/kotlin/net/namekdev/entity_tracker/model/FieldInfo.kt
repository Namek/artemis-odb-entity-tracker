package net.namekdev.entity_tracker.model

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.DataType
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode


data class FieldInfo(
    /** Only available on server side.  */
    var field: Any? = null, // type: com.artemis.utils.reflect.Field

    var isAccessible: Boolean = false,
    var fieldName: String,
    var classType: String,
    var isArray: Boolean = false,
    var valueType: DataType,
    var isPrimitiveType: Boolean = false,

    /** Available when type of field is not a simple type or array.  */
    var treeDesc: ObjectModelNode? = null
)
