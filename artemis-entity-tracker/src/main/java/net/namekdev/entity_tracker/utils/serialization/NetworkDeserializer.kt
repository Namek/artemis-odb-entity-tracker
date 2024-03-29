package net.namekdev.entity_tracker.utils.serialization

import com.artemis.utils.BitVector
import java.util.*


class NetworkDeserializer : NetworkSerialization() {
    private var _source: ByteArray? = null
    private var _sourcePos: Int = 0
    private var _sourceBeginPos: Int = 0

    private val _models = object : ObjectModelsCollection {
        private val models = ArrayList<ObjectModelNode>()

        override fun size(): Int {
            return models.size
        }

        override fun get(index: Int): ObjectModelNode {
            return models[index]
        }

        override fun get(type: Class<*>): ObjectModelNode {
            throw RuntimeException("deserializer doesn't provide inspection")
        }

        override fun getById(id: Int): ObjectModelNode? {
            for (node in models) {
                if (node.id == id)
                    return node
            }

            return null
        }

        override fun add(model: ObjectModelNode) {
            models.add(model)
        }
    }

    fun setSource(bytes: ByteArray, offset: Int, length: Int) {
        _source = bytes
        _sourcePos = offset
        _sourceBeginPos = offset
    }

    val consumedBytesCount: Int
        get() = _sourcePos - _sourceBeginPos

    fun beginArray(elementType: DataType, shouldBePrimitive: Boolean): Int {
        checkType(DataType.Array)
        val isPrimitive = readRawBoolean()

        if (isPrimitive != shouldBePrimitive) {
            throw RuntimeException("Array primitiveness was expected to be: $shouldBePrimitive, got: $isPrimitive")
        }

        checkType(elementType)
        return readRawInt()
    }

    fun beginArray(): Triple<Boolean, DataType, Int> {
        checkType(DataType.Array)
        val isPrimitive = readRawBoolean()
        val elementType = readType()
        val size = readRawInt()
        return Triple(isPrimitive, elementType, size)
    }

    private fun peakArray(): Triple<Boolean, DataType, Int> {
        val beginPos = _sourcePos
        val ret = beginArray()
        _sourcePos = beginPos

        return ret
    }

    fun readType(): DataType {
        val value = DataType.values()[readRawByte().toInt()]
        dbgType(value)
        return value
    }

    fun readByte(): Byte {
        checkType(DataType.Byte)
        return readRawByte()
    }

    fun readShort(): Short {
        checkType(DataType.Short)
        return readRawShort()
    }

    fun readInt(): Int {
        checkType(DataType.Int)
        return readRawInt()
    }

    fun readLong(): Long {
        checkType(DataType.Long)
        return readRawLong()
    }

    fun readRawLong(): Long {
        var value = readRawInt().toLong()
        value = value shl 32
        value = value or readRawInt().toLong()

        return value
    }

    fun readString(): String? {
        if (checkNull()) {
            return null
        }

        checkType(DataType.String)
        val length = readRawInt()

        val sb = StringBuilder(length)
        for (i in 0..length - 1) {
            sb.append((_source!![_sourcePos++].toInt() and 0xFF).toChar())
        }

        return sb.toString()
    }

    fun readBoolean(): Boolean {
        checkType(DataType.Boolean)
        return readRawBoolean()
    }

    fun readRawBoolean(): Boolean {
        val value = readRawByte()
        return value.toInt() != 0
    }

    fun readFloat(): Float {
        checkType(DataType.Float)
        return readRawFloat()
    }

    fun readRawFloat(): Float {
        return java.lang.Float.intBitsToFloat(readRawInt())
    }

    fun readDouble(): Double {
        checkType(DataType.Double)
        return readRawDouble()
    }

    fun readRawDouble(): Double {
        return java.lang.Double.longBitsToDouble(readRawLong())
    }

    fun readBitVector(): BitVector? {
        if (checkNull()) {
            return null
        }

        checkType(DataType.BitVector)

        val allBitsCount = readRawShort()
        val bitVector = BitVector(allBitsCount.toInt())

        var i = 0
        while (i < allBitsCount) {
            var value = readRawInt()

            val isLastPart = allBitsCount - i < Integer.SIZE
            val nBits = if (isLastPart) allBitsCount % Integer.SIZE else Integer.SIZE

            var j = 0
            while (j < nBits) {
                if (value and 1 == 1) {
                    bitVector.set(i)
                }
                value = value shr 1
                ++j
                ++i
            }
        }

        return bitVector
    }

    fun readRawByte(): Byte {
        return _source!![_sourcePos++]
    }

    fun readRawShort(): Short {
        var value = (_source!![_sourcePos++].toInt() and 0xFF).toShort()
        value = (value.toInt() shl 8).toShort()
        value = (value.toInt() or (_source!![_sourcePos++].toInt() and 0xFF)).toShort()

        return value
    }

    @JvmOverloads fun readSomething(allowUnknown: Boolean = false): Any? {
        val type = DataType.values()[_source!![_sourcePos].toInt()]

        if (type == DataType.Null) {
            _sourcePos++
            return null
        }
        else if (type == DataType.Byte)
            return readByte()
        else if (type == DataType.Short)
            return readShort()
        else if (type == DataType.Int)
            return readInt()
        else if (type == DataType.Long)
            return readLong()
        else if (type == DataType.String)
            return readString()
        else if (type == DataType.Boolean)
            return readBoolean()
        else if (type == DataType.Float)
            return readFloat()
        else if (type == DataType.Double)
            return readDouble()
        else if (type == DataType.BitVector)
            return readBitVector()
        else if (allowUnknown) {
            _sourcePos++
            return DataType.Unknown
        }
        else
            throw IllegalArgumentException("Can't serialize type: " + type)
    }

    fun readRawByType(valueType: DataType): Any {
        when (valueType) {
            DataType.Byte -> return readRawByte()
            DataType.Short -> return readRawShort()
            DataType.Int -> return readRawInt()
            DataType.Long -> return readRawLong()
            DataType.String -> return readString() as Any
            DataType.Boolean -> return readRawBoolean()
            DataType.Float -> return readRawFloat()
            DataType.Double -> return readRawDouble()
            DataType.BitVector -> return readBitVector() as Any

            else -> throw RuntimeException("type not supported" + valueType)
        }
    }

    fun readDataDescription(): ObjectModelNode {
        return readDataDescription(null)
    }

    private fun readDataDescription(parentNode: ObjectModelNode?): ObjectModelNode {
        val type = readType()

        var retModel: ObjectModelNode? = null

        if (type == DataType.Description) {
            retModel = readRawDataDescription(null)
        }
        else if (type == DataType.DescriptionRef) {
            val modelId = readRawInt()
            retModel = _models.getById(modelId)
        }
        else {
            throw RuntimeException("unexpected type: " + type)
        }

        return retModel!!
    }

    private fun readRawDataDescription(parentNode: ObjectModelNode?): ObjectModelNode {
        val modelId = readRawInt()
        val node = ObjectModelNode(null, modelId, parentNode)
        this._models.add(node)
        node.name = readString()
        node.isTypePrimitive = readBoolean()
        val nodeType = readType()
        node.dataType = nodeType


        if (nodeType == DataType.Object) {
            val n = readRawInt()
            node.children = Vector<ObjectModelNode>(n)

            for (i in 0..n - 1) {
                val child = readDataDescription(node)
                node.children!!.addElement(child)
            }
        }
        else if (nodeType == DataType.Array) {
            node.dataSubType = readType()
            dbgType(node.dataSubType)

            if (isSimpleType(node.dataSubType)) {
                // do nothing
            }
            else if (node.dataSubType == DataType.Object) {
                //				int objModelId = readRawInt();

                // TODO create model
                //				throw new RuntimeException("TODO array of objects");
            }
            else if (node.dataSubType == DataType.Enum) {
                val enumModel = readDataDescription()
                node.children = Vector(1)
                node.children!!.addElement(enumModel)
            }
            else if (node.dataSubType == DataType.Array) {
                val depth = readRawInt()
                val deepSubType = readType()
                val isDeepSubTypePrimitive = readBoolean()

                var curNode = node
                for (i in 1..depth) {
                    val id = readRawInt()
                    val subnode = ObjectModelNode(null, id, curNode)
                    subnode.dataType = DataType.Array
                    subnode.dataSubType = DataType.Array
                    curNode.children = Vector()
                    curNode.children!!.addElement(subnode)
                    curNode = subnode
                }
                curNode.dataSubType = deepSubType
                curNode.isSubTypePrimitive = isDeepSubTypePrimitive
            }
            else {
                throw RuntimeException("unsupported array type: " + node.dataSubType)
            }
        }
        else if (nodeType == DataType.Enum) {
            val enumModel = readDataDescription()
            node.children = Vector(1)
            node.children!!.addElement(enumModel)
        }
        else if (nodeType == DataType.EnumValue) {
            node.enumValue = readRawInt()
            node.name = readString()
        }
        else if (nodeType == DataType.EnumDescription) {
            val id = readRawInt()

            var enumModel: ObjectModelNode? = _models.getById(id)
            if (enumModel == null) {
                enumModel = ObjectModelNode(null, id, node)
                this._models.add(enumModel)
            }

            val n = readRawInt()
            enumModel.children = Vector<ObjectModelNode>(n)
            for (i in 0..n - 1) {
                val valueId = readRawInt()
                val enumValueModel = ObjectModelNode(null, valueId, null/*TODO here's null! should be?*/)
                enumValueModel.dataType = DataType.EnumValue
                enumValueModel.enumValue = readRawInt()
                enumValueModel.name = readString()
                enumModel.children!!.add(enumValueModel)
                //				this._models.add(enumValueModel);
            }
        }
        else if (!NetworkSerialization.isSimpleType(nodeType)) {
            throw RuntimeException("unsupported type: " + nodeType)
        }

        return node
    }

    fun readObject(): ValueTree? {
        return readObject(true)
    }

    fun readObject(joinDataToModel: Boolean): ValueTree? {
        return readObject(joinDataToModel, ObjectReadSession())
    }

    private fun readObject(joinModelToData: Boolean, session: ObjectReadSession): ValueTree? {
        val model = possiblyReadDescriptions()

        if (model != null) {
            return readObject(model, session, joinModelToData)
        }
        else if (checkNull()) {
            return null
        }
        else {
            // This is hidden array in Object field.
            // Example: Object someField = new int[] { ... }

            return readArray(joinModelToData, session)
        }
    }

    private fun possiblyReadDescriptions(force: Boolean = true): ObjectModelNode? {
        if (force) {
            checkType(DataType.MultipleDescriptions)
        }
        else {
            if (!peakType(DataType.MultipleDescriptions)) {
                return null
            }
        }

        val descrCount = readRawInt()

        var rootModel: ObjectModelNode? = null

        if (descrCount > 0) {
            var model: ObjectModelNode? = null
            for (i in 0 until descrCount - 1) {
                model = readDataDescription()
            }

            rootModel = readDataDescription()
        }
        else {
            checkType(DataType.DescriptionRef)
            val modelId = readRawInt()
            var i = 0
            val n = _models.size()
            while (i < n) {
                val model = _models[i]

                if (model.id == modelId) {
                    rootModel = model
                }
                ++i
            }
        }

        return rootModel
    }


    @JvmOverloads
    fun readObject(model: ObjectModelNode, joinDataToModel: Boolean = false): ValueTree {
        return readObject(model, ObjectReadSession(), joinDataToModel)
    }

    @JvmOverloads
    private fun readObject(model: ObjectModelNode, session: ObjectReadSession, joinDataToModel: Boolean = false): ValueTree {
        checkType(DataType.Object)
        val root = readRawObject(model, null, session, joinDataToModel) as ValueTree?

        return root!!
    }

    private fun readRawObject(model: ObjectModelNode, parentTree: ValueTree?, session: ObjectReadSession, joinModelToData: Boolean): Any? {
        if (checkNull()) {
            return null
        }
        else if (model.dataType == DataType.Object || model.dataType == DataType.Unknown) {
            val dataType = readType()
            val id = readRawShort()

            if (dataType == DataType.Object) {
                val n = model.children!!.size
                val tree = ValueTree(n)
                tree.id = id
                tree.parent = parentTree

                session.remember(id, tree, model)

                for (i in 0..n - 1) {
                    val child = model.children!![i]
                    tree.values[i] = readRawObject(child, tree, session, joinModelToData)
                }

                if (joinModelToData) {
                    tree.model = model
                }

                return tree
            }
            else if (dataType == DataType.ObjectRef) {
                return session.find(id)!!.tree
            }
            else if (dataType == DataType.Array) {
                // This is hidden array in Object field.
                // Example: Object someField = new int[] { ... }

                // TODO HACK: we should identify every array!
                // it would be best to do it in readArray() method.
                // So ID would included within beginArray()
                _sourcePos -= 3

                val arrayTree = readArray(joinModelToData, session)

                if (arrayTree != null) {
                    arrayTree.parent = parentTree
                }

                return arrayTree
            }
            else {
                throw RuntimeException("Types are divergent, expected: ${DataType.Object} or ${DataType.ObjectRef}, got: $dataType")
            }
        }
        else if (isSimpleType(model.dataType)) {
            assert(model.isTypePrimitive)
            val value = readRawByType(model.dataType)

            return value
        }
        else if (model.isEnum) {
            checkType(DataType.Enum)
            val enumVal = readRawInt()
            // TODO probably no one expected integer here, some Enum<?> is rather expected

            return enumVal
        }
        else if (model.isArray) {
            val array = readArray(model, joinModelToData, session)

            if (array != null) {
                array.parent = parentTree
            }

            return array
        }
        else {
            throw RuntimeException("unsupported type: " + model.dataType + ", subtype: " + model.dataSubType)
        }
    }

    fun readArray(model: ObjectModelNode, joinModelToData: Boolean = true): ValueTree? {
        return readArray(model, joinModelToData, ObjectReadSession())
    }

    fun readPrimitiveBooleanArray(): BooleanArray {
        val n = beginArray(DataType.Boolean, true)
        val arr = BooleanArray(n)

        // TODO: optimize by using BitVector?
        for (i in 0..n-1) {
            arr[i] = readRawBoolean()
        }
        return arr
    }

    fun readPrimitiveByteArray(): ByteArray {
        val n = beginArray(DataType.Byte, true)
        val arr = ByteArray(n)
        for (i in 0..n-1) {
            arr[i] = readRawByte()
        }
        return arr
    }

    fun readPrimitiveShortArray(): ShortArray {
        val n = beginArray(DataType.Short, true)
        val arr = ShortArray(n)
        for (i in 0..n-1) {
            arr[i] = readRawShort()
        }
        return arr
    }

    fun readPrimitiveIntArray(): IntArray {
        val n = beginArray(DataType.Int, true)
        val arr = IntArray(n)
        for (i in 0..n-1) {
            arr[i] = readRawInt()
        }
        return arr
    }

    fun readPrimitiveLongArray(): LongArray {
        val n = beginArray(DataType.Long, true)
        val arr = LongArray(n)
        for (i in 0..n-1) {
            arr[i] = readRawLong()
        }
        return arr
    }

    fun readPrimitiveFloatArray(): FloatArray {
        val n = beginArray(DataType.Float, true)
        val arr = FloatArray(n)
        for (i in 0..n-1) {
            arr[i] = readRawFloat()
        }
        return arr
    }

    fun readPrimitiveDoubleArray(): DoubleArray {
        val n = beginArray(DataType.Double, true)
        return DoubleArray(n, { readRawDouble() })
    }

    fun readBooleanArray(): Array<Boolean?> {
        val n = beginArray(DataType.Boolean, false)
        return Array<Boolean?>(n, {
            if (expectTypeOrNull(DataType.Boolean))
                readRawBoolean()
            else null
        })
    }

    fun readByteArray(): Array<Byte?> {
        val n = beginArray(DataType.Byte, false)
        return Array<Byte?>(n, {
            if (expectTypeOrNull(DataType.Byte))
                readRawByte()
            else null
        })
    }

    fun readShortArray(): Array<Short?> {
        val n = beginArray(DataType.Short, false)
        return Array<Short?>(n, {
            if (expectTypeOrNull(DataType.Short))
                readRawShort()
            else null
        })
    }

    fun readIntArray(): Array<Int?> {
        val n = beginArray(DataType.Int, false)
        return Array<Int?>(n, {
            if (expectTypeOrNull(DataType.Int))
                readRawInt()
            else null
        })
    }

    fun readLongArray(): Array<Long?> {
        val n = beginArray(DataType.Long, false)
        return Array<Long?>(n, { i->
            if (expectTypeOrNull(DataType.Long))
                readRawLong()
            else null
        })
    }

    fun readFloatArray(): Array<Float?> {
        val n = beginArray(DataType.Float, false)
        return Array<Float?>(n, {
            if (expectTypeOrNull(DataType.Float))
                readRawFloat()
            else null
        })
    }

    fun readDoubleArray(): Array<Double?> {
        val n = beginArray(DataType.Double, false)
        return Array<Double?>(n, {
            if (expectTypeOrNull(DataType.Double))
                readRawDouble()
            else null
        })
    }

    fun readPrimitiveBooleanArray_asBoxedArray(): Array<Boolean?> {
        val n = beginArray(DataType.Boolean, true)

        // TODO: optimize by using BitVector?
        return Array<Boolean?>(n, { readRawBoolean() })
    }

    fun readPrimitiveByteArray_asBoxedArray(): Array<Byte?> {
        val n = beginArray(DataType.Byte, true)
        return Array<Byte?>(n, { readRawByte() })
    }

    fun readPrimitiveShortArray_asBoxedArray(): Array<Short?> {
        val n = beginArray(DataType.Short, true)
        return Array<Short?>(n, { readRawShort() })
    }

    fun readPrimitiveIntArray_asBoxedArray(): Array<Int?> {
        val n = beginArray(DataType.Int, true)
        return Array<Int?>(n, { readRawInt() })
    }

    fun readPrimitiveLongArray_asBoxedArray(): Array<Long?> {
        val n = beginArray(DataType.Long, true)
        return Array<Long?>(n, { readRawLong() })
    }

    fun readPrimitiveFloatArray_asBoxedArray(): Array<Float?> {
        val n = beginArray(DataType.Float, true)
        return Array<Float?>(n, { readRawFloat() })
    }

    fun readPrimitiveDoubleArray_asBoxedArray(): Array<Double?> {
        val n = beginArray(DataType.Double, true)
        return Array<Double?>(n, { readRawDouble() })
    }


    /**
     * Read array of primitives.
     */
    fun readPrimitiveArrayByType(arrayType: DataType): Any {
        when (arrayType) {
            DataType.Boolean -> return readPrimitiveBooleanArray()
            DataType.Byte -> return readPrimitiveByteArray()
            DataType.Short -> return readPrimitiveShortArray()
            DataType.Int -> return readPrimitiveIntArray()
            DataType.Long -> return readPrimitiveLongArray()
            DataType.Float -> return readPrimitiveFloatArray()
            DataType.Double -> return readPrimitiveDoubleArray()
            else -> throw RuntimeException("unknown primitive array type: ${arrayType}")
        }
    }

    /**
     * Read array of primitives and return it as array of boxed values.
     */
    fun readPrimitiveArrayByType_asBoxedArray(arrayType: DataType): Array<*> {
        when (arrayType) {
            DataType.Boolean -> return readPrimitiveBooleanArray_asBoxedArray()
            DataType.Byte -> return readPrimitiveByteArray_asBoxedArray()
            DataType.Short -> return readPrimitiveShortArray_asBoxedArray()
            DataType.Int -> return readPrimitiveIntArray_asBoxedArray()
            DataType.Long -> return readPrimitiveLongArray_asBoxedArray()
            DataType.Float -> return readPrimitiveFloatArray_asBoxedArray()
            DataType.Double -> return readPrimitiveDoubleArray_asBoxedArray()
            else -> throw RuntimeException("unknown primitive array type: ${arrayType}")
        }
    }

    /**
     * Read array of non-primitives (can contain nulls).
     */
    fun readArrayByType(arrayType: DataType): Array<*> {
        when (arrayType) {
            DataType.Boolean -> return readBooleanArray()
            DataType.Byte -> return readByteArray()
            DataType.Short -> return readShortArray()
            DataType.Int -> return readIntArray()
            DataType.Long -> return readLongArray()
            DataType.Float -> return readFloatArray()
            DataType.Double -> return readDoubleArray()
            else -> throw RuntimeException("unknown primitive array type: ${arrayType}")
        }
    }


    /**
     * Read array without a known model a priori.
     */
    fun readArray(joinModelToData: Boolean = true): ValueTree? {
        return readArray(joinModelToData, ObjectReadSession())
    }

    /**
     * Read array without a known model a priori.
     */
    private fun readArray(joinModelToData: Boolean, session: ObjectReadSession): ValueTree? {
        val rootModel = possiblyReadDescriptions(false)

        if (checkNull())
            return null

        if (rootModel != null && rootModel.isArray) {
            return readArray(rootModel, joinModelToData, session)
        }
        else {
            val (isPrimitive, elementType, n) = peakArray()
            val node: ValueTree?

            if (isPrimitive) {
                val arr = readPrimitiveArrayByType_asBoxedArray(elementType) as Array<Any?>
                node = ValueTree(arr)
            }
            else if (elementType == DataType.Unknown) {
                val n = beginArray(DataType.Unknown, false)
                node = ValueTree(Array<Any?>(n, { readObject() }))
            }
            else {
                val arr = readArrayByType(elementType) as Array<Any?>
                node = ValueTree(arr)
            }

            return node
        }
    }

    private fun readArray(model: ObjectModelNode, joinModelToData: Boolean, session: ObjectReadSession): ValueTree? {
        if (checkNull())
            return null

        if (model.isSubTypePrimitive) {
            val array = readPrimitiveArrayByType_asBoxedArray(model.arrayType())
            val node = ValueTree(array as Array<Any?>)

            if (joinModelToData) {
                node.model = model
            }

            return node
        }
        else {
            val arrayType = model.arrayType()
            val n = beginArray(arrayType, false)
            val node = ValueTree(n)

            if (joinModelToData) {
                node.model = model
            }

            if (arrayType == DataType.Object || arrayType == DataType.Unknown) {
                for (i in 0..n - 1) {
                    val value = readObject(joinModelToData, session)

                    if (value != null) {
                        value.parent = node
                    }

                    node.values[i] = value
                }
            }
            else if (NetworkSerialization.isSimpleType(arrayType)) {
                for (i in 0..n - 1) {
                    node.values[i] = readRawByType(arrayType)
                }
            }
            else if (/*model.isEnumArray()*/ arrayType == DataType.Enum) {
                for (i in 0..n - 1) {
                    val type = readType()
                    node.values[i] = (
                        if (type == DataType.Null)
                            null
                        else if (type == DataType.EnumValue)
                            readRawInt()
                        else
                            throw RuntimeException("array enum didn't expect type: $type")
                    )
                }
            }
            else if (arrayType == DataType.Array) {
                val subModel = model.children!![0]

                for (i in 0..n-1) {
                    val subArray = readArray(subModel, joinModelToData, session)

                    if (subArray != null) {
                        subArray.parent = node
                    }

                    node.values[i] = subArray
                }
            }
            else {
                throw RuntimeException("unsupported array type: " + arrayType)
            }

            return node
        }
    }

    fun readRawInt(): Int {
        var value = _source!![_sourcePos++].toInt() and 0xFF
        value = value shl 8
        value = value or (_source!![_sourcePos++].toInt() and 0xFF)
        value = value shl 8
        value = value or (_source!![_sourcePos++].toInt() and 0xFF)
        value = value shl 8
        value = value or (_source!![_sourcePos++].toInt() and 0xFF)

        return value
    }

    protected fun checkType(type: DataType) {
        val srcType = _source!![_sourcePos++]
        dbgType(type)

        if (srcType.toInt() != type.ordinal) {
            val resultType = DataType.values()[srcType.toInt()]
            throw RuntimeException("Types are divergent, expected: $type, got: $resultType")
        }
    }

    protected fun peakType(type: DataType): Boolean {
        val t = _source!![_sourcePos].toInt()
        return t == type.ordinal
    }

    fun expectTypeOrNull(expectedType: DataType): Boolean {
        val type = readType()
        val isNull = type === DataType.Null

        if (type != expectedType && !isNull) {
            throw RuntimeException("Types are divergent, expected: $type, got: $type")
        }

        return !isNull
    }

    protected fun checkNull(): Boolean {
        if (_source!![_sourcePos].toInt() == DataType.Null.ordinal) {
            dbgType(DataType.Null)
            ++_sourcePos
            return true
        }

        return false
    }

    private inline fun dbgType(t: DataType) {
        //println(t)
    }
}

internal class ObjectReadSession {
    val trees = ArrayList<TreeContainer>()
    val treesMap = TreeMap<Int, TreeContainer>()

    fun find(id: Short): TreeContainer? {
        var container = treesMap.get(id.toInt())

        if (container == null) {
            container = trees.find { it.id == id }
        }

        return container
    }

    fun remember(id: Short, tree: ValueTree, model: ObjectModelNode) {
        val container = TreeContainer(id, tree, model)
        trees.add(container)
        treesMap.put(id.toInt(), container)
    }
}

data class TreeContainer(val id: Short, val tree: ValueTree, val model: ObjectModelNode)