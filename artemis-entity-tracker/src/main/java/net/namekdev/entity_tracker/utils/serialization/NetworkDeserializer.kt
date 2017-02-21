package net.namekdev.entity_tracker.utils.serialization

import java.util.ArrayList
import java.util.Vector

import com.artemis.utils.BitVector


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

    @JvmOverloads fun beginArray(elementType: Type = Type.Unknown): Int {
        checkType(Type.Array)
        checkType(elementType)
        return readRawInt()
    }

    fun readType(): Type {
        val value = Type.values()[readRawByte().toInt()]
        //		dbgType(value);
        return value
    }

    fun readByte(): Byte {
        checkType(Type.Byte)
        return readRawByte()
    }

    fun readShort(): Short {
        checkType(Type.Short)
        return readRawShort()
    }

    fun readInt(): Int {
        checkType(Type.Int)
        return readRawInt()
    }

    fun readLong(): Long {
        checkType(Type.Long)
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

        checkType(Type.String)
        val length = readRawInt()

        val sb = StringBuilder(length)
        for (i in 0..length - 1) {
            sb.append((_source!![_sourcePos++].toInt() and 0xFF).toChar())
        }

        return sb.toString()
    }

    fun readBoolean(): Boolean {
        checkType(Type.Boolean)
        return readRawBoolean()
    }

    fun readRawBoolean(): Boolean {
        val value = readRawByte()
        return value.toInt() != 0
    }

    fun readFloat(): Float {
        checkType(Type.Float)
        return readRawFloat()
    }

    fun readRawFloat(): Float {
        return java.lang.Float.intBitsToFloat(readRawInt())
    }

    fun readDouble(): Double {
        checkType(Type.Double)
        return readRawDouble()
    }

    fun readRawDouble(): Double {
        return java.lang.Double.longBitsToDouble(readRawLong())
    }

    fun readBitVector(): BitVector? {
        if (checkNull()) {
            return null
        }

        checkType(Type.BitVector)

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
        val type = Type.values()[_source!![_sourcePos].toInt()]

        if (type == Type.Null) {
            _sourcePos++
            return null
        }
        else if (type == Type.Byte)
            return readByte()
        else if (type == Type.Short)
            return readShort()
        else if (type == Type.Int)
            return readInt()
        else if (type == Type.Long)
            return readLong()
        else if (type == Type.String)
            return readString()
        else if (type == Type.Boolean)
            return readBoolean()
        else if (type == Type.Float)
            return readFloat()
        else if (type == Type.Double)
            return readDouble()
        else if (type == Type.BitVector)
            return readBitVector()
        else if (allowUnknown) {
            _sourcePos++
            return Type.Unknown
        }
        else
            throw IllegalArgumentException("Can't serialize type: " + type)
    }

    fun readRawByType(valueType: Type): Any {
        when (valueType) {
            Type.Byte -> return readRawByte()
            Type.Short -> return readRawShort()
            Type.Int -> return readRawInt()
            Type.Long -> return readRawLong()
            Type.String -> return readString() as Any
            Type.Boolean -> return readRawBoolean()
            Type.Float -> return readRawFloat()
            Type.Double -> return readRawDouble()
            Type.BitVector -> return readBitVector() as Any

            else -> throw RuntimeException("type not supported" + valueType)
        }
    }

    fun readDataDescription(): ObjectModelNode {
        return readDataDescription(null)
    }

    private fun readDataDescription(parentNode: ObjectModelNode?): ObjectModelNode {
        val type = readType()

        var retModel: ObjectModelNode? = null

        if (type == Type.Description) {
            retModel = readRawDataDescription(null)
        }
        else if (type == Type.DescriptionRef) {
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
        val nodeType = readType()
        node.networkType = nodeType

        if (nodeType == Type.Object) {
            val n = readRawInt()
            node.children = Vector<ObjectModelNode>(n)

            for (i in 0..n - 1) {
                val child = readDataDescription(node)
                node.children!!.addElement(child)
            }
        }
        else if (nodeType == Type.Array) {
            node.childType = readRawByte().toShort()
            //			dbgType(Type.values()[node.childType]);

            if (NetworkSerialization.isSimpleType(Type.values()[node.childType.toInt()])) {
                // do nothing
            }
            else if (node.childType.toInt() == Type.Object.ordinal) {
                //				int objModelId = readRawInt();

                // TODO create model
                //				throw new RuntimeException("TODO array of objects");
            }
            else if (node.childType.toInt() == Type.Enum.ordinal) {
                // Note: if we treat array of enums the same way as array of objects
                // then we do not have to write anything here.
                /*int enumModelId = readRawInt();
				int enumDescrModelId = readRawInt();

				ObjectModelNode enumFieldModel = new ObjectModelNode(null, enumModelId, node);
				ObjectModelNode enumDescrModel = _models.getById(enumDescrModelId);// new ObjectModelNode(null, enumDescrModelId, null);

				enumFieldModel.networkType = Type.Enum;
				enumFieldModel.children = new Vector<>(1);
				enumFieldModel.children.add(enumDescrModel);

				node.children = new Vector<>(1);
				node.children.add(enumFieldModel);

				this._models.add(enumFieldModel);*/
            }
            else {
                throw RuntimeException("unsupported array type: " + node.childType)
            }
        }
        else if (nodeType == Type.Enum) {
            val enumModelId = readRawInt()
            var enumModelRef: ObjectModelNode? = _models.getById(enumModelId)
            if (enumModelRef == null) {
                enumModelRef = ObjectModelNode(null, enumModelId, node)
                this._models.add(enumModelRef)
            }
            node.children = Vector<ObjectModelNode>(1)
            node.children!!.addElement(enumModelRef)
        }
        else if (nodeType == Type.EnumValue) {
            node.childType = readRawInt().toShort()
            node.name = readString()
        }
        else if (nodeType == Type.EnumDescription) {
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
                enumValueModel.childType = readRawInt().toShort()
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

    fun readObject(): ValueTree {
        return readObject(false)
    }

    private fun readObject(joinDataToModel: Boolean): ValueTree {
        checkType(Type.MultipleDescriptions)
        val descrCount = readRawInt()

        var rootModel: ObjectModelNode? = null

        if (descrCount > 0) {
            var model: ObjectModelNode? = null
            for (i in 0..descrCount - 1 - 1) {
                model = readDataDescription()
            }

            rootModel = readDataDescription()
        }
        else {
            checkType(Type.DescriptionRef)
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

        return readObject(rootModel!!, joinDataToModel)
    }

    @JvmOverloads fun readObject(model: ObjectModelNode, joinDataToModel: Boolean = false): ValueTree {
        checkType(Type.Object)
        val root = readRawObject(model, null, joinDataToModel) as ValueTree?

        return root!!
    }

    protected fun readRawObject(model: ObjectModelNode, parentTree: ValueTree?, joinModelToData: Boolean): Any? {
        val isArray = model.isArray

        if (checkNull()) {
            return null
        }
        else if (!isArray && (model.networkType == Type.Object || model.networkType == Type.Unknown)) {
            checkType(Type.Object)
            val n = model.children!!.size
            val tree = ValueTree(n)
            tree.parent = parentTree

            for (i in 0..n - 1) {
                val child = model.children!![i]
                tree.values[i] = readRawObject(child, tree, joinModelToData)
            }

            if (joinModelToData) {
                tree.model = model
            }

            return tree
        }
        else if (NetworkSerialization.isSimpleType(model.networkType)) {
            val value = readRawByType(model.networkType)

            return value
        }
        else if (model.isEnum) {
            checkType(Type.Enum)
            val enumVal = readRawInt()
            // TODO probably no one expected integer here, some Enum<?> is rather expected

            return enumVal
        }
        else if (isArray) {
            val arrayType = model.arrayType()
            val tree = readRawArray(arrayType, joinModelToData)

            tree.parent = parentTree

            if (joinModelToData) {
                tree.model = model
            }

            return tree
        }
        else {
            throw RuntimeException("unsupported type: " + model.networkType)
        }
    }

    fun readArray(): ValueTree {
        return readRawArray(null, false)
    }

    fun readRawArray(arrayType: Type?, joinModelToData: Boolean): ValueTree {
        var arrayType = arrayType
        checkType(Type.Array)

        if (arrayType != null) {
            checkType(arrayType)
        }
        else {
            arrayType = readType()
        }

        val n = readRawInt()
        val tree = ValueTree(n)

        if (arrayType == Type.Object || arrayType == Type.Unknown) {
            for (i in 0..n - 1) {
                val value = readObject(joinModelToData)
                value.parent = tree
                tree.values[i] = value
            }
        }
        else if (NetworkSerialization.isSimpleType(arrayType)) {
            for (i in 0..n - 1) {
                tree.values[i] = readRawByType(arrayType)
            }
        }
        else if (/*model.isEnumArray()*/ arrayType == Type.Enum) {
            for (i in 0..n - 1) {
                tree.values[i] = readRawInt()
            }
        }
        else {
            throw RuntimeException("unsupported array type: " + arrayType)
        }

        return tree
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

    protected fun checkType(type: Type) {
        val srcType = _source!![_sourcePos++]
        //		dbgType(type);

        if (srcType.toInt() != type.ordinal) {
            val resultType = Type.values()[srcType.toInt()]
            throw RuntimeException("Types are divergent, expected: $type, got: $resultType")
        }
    }

    protected fun checkNull(): Boolean {
        if (_source!![_sourcePos].toInt() == Type.Null.ordinal) {
            //			dbgType(Type.Null);
            ++_sourcePos
            return true
        }

        return false
    }

    private fun dbgType(t: Type) {
        println(t)
    }
}
