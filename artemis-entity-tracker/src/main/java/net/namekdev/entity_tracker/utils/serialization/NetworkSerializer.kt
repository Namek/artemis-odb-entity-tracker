package net.namekdev.entity_tracker.utils.serialization

import java.util.TreeSet

import com.artemis.utils.BitVector

import net.namekdev.entity_tracker.utils.ReflectionUtils

/**
 * This network serializer may seem indeterministic however it is not the case.
 * When given object is being serialized through `addObject()` call or any
 * descendant field is of custom type (class) then the definitions are implicitily
 * added to serialization buffer.
 */
class NetworkSerializer @JvmOverloads constructor(val inspector: ObjectTypeInspector = ObjectTypeInspector()) : NetworkSerialization() {
    private val _ourBuffer: ByteArray
    private var _buffer: ByteArray
    private var _pos: Int = 0

    private val _serializeResult = SerializationResult()
    private var _typeCountOnLastCheck = 0
    private val _modelsMarkedAsSent = TreeSet<Int>()

    init {
        _buffer = ByteArray(102400)
        _ourBuffer = _buffer
    }

    @JvmOverloads fun reset(buffer: ByteArray = _ourBuffer): NetworkSerializer {
        _pos = 0
        _buffer = buffer
        //		_buffer[_pos++] = PACKET_BEGIN;
        _modelsMarkedAsSent.clear()
        return this
    }

    val newInspectedTypeCountToBeManuallySent: Int
        get() {
            val count = inspector.registeredModelsCount
            val diff = count - _typeCountOnLastCheck
            _typeCountOnLastCheck = count
            return diff
        }

    fun beginArray(elementType: DataType, length: Int): NetworkSerializer {
        addType(DataType.Array)
        addType(elementType)
        addRawInt(length)

        return this
    }

    fun beginArray(length: Int): NetworkSerializer {
        return beginArray(DataType.Unknown, length)
    }

    fun addType(type: DataType): NetworkSerializer {
        return addRawByte(type.ordinal.toByte())
    }

    fun addByte(value: Byte): NetworkSerializer {
        addType(DataType.Byte)
        _buffer[_pos++] = value
        return this
    }

    fun addRawByte(value: Byte): NetworkSerializer {
        _buffer[_pos++] = value
        return this
    }

    fun addShort(value: Short): NetworkSerializer {
        addType(DataType.Short)
        addRawShort(value)
        return this
    }

    protected fun addRawShort(value: Short) {
        _buffer[_pos++] = (value.toInt() shr 8 and 0xFF).toByte()
        _buffer[_pos++] = (value.toInt() and 0xFF).toByte()
    }

    fun addInt(value: Int): NetworkSerializer {
        addType(DataType.Int)
        addRawInt(value)
        return this
    }

    fun addRawInt(value: Int) {
        _buffer[_pos++] = (value shr 24 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 16 and 0xFF).toByte()
        _buffer[_pos++] = (value shr 8 and 0xFF).toByte()
        _buffer[_pos++] = (value and 0xFF).toByte()
    }

    fun addLong(value: Long): NetworkSerializer {
        addType(DataType.Long)
        addRawLong(value)
        return this
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

    fun addString(value: String?): NetworkSerializer {
        if (tryAddNullable(value)) {
            return this
        }

        addType(DataType.String)

        val n = value!!.length
        addRawInt(n)

        for (i in 0..n - 1) {
            _buffer[_pos++] = (value!![i].toInt() and 0xFF).toByte()
        }

        return this
    }

    fun addBoolean(value: Boolean): NetworkSerializer {
        addType(DataType.Boolean)
        return addRawBoolean(value)
    }

    fun addRawBoolean(value: Boolean): NetworkSerializer {
        return addRawByte((if (value) 1 else 0).toByte())
    }

    fun addFloat(value: Float): NetworkSerializer {
        addType(DataType.Float)
        return addRawFloat(value)
    }

    fun addRawFloat(value: Float): NetworkSerializer {
        addRawInt(java.lang.Float.floatToIntBits(value))
        return this
    }

    fun addDouble(value: Double): NetworkSerializer {
        addType(DataType.Double)
        return addRawDouble(value)
    }

    fun addRawDouble(value: Double): NetworkSerializer {
        addRawLong(java.lang.Double.doubleToLongBits(value))
        return this
    }

    fun addBitVector(bitVector: BitVector?): NetworkSerializer {
        if (tryAddNullable(bitVector)) {
            return this
        }

        addType(DataType.BitVector)

        val bitsCount = bitVector!!.length()
        addRawShort(bitsCount.toShort())

        var i = 0
        var value: Int
        while (i < bitsCount) {
            value = 0
            var j = 0
            while (j < Integer.SIZE && j < bitsCount) {
                val bit = bitVector.get(i)

                if (bit) {
                    value = value or (1 shl j)
                }
                ++j
                ++i
            }

            addRawInt(value)
        }

        return this
    }

    protected fun tryAddNullable(data: Any?): Boolean {
        if (data == null) {
            addType(DataType.Null)
            return true
        }

        return false
    }

    @JvmOverloads fun addSomething(obj: Any?, allowUnknown: Boolean = false): NetworkSerializer {
        if (obj == null) {
            tryAddNullable(obj)
        }
        else if (obj is Byte) {
            addByte(obj.toByte())
        }
        else if (obj is Short) {
            addShort(obj.toShort())
        }
        else if (obj is Int) {
            addInt(obj.toInt())
        }
        else if (obj is Long) {
            addLong(obj.toLong())
        }
        else if (obj is String) {
            addString(obj as String)
        }
        else if (obj is Boolean) {
            addBoolean(obj)
        }
        else if (obj is Float) {
            addFloat(obj.toFloat())
        }
        else if (obj is Double) {
            addDouble(obj.toDouble())
        }
        else if (obj is BitVector) {
            addBitVector(obj as BitVector)
        }
        else if (allowUnknown) {
            addType(DataType.Unknown)
        }
        else {
            throw IllegalArgumentException("Can't serialize type: " + obj.javaClass)
        }

        return this
    }

    fun addRawByType(valueType: DataType, value: Any): NetworkSerializer {
        when (valueType) {
            DataType.Byte -> addRawByte(value as Byte)
            DataType.Short -> addRawShort(value as Short)
            DataType.Int -> addRawInt(value as Int)
            DataType.Long -> addRawLong(value as Long)
            DataType.String -> addString(value as String)
            DataType.Boolean -> addRawBoolean(value as Boolean)
            DataType.Float -> addRawFloat(value as Float)
            DataType.Double -> addRawDouble(value as Double)
            DataType.BitVector -> addBitVector(value as BitVector)

            else -> throw RuntimeException("type not supported: " + valueType)
        }

        return this
    }

    fun addDataDescriptionOrRef(model: ObjectModelNode): NetworkSerializer {
        if (!_modelsMarkedAsSent.contains(model.id)) {
            addType(DataType.Description)
            addRawDataDescription(model)

            _modelsMarkedAsSent.add(model.id)
        }
        else {
            addType(DataType.DescriptionRef)
            addRawInt(model.id)
        }

        return this
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
        else if (NetworkSerialization.isSimpleType(model.dataType)) {
            addType(model.dataType)
        }
        else if (model.isEnum) {
            addType(DataType.Enum)
            addRawInt(model.enumModelId())
        }
        else if (model.dataType == DataType.EnumValue) {
            addType(DataType.EnumValue)
            addRawInt(model.enumValue)
            addString(model.name!!)
        }
        else if (model.dataType == DataType.EnumDescription) {
            addType(DataType.EnumDescription)

            val enumModelId = model.id
            addRawInt(enumModelId)

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

            if (NetworkSerialization.isSimpleType(arrayType)) {
                // do nothing
            }
            else if (arrayType == DataType.Object || arrayType == DataType.Unknown) {
                //				int modelId = model.children.get(0).id;
                //				addRawInt(modelId);
            }
            else if (arrayType == DataType.Enum) {
                // Note: if we treat array of enums the same way as array of objects
                // then we do not have to write anything here.
                /*
				int enumModelId = model.enumModelId();
				addRawInt(enumModelId); // id of Enum
				ObjectModelNode enumModel = inspector.getModelById(enumModelId);
				addRawInt(enumModel.children.get(0).id); // id of EnumDescription
				*/
            }
            else {
                throw RuntimeException("unsupported array type: " + arrayType)
            }
        }
        else {
            throw RuntimeException("unsupported type: " + model.dataType)
        }
    }

    /**
     * Inspects object, adds it's definition or cached ID if it was already inspected.
     * Then serializes the object.

     *
     * It is not the same as manual subsequent calls
     * of `addObjectDescription()` and `addObject()`
     * because of the inspection cache.
     */
    fun addObject(obj: Any?): NetworkSerializer {
        if (tryAddNullable(obj)) {
            return this
        }

        assert(!obj!!.javaClass.isArray)
        val previousInspectionCount = inspector.registeredModelsCount
        val model = inspector.inspect(obj.javaClass)
        val inspectionCount = inspector.registeredModelsCount

        addType(DataType.MultipleDescriptions)
        val diff = inspectionCount - previousInspectionCount
        addRawInt(diff)

        if (diff > 0) {
            for (i in previousInspectionCount + 1 until inspectionCount) {
                addDataDescriptionOrRef(
                    inspector.getRegisteredModelByIndex(i)
                )
            }
            addDataDescriptionOrRef(
                inspector.getRegisteredModelByIndex(previousInspectionCount)
            )

            _typeCountOnLastCheck = inspectionCount
        }
        else {
            addType(DataType.DescriptionRef)
            addRawInt(model.id)
        }

        // Note: even though we have inspected as much as we could up to this point,
        // there could be added more types because of Object Arrays.
        addObject(model, obj)

        return this
    }

    fun addObject(model: ObjectModelNode, obj: Any): NetworkSerializer {
        addType(DataType.Object)
        addRawObject(model, obj)

        return this
    }

    protected fun addRawObject(model: ObjectModelNode, obj: Any?) {
        if (tryAddNullable(obj)) {
            // well, null is added here.
        }
        else if (model.dataType == DataType.Object || model.dataType == DataType.Unknown) {
            addType(DataType.Object)
            val n = model.children!!.size

            for (i in 0..n - 1) {
                val child = model.children!![i]
                val childObject = ReflectionUtils.getHiddenFieldValue(obj!!.javaClass, child.name!!, obj)

                addRawObject(child, childObject)
            }
        }
        else if (isSimpleType(model.dataType)) {
            assert(model.isTypePrimitive)
            addRawByType(model.dataType, obj!!)
        }
        else if (model.isEnum) {
            addType(DataType.Enum)

            val enumVal = (obj as Enum<DataType>).ordinal
            addRawInt(enumVal)
        }
        else if (model.isArray) {
            // TODO probably this case will be moved to `addArray()`

            val array = obj as Array<Any>
            addRawArray(array, model.arrayType())
        }
        else {
            throw RuntimeException("unsupported type: " + model.dataType)
        }
    }


    fun addArray(array: Array<Any>?): NetworkSerializer {
        assert(array != null)

        val arrayType = if (array!!.isNotEmpty()) determineType(array[0].javaClass).first else DataType.Object
        addRawArray(array, arrayType)

        return this
    }

    fun addRawArray(array: Array<Any>, arrayType: DataType) {
        val n = array.size
        beginArray(arrayType, n)

        if (arrayType == DataType.Unknown || arrayType == DataType.Object) {
            for (i in 0..n - 1) {
                addObject(array[i])
            }
        }
        else if (isSimpleType(arrayType)) {
            for (i in 0..n - 1) {
                addRawByType(arrayType, array[i])
            }
        }
        else if (arrayType == DataType.Enum) {
            for (i in 0..n - 1) {
                val enumVal = (array[i] as Enum<DataType>).ordinal
                addRawInt(enumVal)
            }
        }
        else {
            throw RuntimeException("unsupported array type: " + arrayType)
        }
    }

    //		_buffer[_pos++] = PACKET_END;
    val result: SerializationResult
        get() = if (_buffer == _ourBuffer)
            _serializeResult.setup(_buffer, _pos)
        else
            SerializationResult(_buffer, _pos)


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
