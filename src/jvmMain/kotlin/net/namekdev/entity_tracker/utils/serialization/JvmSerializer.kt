package net.namekdev.entity_tracker.utils.serialization

import com.artemis.utils.BitVector
import net.namekdev.entity_tracker.utils.ReflectionUtils
import java.util.*

class JvmSerializer : NetworkSerializer<JvmSerializer, BitVector> {
    val inspector: ObjectTypeInspector
    private var _typeCountOnLastCheck = 0


    constructor() {
        this.inspector = ObjectTypeInspector()
    }

    constructor(inspector: ObjectTypeInspector) {
        this.inspector = inspector
    }


    val newInspectedTypeCountToBeManuallySent: Int
        get() {
            val count = inspector.registeredModelsCount
            val diff = count - _typeCountOnLastCheck
            _typeCountOnLastCheck = count
            return diff
        }

    override fun isBitVector(obj: Any): Boolean {
        // TODO
        return false
    }

    override fun addBitVector(bitVector: BitVector): JvmSerializer {
        addType(DataType.BitVector)

        val bitsCount = bitVector!!.length()
        addRawShort(bitsCount.toShort())

        var i = 0
        var value: Int
        while (i < bitsCount) {
            value = 0
            var j = 0
            while (j < Int.SIZE_BITS && j < bitsCount) {
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


    fun addArray(array: Array<Any>?): JvmSerializer {
        return addArray(array as Any?)
    }

    private inline fun addArray(array: Any?): JvmSerializer {
        addArray(array, ObjectSerializationSession())
        return this
    }

    /**
     * By using this method, we don't know the type of array.
     */
    private fun addArray(array: Any?, session: ObjectSerializationSession) {
        // array is represented as Object because it may be IntArray, ByteArray, etc. which is incompatible with Array<Any>.
        // That's about array's component (element) type: primitive or non-primitive. Array<Any> contains non-primitive objects.

        if (array == null) {
            throw RuntimeException("you can't pass as an array")
        }

        // case: array of non-primitives
        else if (array is Array<*>) {
            beginArray(array.size)

            for (el in array) {
                addObject(el, session)
            }
        }

        // case: array of primitives
        else {
            if (array is BooleanArray)
                addArray(array)
            else if (array is ByteArray)
                addArray(array)
            else if (array is ShortArray)
                addArray(array)
            else if (array is IntArray)
                addArray(array)
            else if (array is LongArray)
                addArray(array)
            else if (array is FloatArray)
                addArray(array)
            else if (array is DoubleArray)
                addArray(array)
            else {
                throw RuntimeException("unknown array type")
            }
        }
    }


    private fun inspectThenAddDescriptionOrRef(obj: Any): ObjectModelNode {
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
        } else {
            addType(DataType.DescriptionRef)
            addRawInt(model.id)
        }

        return model
    }

    /**
     * Inspects object, adds it's definition or cached ID if it was already inspected.
     * Then serializes the object.

     *
     * It is not the same as manual subsequent calls
     * of `addObjectDescription()` and `addObject()`
     * because of the inspection cache.
     */
    fun addObject(obj: Any?): JvmSerializer {
        return addObject(obj, ObjectSerializationSession())
    }

    private fun addObject(obj: Any?, session: ObjectSerializationSession): JvmSerializer {
        if (tryAddNullable(obj)) {
            return this
        }

//        assert(!obj!!.javaClass.isArray)
        val model = inspectThenAddDescriptionOrRef(obj!!)

        // Note: even though we have inspected as much as we could up to this point,
        // there could be added more types because of Object Arrays.
        addObject(model, obj, session)

        return this
    }

    fun addObject(model: ObjectModelNode, obj: Any): JvmSerializer {
        addType(DataType.Object)
        addRawObject(model, obj, ObjectSerializationSession())

        return this
    }

    private fun addObject(model: ObjectModelNode, obj: Any, session: ObjectSerializationSession): JvmSerializer {
        addType(DataType.Object)
        addRawObject(model, obj, session)

        return this
    }

//    abstract protected fun isArray(obj: Any): Boolean

    private fun addRawObject(model: ObjectModelNode, obj: Any?, session: ObjectSerializationSession) {
        if (tryAddNullable(obj)) {
            // well, null is added here.
            return
        }

        val obj = obj!!
//        val isArray = isArray(obj)
//        val isArray = obj is Array<*>
//        val isArray = obj::class == kotlin.Array::class
        val isArray = obj.javaClass.isArray
        val isReferential = model.dataType == DataType.Object || model.dataType == DataType.Unknown || isArray

        if (isReferential) {
            val remembered = session.hasOrRemember(obj)

            if (remembered.first) {
                // add reference to cyclic dependency
                addType(DataType.ObjectRef)
                addRawShort(remembered.second.id)
            } else if (!isArray) {
                addType(DataType.Object)
                addRawShort(remembered.second.id)
                val n = model.children!!.size

                for (i in 0 until n) {
                    val child = model.children!![i]
                    val childObject = ReflectionUtils.getHiddenFieldValue(obj.javaClass, child.name!!, obj)

                    addRawObject(child, childObject, session)
                }
            } else if (model.isArray) {
                addArray(obj, model, session)

                // TODO add remembered id!
            } else if (isArray) {
                // This is hidden array in Object field.
                // Example: Object someField = new int[] { ... }
                addArray(obj, session)

                // TODO add remembered id!
            }

            return
        }

        if (isSimpleType(model.dataType)) {
            if (!model.isTypePrimitive) {
                addSomething(obj, false)
            }
            else {
                addRawByType(model.dataType, obj)
            }
        } else if (model.isEnum) {
            addType(DataType.Enum)

            val enumVal = (obj as Enum<DataType>).ordinal
            addRawInt(enumVal)
        } else {
            throw RuntimeException("unsupported type: " + model.dataType)
        }
    }

    /**
     * Use this method if you specifically know the structure of array - it'll be more efficient.
     */
    fun addArray(array: Any, model: ObjectModelNode) {
        return addArray(array, model, ObjectSerializationSession())
    }

    /**
     * Use this method if you specifically know the structure of array - it'll be more efficient.
     */
    private fun addArray(array: Any?, model: ObjectModelNode, session: ObjectSerializationSession) {
        if (array == null) {
            addType(DataType.Null)
        } else if (!model.isSubTypePrimitive && array is Array<*>) {
            val n = array.size
            val arrayType = model.arrayType()
            beginArray(arrayType, n, false)


            if (arrayType == DataType.Unknown || arrayType == DataType.Object) {
                for (i in 0..n - 1) {
                    addObject(array[i], session)
                }
            } else if (isSimpleType(arrayType)) {
                for (el in array) {
                    if (el == null) {
                        addType(DataType.Null)
                    } else {
                        addType(arrayType)
                        addRawByType(arrayType, el)
                    }
                }
            } else if (arrayType == DataType.Enum) {
                for (i in 0..n - 1) {
                    val el = array[i]

                    if (el === null) {
                        addType(DataType.Null)
                    } else {
                        addType(DataType.EnumValue)
                        val enumVal = (el as Enum<DataType>).ordinal
                        addRawInt(enumVal)
                    }
                }
            } else if (arrayType == DataType.Array) {
                for (subArr in array) {
                    addArray(subArr, model.arrayElTypeModel(), session)
                }
            } else {
                throw RuntimeException("unsupported array type: " + arrayType)
            }
        } else if (model.isSubTypePrimitive) {
            if (model.dataSubType == DataType.Boolean)
                addArray(array as BooleanArray)
            else if (model.dataSubType == DataType.Byte)
                addArray(array as ByteArray)
            else if (model.dataSubType == DataType.Short)
                addArray(array as ShortArray)
            else if (model.dataSubType == DataType.Int)
                addArray(array as IntArray)
            else if (model.dataSubType == DataType.Long)
                addArray(array as LongArray)
            else if (model.dataSubType == DataType.Float)
                addArray(array as FloatArray)
            else if (model.dataSubType == DataType.Double)
                addArray(array as DoubleArray)
            else
                throw RuntimeException("unknown array type")
        } else {
            throw RuntimeException("unknown array type")
        }
    }
}