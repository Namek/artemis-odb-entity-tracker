package net.namekdev.entity_tracker.utils.serialization

abstract class NetworkSerialization {}

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
    BitVector, //takes minimum 4 bytes, BitVector is a type from artemis-odb

    // more complicated data types
    Unknown,
    Object,
    ObjectRef, // a reference by id
    Array,
    Enum,

    // special values
    Null; //takes 1 byte

    /**
     * "Simple" type is a type which does not need a description.
     */
    val isSimpleType: kotlin.Boolean
        get() = when (this) {
            DataType.Byte,
            DataType.Short,
            DataType.Int,
            DataType.Long,
            DataType.String,
            DataType.Boolean,
            DataType.Float,
            DataType.Double,
            DataType.BitVector ->
                true

            else -> false
        }

    val isLanguageType: kotlin.Boolean
        get() = when(this) {
            DataType.Boolean,
            DataType.Byte,
            DataType.Short,
            DataType.Int,
            DataType.Long,
            DataType.String,
            DataType.Float,
            DataType.Double ->
                true

            else -> false
        }
}
