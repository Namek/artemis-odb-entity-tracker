package net.namekdev.entity_tracker.utils.serialization

abstract class NetworkSerialization {
    enum class DataType {
        Undefined,

        // meta data
        Description,
        DescriptionRef,
        MultipleDescriptions,
        EnumDescription,  //defines a list of possible values in enum
        EnumValue,

        // simple types
        Byte,
        Short,
        Int,
        Long,
        String,
        Boolean, //takes 1 byte
        Float,
        Double,
        BitVector, //takes minimum 4 bytes, CommonBitVector is a type from artemis-odb

        // more complicated data types
        Unknown,
        Object,
        ObjectRef, // a reference by id
        Array,
        Enum,

        // special values
        Null, //takes 1 byte
    }


    companion object {
//        fun createSerializer(): NetworkSerializer {
//            return NetworkSerializer()
//        }
//
//        fun createDeserializer(): NetworkDeserializer {
//            return NetworkDeserializer()
//        }

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
