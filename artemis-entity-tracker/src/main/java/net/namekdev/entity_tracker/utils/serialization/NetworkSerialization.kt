package net.namekdev.entity_tracker.utils.serialization

import com.artemis.utils.BitVector

abstract class NetworkSerialization {
    enum class Type {
        Unknown,
        Null,
        Array,
        Description,
        MultipleDescriptions,
        DescriptionRef,
        Object,
        Enum,
        EnumDescription,
        EnumValue,

        Byte,
        Short,
        Int,
        Long,
        String,
        Boolean, //takes 1 byte
        Float,
        Double,
        BitVector //takes minimum 4 bytes
    }

    companion object {
        fun createSerializer(): NetworkSerializer {
            return NetworkSerializer()
        }

        fun createDeserializer(): NetworkDeserializer {
            return NetworkDeserializer()
        }

        fun determineType(type: Class<*>): Type {
            var netType = Type.Unknown

            if (type == Byte::class.javaPrimitiveType || type == Byte::class.javaObjectType)
                netType = Type.Byte
            else if (type == Short::class.javaPrimitiveType || type == Short::class.javaObjectType)
                netType = Type.Short
            else if (type == Int::class.javaPrimitiveType || type == Int::class.javaObjectType)
                netType = Type.Int
            else if (type == Long::class.javaPrimitiveType || type == Long::class.javaObjectType)
                netType = Type.Long
            else if (type == String::class.java)
                netType = Type.String
            else if (type == Boolean::class.javaPrimitiveType || type == Boolean::class.javaObjectType)
                netType = Type.Boolean
            else if (type == Float::class.javaPrimitiveType || type == Float::class.javaObjectType)
                netType = Type.Float
            else if (type == Double::class.javaPrimitiveType || type == Double::class.javaObjectType)
                netType = Type.Double
            else if (type == BitVector::class.java)
                netType = Type.BitVector
            else if (type.isEnum)
                netType = Type.Enum

            return netType
        }

        fun convertStringToTypedValue(value: String, valueType: Type): Any? {
            when (valueType) {
                NetworkSerialization.Type.Byte -> return java.lang.Byte.valueOf(value)
                NetworkSerialization.Type.Short -> return java.lang.Short.valueOf(value)
                NetworkSerialization.Type.Int -> return Integer.valueOf(value)
                NetworkSerialization.Type.Long -> return java.lang.Long.valueOf(value)
                NetworkSerialization.Type.String -> return value
                NetworkSerialization.Type.Boolean -> return java.lang.Boolean.valueOf(value)
                NetworkSerialization.Type.Float -> return java.lang.Float.valueOf(value)
                NetworkSerialization.Type.Double -> return java.lang.Double.valueOf(value)
                NetworkSerialization.Type.Enum -> throw UnsupportedOperationException("probably unsupported, not sure")
                NetworkSerialization.Type.BitVector -> return BitVector(Integer.valueOf(value)!!)
                NetworkSerialization.Type.Array -> throw UnsupportedOperationException("arrays are not supported (yet?)")
                else -> return null
            }
        }

        fun isSimpleType(valueType: Type?): Boolean {
            if (valueType == null)
                return false

            when (valueType) {
                NetworkSerialization.Type.Byte -> return true
                NetworkSerialization.Type.Short -> return true
                NetworkSerialization.Type.Int -> return true
                NetworkSerialization.Type.Long -> return true
                NetworkSerialization.Type.String -> return true
                NetworkSerialization.Type.Boolean -> return true
                NetworkSerialization.Type.Float -> return true
                NetworkSerialization.Type.Double -> return true
                NetworkSerialization.Type.BitVector -> return true
                else -> return false
            }
        }
    }
}
