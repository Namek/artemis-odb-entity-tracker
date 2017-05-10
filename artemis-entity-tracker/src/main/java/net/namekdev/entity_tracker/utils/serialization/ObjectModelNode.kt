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
    var modelRefId: Int = -1


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

    fun enumModelId(): Int {
        if (!isEnum && !isEnumArray) {
            throw RuntimeException("this is not enum field!")
        }

        return modelRefId
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
        /*
		if (networkType != model.networkType || arrayType != model.arrayType) {
			return false;
		}

		if (children == null && model.children != null || children != null && model.children == null) {
			return false;
		}

		if (children != null) {
			if (children.size() != model.children.size()) {
				return false;
			}

			for (int i = 0, n = children.size(); i < n; ++i) {
				// TODO handle cyclic checks here!
//				if (children.get(i) != model.children.get(i)) {
//				if (!children.get(i).equals(model.children.get(i))) {
				if (children.get(i).id != model.children.get(i).id) {
					return false;
				}
			}
		}*/

        return true
    }

    private fun equals(obj: ObjectModelNode, passedVisitedNodes: ArrayList<ObjectModelNode>?): Boolean {
        if (id != obj.id || isArray != obj.isArray)
            return false

        if (name == null && obj.name != null || name != null && obj.name == null)
            return false

        if (name != null && name != obj.name)
            return false

        if (dataType != obj.dataType || dataSubType != obj.dataSubType)
            return false

        if (isTypePrimitive != obj.isTypePrimitive || isSubTypePrimitive != obj.isSubTypePrimitive)
            return false

        if (children == null && obj.children != null || children != null && obj.children == null)
            return false

        val children = this.children
        if (children != null) {
            val otherChildren = obj.children!!

            if (children.size != otherChildren.size) {
                return false
            }

            val visitedNodes = passedVisitedNodes ?: ArrayList()
            for (i in 0..children.size-1) {
                val a = children[i]
                val b = otherChildren[i]

                val hasA = visitedNodes.contains(a)
                val hasB = visitedNodes.contains(b)

                if (!hasA) {
                    visitedNodes.add(a)
                }

                if (!hasB) {
                    visitedNodes.add(b)
                }

                if (!hasA || !hasB) {
                    if (!a.equals(b, visitedNodes))
                        return false
                }
            }
        }

        return true
    }

    fun copyFrom(other: ObjectModelNode): ObjectModelNode {
        this.id = other.id
        this.name = other.name
        this.dataType = other.dataType
        this.dataSubType = other.dataSubType
        this.isTypePrimitive = other.isTypePrimitive
        this.isSubTypePrimitive = other.isSubTypePrimitive
        this.children = Vector(other.children!!)
        this.enumValue = other.enumValue
        return this
    }
}
