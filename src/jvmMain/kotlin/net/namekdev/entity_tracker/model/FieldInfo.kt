package net.namekdev.entity_tracker.model

import com.artemis.utils.BitVector
import com.artemis.utils.reflect.Field
import net.namekdev.entity_tracker.utils.serialization.DataType

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
fun determineType(type: Class<*>): Pair<DataType, Boolean> {
    return when (type) {
        Byte::class.javaPrimitiveType -> Pair(DataType.Byte, true)
        Byte::class.javaObjectType -> Pair(DataType.Byte, false)
        Short::class.javaPrimitiveType -> Pair(DataType.Short, true)
        Short::class.javaObjectType -> Pair(DataType.Short, false)
        Int::class.javaPrimitiveType -> Pair(DataType.Int, true)
        Int::class.javaObjectType -> Pair(DataType.Int, false)
        Long::class.javaPrimitiveType -> Pair(DataType.Long, true)
        Long::class.javaObjectType -> Pair(DataType.Long, false)
        String::class.java -> Pair(DataType.String, false)
        Boolean::class.javaPrimitiveType -> Pair(DataType.Boolean, true)
        Boolean::class.javaObjectType -> Pair(DataType.Boolean, false)
        Float::class.javaPrimitiveType -> Pair(DataType.Float, true)
        Float::class.javaObjectType -> Pair(DataType.Float, false)
        Double::class.javaPrimitiveType -> Pair(DataType.Double, true)
        Double::class.javaObjectType -> Pair(DataType.Double, false)
        BitVector::class.java -> Pair(DataType.BitVector, false)
        else ->
            if (type.isEnum)
                Pair(DataType.Enum, false)
            else
                Pair(DataType.Unknown, false)
    }
}


fun convertStringToTypedValue(value: String, valueType: DataType): Any? {
    when (valueType) {
        DataType.Byte -> return java.lang.Byte.valueOf(value)
        DataType.Short -> return java.lang.Short.valueOf(value)
        DataType.Int -> return Integer.valueOf(value)
        DataType.Long -> return java.lang.Long.valueOf(value)
        DataType.String -> return value
        DataType.Boolean -> return java.lang.Boolean.valueOf(value)
        DataType.Float -> return java.lang.Float.valueOf(value)
        DataType.Double -> return java.lang.Double.valueOf(value)
        DataType.Enum -> throw UnsupportedOperationException("probably unsupported, not sure")
        DataType.BitVector -> return BitVector(Integer.valueOf(value)!!)
        else -> return null
    }
}