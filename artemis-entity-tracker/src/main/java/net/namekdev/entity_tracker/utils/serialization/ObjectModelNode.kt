package net.namekdev.entity_tracker.utils.serialization

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.determineType
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.isSimpleType
import java.util.Vector

import net.namekdev.entity_tracker.utils.ReflectionUtils

/**
 * Describes a structure of class or class field.
 * Allows to hierarchically get or set a value.

 *
 * It does not describe a structure of a specific object hierarchy.
 * Array fields will be just described as a definition,
 * independently of the array's content.
 */
class ObjectModelNode(
    private val _models: ObjectModelsCollection?,
    id: Int, // when it's null it defines a class, otherwise it's field
    var parent: ObjectModelNode?
) {

    var id = -1
    var name: String? = null

    var children: Vector<ObjectModelNode>? = null

    var networkType: Type = Type.Unknown
    var childType: Short = 0


    init {
        this.id = id
    }

    val isLeaf: Boolean
        get() = !isArray && (isEnum || networkType != Type.Object)

    val isArray: Boolean
        get() = networkType == Type.Array

    val isEnum: Boolean
        get() = networkType == Type.Enum

    val isEnumArray: Boolean
        get() = isArray && childType.toInt() == Type.Enum.ordinal

    fun arrayType(): Type {
        if (!isArray) {
            throw RuntimeException("this is not array!")
        }

        return Type.values()[childType.toInt()]
    }

    fun enumModelId(): Int {
        if (!isEnum && !isEnumArray) {
            throw RuntimeException("this is not enum field!")
        }

        return children!!.elementAt(0).id
    }

    fun setValue(targetObj: Any, treePath: IntArray?, value: Any?) {
        var targetObj = targetObj
        assert(treePath != null && treePath.size >= 1)

        val valueType = value!!.javaClass
        assert(value == null || isSimpleType(determineType(valueType)) || valueType.isEnum)

        var pathIndex = 0
        var node = this

        while (pathIndex < treePath!!.size) {
            val index = treePath[pathIndex]

            if (node.networkType == Type.Object || node.networkType == Type.Unknown /*!node.isArray() && node.children != null*/) {
                node = node.children!![index]
                val fieldName = node.name

                if (node.isLeaf) {
                    ReflectionUtils.setHiddenFieldValue(targetObj.javaClass, fieldName, targetObj, value)
                }
                else {
                    targetObj = ReflectionUtils.getHiddenFieldValue(targetObj.javaClass, fieldName, targetObj)
                }
            }
            else if (isSimpleType(node.networkType) || node.isEnum) {
                node = node.children!![index]
                assert(node.isLeaf)

                val fieldName = node.name
                ReflectionUtils.setHiddenFieldValue(targetObj.javaClass, fieldName, targetObj, value)
            }
            else if (node.isArray) {
                val array = targetObj as Array<Any>
                val arrayType = node.arrayType()

                if (arrayType == Type.Unknown || arrayType == Type.Object) {
                    assert(pathIndex < treePath.size - 1)
                    assert(node.children == null)
                    ++pathIndex

                    val arrayEl = array[pathIndex]
                    val arrayElModel = _models!!.get(arrayEl.javaClass)

                    targetObj = arrayEl
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

        if (id != obj.id || isArray != obj.isArray) {
            return false
        }

        if (name == null && obj.name != null || name != null && obj.name == null) {
            return false
        }

        if (name != null && name != obj.name) {
            return false
        }

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

    fun copyFrom(other: ObjectModelNode): ObjectModelNode {
        this.id = other.id
        this.name = other.name
        this.networkType = other.networkType
        this.childType = other.childType
        this.children = Vector(other.children!!)
        return this
    }
}