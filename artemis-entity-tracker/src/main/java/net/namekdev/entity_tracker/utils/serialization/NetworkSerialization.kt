package net.namekdev.entity_tracker.utils.serialization

import com.artemis.utils.BitVector

abstract class NetworkSerialization {
    enum class MetaType {
        Description,
        MultipleDescriptions,
        DescriptionRef,
        EnumDescription,
        EnumValue,
    }

    enum class DataType {
        Unknown,
        Object,
        Array,
        Enum,

        Byte,
        Short,
        Int,
        Long,
        String,
        Boolean, //takes 1 byte
        Float,
        Double,
        BitVector //takes minimum 4 bytes, BitVector is a type from artemis-odb
    }

    enum class SpecialValue {
        Null, //takes 1 byte
    }

    companion object {
        fun createSerializer(): NetworkSerializer {
            return NetworkSerializer()
        }

        fun createDeserializer(): NetworkDeserializer {
            return NetworkDeserializer()
        }

        fun determineType(type: Class<*>): DataType {
            return when (type) {
                Byte::class.javaPrimitiveType, Byte::class.javaObjectType -> DataType.Byte
                Short::class.javaPrimitiveType, Short::class.javaObjectType -> DataType.Short
                Int::class.javaPrimitiveType, Int::class.javaObjectType -> DataType.Int
                Long::class.javaPrimitiveType, Long::class.javaObjectType -> DataType.Long
                String::class.java -> DataType.String
                Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> DataType.Boolean
                Float::class.javaPrimitiveType, Float::class.javaObjectType -> DataType.Float
                Double::class.javaPrimitiveType, Double::class.javaObjectType -> DataType.Double
                BitVector::class.java -> DataType.BitVector
                else -> if (type.isEnum) DataType.Enum else DataType.Unknown
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

        fun isSimpleType(valueType: DataType?): Boolean {
            if (valueType == null)
                return false

            when (valueType) {
                DataType.Byte -> return true
                DataType.Short -> return true
                DataType.Int -> return true
                DataType.Long -> return true
                DataType.String -> return true
                DataType.Boolean -> return true
                DataType.Float -> return true
                DataType.Double -> return true
                DataType.BitVector -> return true
                else -> return false
            }
        }
    }
}
