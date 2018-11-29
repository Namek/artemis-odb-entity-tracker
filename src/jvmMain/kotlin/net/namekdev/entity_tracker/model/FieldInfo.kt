package net.namekdev.entity_tracker.model

import com.artemis.utils.BitVector
import com.artemis.utils.reflect.Field
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization

fun reflectField(field: Field): FieldInfo {
    val type = field.type
    val (dataType, isPrimitiveType) = determineType(type)

    return FieldInfo(
        field,
        field.isAccessible,
        field.name,
        type.simpleName,
        type.isArray,
        dataType,
        isPrimitiveType
    )
}

/**
 * @return pair of dataType + isTypePrimitive
 */
fun determineType(type: Class<*>): Pair<NetworkSerialization.DataType, Boolean> {
    return when (type) {
        Byte::class.javaPrimitiveType -> Pair(NetworkSerialization.DataType.Byte, true)
        Byte::class.javaObjectType -> Pair(NetworkSerialization.DataType.Byte, false)
        Short::class.javaPrimitiveType -> Pair(NetworkSerialization.DataType.Short, true)
        Short::class.javaObjectType -> Pair(NetworkSerialization.DataType.Short, false)
        Int::class.javaPrimitiveType -> Pair(NetworkSerialization.DataType.Int, true)
        Int::class.javaObjectType -> Pair(NetworkSerialization.DataType.Int, false)
        Long::class.javaPrimitiveType -> Pair(NetworkSerialization.DataType.Long, true)
        Long::class.javaObjectType -> Pair(NetworkSerialization.DataType.Long, false)
        String::class.java -> Pair(NetworkSerialization.DataType.String, false)
        Boolean::class.javaPrimitiveType -> Pair(NetworkSerialization.DataType.Boolean, true)
        Boolean::class.javaObjectType -> Pair(NetworkSerialization.DataType.Boolean, false)
        Float::class.javaPrimitiveType -> Pair(NetworkSerialization.DataType.Float, true)
        Float::class.javaObjectType -> Pair(NetworkSerialization.DataType.Float, false)
        Double::class.javaPrimitiveType -> Pair(NetworkSerialization.DataType.Double, true)
        Double::class.javaObjectType -> Pair(NetworkSerialization.DataType.Double, false)
        BitVector::class.java -> Pair(NetworkSerialization.DataType.BitVector, false)
        else ->
            if (type.isEnum)
                Pair(NetworkSerialization.DataType.Enum, false)
            else
                Pair(NetworkSerialization.DataType.Unknown, false)
    }
}


fun convertStringToTypedValue(value: String, valueType: NetworkSerialization.DataType): Any? {
    when (valueType) {
        NetworkSerialization.DataType.Byte -> return java.lang.Byte.valueOf(value)
        NetworkSerialization.DataType.Short -> return java.lang.Short.valueOf(value)
        NetworkSerialization.DataType.Int -> return Integer.valueOf(value)
        NetworkSerialization.DataType.Long -> return java.lang.Long.valueOf(value)
        NetworkSerialization.DataType.String -> return value
        NetworkSerialization.DataType.Boolean -> return java.lang.Boolean.valueOf(value)
        NetworkSerialization.DataType.Float -> return java.lang.Float.valueOf(value)
        NetworkSerialization.DataType.Double -> return java.lang.Double.valueOf(value)
        NetworkSerialization.DataType.Enum -> throw UnsupportedOperationException("probably unsupported, not sure")
        NetworkSerialization.DataType.BitVector -> return BitVector(Integer.valueOf(value)!!)
        else -> return null
    }
}