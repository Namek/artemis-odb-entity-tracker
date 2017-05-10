package net.namekdev.entity_tracker.utils.serialization

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.determineType
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.isSimpleType

import net.namekdev.entity_tracker.utils.ReflectionUtils
import java.util.*

/**
 * Describes a structure of class or class field.
 * Allows to hierarchically get or set a value.
 *
 * It does not describe a structure of a specific object hierarchy.
 * AutoSizedArray fields will be just described as a definition,
 * independently of the array's content.
 */
class ObjectModelNode(
    private val _models: ObjectModelsCollection?,
    id: Int,

    // when it's null it defines a class, otherwise it's field
    var parent: ObjectModelNode?
) {
    var id = -1
    var name: String? = null

    var children: Vector<ObjectModelNode>? = null

    var dataType: DataType = DataType.Undefined
    var dataSubType: DataType = DataType.Undefined


    /** Determines [dataType]. Is it primitive type? Otherwise, it's objectType */
    var isTypePrimitive = false

    /** Determines [dataSubType] Is it primitive type? Otherwise, it's objectType */
    var isSubTypePrimitive = false

    var enumValue: Int = 0


    init {
        this.id = id
    }

    val isLeaf: Boolean
        get() = !isArray && (isEnum || dataType != DataType.Object)

    val isArray: Boolean
        get() = dataType == DataType.Array

    val isEnum: Boolean
        get() = dataType == DataType.Enum

    val isEnumArray: Boolean
        get() = isArray && dataSubType == DataType.Enum

    fun arrayType(): DataType {
        if (!isArray) {
            throw RuntimeException("this is not array!")
        }

        return dataSubType
    }

    fun arrayElTypeModel(): ObjectModelNode {
        if (!isArray) {
            throw RuntimeException("this is not array!")
        }

        return children!![0]
    }

    fun enumModel(): ObjectModelNode {
        if (!isEnum && !isEnumArray) {
            throw RuntimeException("this is not enum field!")
        }

        return children!![0]
    }

    fun enumModelId(): Int {
       return enumModel().id
    }

    fun setValue(targetObj: Any, treePath: IntArray?, value: Any?) {
        var traverseObj: Any? = targetObj
        assert(treePath != null && treePath.size >= 1)

        val valueType = value?.javaClass
        assert(value == null || isSimpleType(determineType(valueType!!).first) || valueType.isEnum)

        var pathIndex = 0
        var node = this

        while (pathIndex < treePath!!.size) {
            val index = treePath[pathIndex]

            if (node.dataType == DataType.Object || node.dataType == DataType.Unknown /*!node.isArray() && node.children != null*/) {
                node = node.children!![index]
                val fieldName = node.name

                if (node.isLeaf) {
                    ReflectionUtils.setHiddenFieldValue(traverseObj!!.javaClass, fieldName!!, traverseObj, value)
                }
                else {
                    traverseObj = ReflectionUtils.getHiddenFieldValue(traverseObj!!.javaClass, fieldName!!, traverseObj)
                }
            }
            else if (isSimpleType(node.dataType) || node.isEnum) {
                node = node.children!![index]
                assert(node.isLeaf)

                val fieldName = node.name
                ReflectionUtils.setHiddenFieldValue(traverseObj!!.javaClass, fieldName!!, traverseObj, value)
            }
            else if (node.isArray) {
                val array = traverseObj as Array<Any?>
                val arrayType = node.arrayType()

                if (arrayType == DataType.Unknown || arrayType == DataType.Object) {
                    assert(pathIndex < treePath.size - 1)
                    assert(node.children == null)
                    ++pathIndex

                    // @Note: This may need some attention. `arrayEl` could be null, then model couldn't be defined.
                    val arrayEl = array[pathIndex]
                    val arrayElModel = _models!!.get(arrayEl!!.javaClass)

                    traverseObj = arrayEl
                    node = arrayElModel
                }
                else if (isSimpleType(arrayType)) {
                    assert(pathIndex == treePath.size - 1)
                    ++pathIndex

                    array[pathIndex] = value
                }
                else {
                    throw RuntimeException("unsupported operation")
                }

                //				if (node.arrayType == Type.Object || node.arrayType == TYPE_UNKNOWN) {
                //					assert pathIndex < treePath.length;
                //					targetObj = array[index];
                //					index = treePath[++pathIndex];
                //					node = node.children.get(index);
                //					String fieldName = node.name;
                //					targetObj = ReflectionUtils.getHiddenFieldValue(targetObj.getClass(), fieldName, targetObj);
                //				}
                //				else {
                //					assert pathIndex+1 < treePath.length;
                //					array[pathIndex] = value;
                //				}
            }
            else {
                throw RuntimeException("oops, logical error")
            }

            pathIndex += 1
        }
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is ObjectModelNode) {
            return false
        }

        return equals(obj, null)
    }

    private fun equals(obj: ObjectModelNode, passedVisitedNodes: ArrayList<ObjectModelNode>?): Boolean {
        var ret = true

        if (id != obj.id || isArray != obj.isArray)
            ret = false
        else if (name == null && obj.name != null || name != null && obj.name == null)
            ret = false
        else if (name != null && !name.equals(obj.name))
            ret = false
        else if (dataType != obj.dataType || dataSubType != obj.dataSubType)
            ret = false
        else if (isTypePrimitive != obj.isTypePrimitive || isSubTypePrimitive != obj.isSubTypePrimitive)
            ret = false
        else if (children == null && obj.children != null || children != null && obj.children == null)
            ret = false
        else if (enumValue != obj.enumValue)
            ret = false
        else if (this.children != null) {
            val children = this.children!!
            val otherChildren = obj.children!!

            if (children.size != otherChildren.size) {
                return false
            }

            val visitedNodes = passedVisitedNodes ?: ArrayList()

            for (i in 0..children.size-1) {
                val a = children[i]
                val b = otherChildren[i]

                if (a == null && b != null || a != null && b == null) {
                    ret = false
                    break
                }

                val hasA = visitedNodes.find { node -> node === a } != null
                val hasB = visitedNodes.find { node -> node === b } != null

                if (!hasA) {
                    visitedNodes.add(a)
                }

                if (!hasB) {
                    visitedNodes.add(b)
                }

                if (!hasA || !hasB) {
                    if (!a.equals(b, visitedNodes)) {
                        ret = false
                        break
                    }
                }
            }
        }

        return ret
    }

    fun copyFrom(other: ObjectModelNode): ObjectModelNode {
        this.id = other.id
        this.name = other.name
        this.dataType = other.dataType
        this.dataSubType = other.dataSubType
        this.isTypePrimitive = other.isTypePrimitive
        this.isSubTypePrimitive = other.isSubTypePrimitive
        this.children = if (other.children != null) Vector(other.children) else null
        this.enumValue = other.enumValue
        return this
    }

    override fun toString(): String {
        val type: String = (
            if (this.dataType == DataType.Array) {
                var subType = dataSubType.toString()
                if (isSubTypePrimitive)
                    subType = subType.toLowerCase()

                "Array<" + subType + ">"
            }
            else this.dataType.toString()
        )
        return "id=" + this.id.toString() + ": " + type + ":: name=" + this.name ?: ""
    }
}
