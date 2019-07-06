package net.namekdev.entity_tracker.utils.serialization


import net.namekdev.entity_tracker.utils.assert
import net.namekdev.entity_tracker.utils.doubleToLongBits
import net.namekdev.entity_tracker.utils.floatToIntBits

class NetworkSerializerClientState {
    internal val modelsMarkedAsSent = mutableSetOf<Int>()
    internal var packetBeingConstructed: Boolean = false
}

/**
 * This network serializer may seem indeterminant, however it is not the case.
 * When given object is being serialized through `addObject()` call or any
 * descendant field is of a custom type (class) then the definitions are implicitly
 * added to serialization buffer.
 */
abstract class NetworkSerializer<Self : NetworkSerializer<Self, BitVectorType>, BitVectorType : Any> : NetworkSerialization() {
    private var _buffer: ByteArray = ByteArray(102400)
    private var _pos: Int = 0
    private var _clientState: NetworkSerializerClientState? = null
    private var _defaultClientState = NetworkSerializerClientState()
    private val _serializeResult = SerializationResult()


    abstract fun isBitVector(obj: Any): Boolean
    abstract fun addBitVector(bitVector: BitVectorType): Self


    fun beginPacket(clientState: NetworkSerializerClientState? = null): Self {
        assert(_clientState == null)
        assert(!(clientState?.packetBeingConstructed ?: false))

        _clientState = clientState ?: _defaultClientState
        _clientState?.let {
            it.packetBeingConstructed = true
        }

        return this as Self
    }

    fun endPacket(): SerializationResult {
        assert(_clientState?.packetBeingConstructed ?: (_pos > 0))

        _serializeResult.setup(_buffer, _pos)

        _clientState?.let {
            it.packetBeingConstructed = false
        }
        _clientState = null
        _pos = 0

        return _serializeResult
    }

    fun beginArray(elementType: DataType, length: Int, isPrimitive: Boolean): Self {
        addType(DataType.Array)
        addRawBoolean(isPrimitive)
        addType(elementType)
        addRawInt(length)

        return this as Self
    }

    fun beginArray(length: Int): Self {
        return beginArray(DataType.Unknown, length, false)
    }

    fun addType(type: DataType): Self {
        dbgType(type)
        return addRawByte(type.ordinal.toByte())
    }

    fun addByte(value: Byte): Self {
        addType(DataType.Byte)
        _buffer[_pos++] = value
        return this as Self
    }

    fun addRawByte(value: Byte): Self {
        _buffer[_pos++] = value
        return this as Self
    }

    fun addShort(value: Short): Self {
        addType(DataType.Short)
        addRawShort(value)
        return this as Self
    }

    protected fun addRawShort(value: Short) {
        _buffer[_pos++] = (value.toInt() shr 8 and 0xFF).toByte()
        _buffer[_pos++] = (value.toInt() and 0xFF).toByte()
    }

    fun addInt(value: Int): Self {
        addType(DataType.Int)
        addRawInt(value)
        return this as Self
    }

    fun addRawInt(value: Int) {
        _buffer[_pos++] = (value shr 24 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 16 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 8 and 0xFF).toByte()
        _buffer[_pos++] = (value and 0xFF).toByte()
    }

    fun addLong(value: Long): Self {
        addType(DataType.Long)
        addRawLong(value)
        return this as Self
    }

    protected fun addRawLong(value: Long) {
        _buffer[_pos++] = (value shr 56 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 48 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 40 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 32 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 24 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 16 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 8 and 0xFF).toByte()
        _buffer[_pos++] = (value and 0xFF).toByte()
    }

    fun addString(value: String?): Self {
        if (tryAddNullable(value)) {
            return this as Self
        }

        addType(DataType.String)

        val n = value!!.length
        addRawInt(n)

        for (i in 0..n - 1) {
            _buffer[_pos++] = (value!![i].toInt() and 0xFF).toByte()
        }

        return this as Self
    }

    fun addBoolean(value: Boolean): Self {
        addType(DataType.Boolean)
        return addRawBoolean(value)
    }

    fun addRawBoolean(value: Boolean): Self {
        return addRawByte((if (value) 1 else 0).toByte())
    }

    fun addFloat(value: Float): Self {
        addType(DataType.Float)
        return addRawFloat(value)
    }

    fun addRawFloat(value: Float): Self {
        addRawInt(Float.floatToIntBits(value))
        return this as Self
    }

    fun addDouble(value: Double): Self {
        addType(DataType.Double)
        return addRawDouble(value)
    }

    fun addRawDouble(value: Double): Self {
        addRawLong(Double.doubleToLongBits(value))
        return this as Self
    }

    fun addBitVectorOrNull(bitVector: BitVectorType?): Self {
        if (tryAddNullable(bitVector)) {
            return this as Self
        }

        return addBitVector(bitVector!!)
    }

    protected fun tryAddNullable(data: Any?): Boolean {
        if (data == null) {
            addType(DataType.Null)
            return true
        }

        return false
    }

    fun addSimpleTypeValue(valueType: DataType, value: Any?): Self {
        assert(valueType.isSimpleType)

        if (value == null)
            addType(DataType.Null)
        else if (valueType.isSimpleType) {
            addType(valueType)
            addRawByType(valueType, value)
        }
        else {
            throw IllegalArgumentException("Can't serialize type: " + value::class)
        }

        return this as Self
    }

    fun addFlatByType(valueType: DataType, value: Any?): Self {
        if (valueType.isSimpleType)
            addSimpleTypeValue(valueType, value)
        else if (valueType == DataType.Enum) {
            if (value is Int) {
                addType(DataType.Enum)
                addRawInt(value)
            }
            else if (value == null) {
                addType(DataType.Null)
            }
            else TODO()
        }
        else TODO()

        return this as Self
    }

    fun addRawByType(valueType: DataType, value: Any): Self {
        when (valueType) {
            DataType.Byte -> addRawByte(value as Byte)
            DataType.Short -> addRawShort(value as Short)
            DataType.Int -> addRawInt(value as Int)
            DataType.Long -> addRawLong(value as Long)
            DataType.String -> addString(value as String)
            DataType.Boolean -> addRawBoolean(value as Boolean)
            DataType.Float -> addRawFloat(value as Float)
            DataType.Double -> addRawDouble(value as Double)
            DataType.BitVector -> addBitVector(value as BitVectorType)

            else -> throw RuntimeException("type not supported: " + valueType)
        }

        return this as Self
    }

    fun addDataDescriptionOrRef(model: ObjectModelNode): Self {
        if (!_clientState!!.modelsMarkedAsSent.contains(model.id)) {
            _clientState!!.modelsMarkedAsSent.add(model.id)

            addType(DataType.Description)
            addRawDataDescription(model)
        }
        else {
            addType(DataType.DescriptionRef)
            addRawInt(model.id)
        }

        return this as Self
    }

    private fun addRawDataDescription(model: ObjectModelNode) {
        addRawInt(model.id)
        addString(model.name)
        addBoolean(model.isTypePrimitive)

        if (model.dataType == DataType.Object || model.dataType == DataType.Unknown) {
            addType(DataType.Object)
            val n = model.children!!.size
            addRawInt(n)

            for (i in 0..n - 1) {
                val node = model.children!![i]
                addDataDescriptionOrRef(node)
            }
        }
        else if (model.dataType.isSimpleType) {
            addType(model.dataType)
        }
        else if (model.isEnum) {
            addType(DataType.Enum)
            addDataDescriptionOrRef(model.enumModel())
        }
        else if (model.dataType == DataType.EnumValue) {
            addType(DataType.EnumValue)
            addRawInt(model.enumValue)
            addString(model.name!!)
        }
        else if (model.dataType == DataType.EnumDescription) {
            addType(DataType.EnumDescription)
            addRawInt(model.children!!.size)

            for (enumValueModel in model.children!!) {
                addRawInt(enumValueModel.id)
                addRawInt(enumValueModel.enumValue)
                addString(enumValueModel.name!!)
            }
        }
        else if (model.isArray) {
            val arrayType = model.arrayType()
            addType(DataType.Array)
            addType(if (arrayType == DataType.Unknown) DataType.Object else arrayType)

            if (arrayType.isSimpleType) {
                addBoolean(model.isSubTypePrimitive)
            }
            else if (arrayType == DataType.Object || arrayType == DataType.Unknown) {
                // do nothing
            }
            else if (arrayType == DataType.Enum) {
                val enumModel = model.arrayElTypeModel()
                addDataDescriptionOrRef(enumModel)
            }
            else if (arrayType == DataType.Array) {
                var m = model
                var depth = 0
                while (m.children != null) {
                    m = m.children!![0]
                    ++depth
                }
                addRawInt(depth)
                addType(m.dataSubType)
                addBoolean(m.isSubTypePrimitive)

                m = model
                while (m.children != null) {
                    m = m.children!![0]
                    addRawInt(m.id)
                }
            }
            else {
                throw RuntimeException("unsupported array type: " + arrayType)
            }
        }
        else {
            throw RuntimeException("unsupported type: " + model.dataType)
        }
    }


    fun addArray(array: BooleanArray): Self {
        beginArray(DataType.Boolean, array.size, true)

        // TODO may be optimized by using CommonBitVector or similar
        for (value in array) {
            addRawBoolean(value)
        }

        return this as Self
    }

    fun addArray(array: ByteArray): Self {
        beginArray(DataType.Byte, array.size, true)

        for (value in array) {
            addRawByte(value)
        }

        return this as Self
    }

    fun addArray(array: ShortArray): Self {
        beginArray(DataType.Short, array.size, true)

        for (value in array) {
            addRawShort(value)
        }

        return this as Self
    }

    fun addArray(array: IntArray): Self {
        beginArray(DataType.Int, array.size, true)

        for (value in array) {
            addRawInt(value)
        }

        return this as Self
    }

    fun addArray(array: LongArray): Self {
        beginArray(DataType.Long, array.size, true)

        for (value in array) {
            addRawLong(value)
        }

        return this as Self
    }

    fun addArray(array: FloatArray): Self {
        beginArray(DataType.Float, array.size, true)

        for (value in array) {
            addRawFloat(value)
        }

        return this as Self
    }

    fun addArray(array: DoubleArray): Self {
        beginArray(DataType.Double, array.size, true)

        for (value in array) {
            addRawDouble(value)
        }

        return this as Self
    }



    private inline fun dbgType(t: DataType) {
        //println(t)
    }


    class SerializationResult {
        lateinit var buffer: ByteArray
        var size: Int = 0

        constructor() {}

        constructor(buffer: ByteArray, size: Int) {
            this.buffer = buffer
            this.size = size
        }

        fun setup(buffer: ByteArray, size: Int): SerializationResult {
            this.buffer = buffer
            this.size = size
            return this
        }
    }
}

internal class ObjectSerializationSession {
    val objs = ArrayList<ObjectContainer>()
    val objsMap = mutableMapOf<Int, ObjectContainer>()
    var lastId = 0.toShort()

    fun hasOrRemember(obj: Any): Pair<Boolean, ObjectContainer> {
        val hashCode: Int = obj.hashCode()
        var container = objsMap.get(hashCode)

        if (container == null) {
            container = objs.find { it.obj == obj }
        }

        val found = container != null

        if (container == null) {
            container = ObjectContainer(obj, createId())
            objs.add(container)
            objsMap.put(hashCode, container)
        }

        return Pair(found, container)
    }

    private fun createId(): Short {
        return ++lastId
    }
}

data class ObjectContainer(val obj: Any, val id: Short) { }