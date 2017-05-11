package net.namekdev.entity_tracker.utils.serialization

import com.artemis.utils.BitVector

abstract class NetworkSerialization {
    enum class DataType {
        Undefined,

        // meta data
        Description,
        MultipleDescriptions,
        EnumDescription,  //defines a list of possible values in enum
        EnumValue,

        // simple timpes
        Byte,
        Short,
        Int,
        Long,
        String,
        Boolean, //takes 1 byte
        Float,
        Double,
        BitVector, //takes minimum 4 bytes, BitVector is a type from artemis-odb

        // more complicated data types
        Unknown,
        Object,
        Array,
        Enum,

        // special values
        Null, //takes 1 byte
        DescriptionRef,
    }


    companion object {
        fun createSerializer(): NetworkSerializer {
            return NetworkSerializer()
        }

        fun createDeserializer(): NetworkDeserializer {
            return NetworkDeserializer()
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
