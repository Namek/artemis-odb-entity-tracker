package net.namekdev.entity_tracker.utils.serialization

import net.namekdev.entity_tracker.model.determineType
import net.namekdev.entity_tracker.utils.ReflectionUtils


actual fun ObjectModelNode.setValue(targetObj: Any, treePath: IntArray?, value: Any?) {
    var traverseObj: Any? = targetObj
    net.namekdev.entity_tracker.utils.assert(treePath != null && treePath.size >= 1)

    val valueType = value?.javaClass
    net.namekdev.entity_tracker.utils.assert(
        value == null
        || NetworkSerialization.isSimpleType(determineType(valueType!!).first)
        || valueType.isEnum
    )

    var pathIndex = 0
    var node = this

    while (pathIndex < treePath!!.size) {
        val index = treePath[pathIndex]

        if (node.dataType == NetworkSerialization.DataType.Object || node.dataType == NetworkSerialization.DataType.Unknown /*!node.isArray() && node.children != null*/) {
            node = node.children!![index]
            val fieldName = node.name

            if (node.isLeaf) {
                ReflectionUtils.setHiddenFieldValue(traverseObj!!.javaClass, fieldName!!, traverseObj, value)
            }
            else {
                traverseObj = ReflectionUtils.getHiddenFieldValue(traverseObj!!.javaClass, fieldName!!, traverseObj)
            }
        }
        else if (NetworkSerialization.isSimpleType(node.dataType) || node.isEnum) {
            node = node.children!![index]
            net.namekdev.entity_tracker.utils.assert(node.isLeaf)

            val fieldName = node.name
            ReflectionUtils.setHiddenFieldValue(traverseObj!!.javaClass, fieldName!!, traverseObj, value)
        }
        else if (node.isArray) {
            val array = traverseObj as Array<Any?>
            val arrayType = node.arrayType()

            if (arrayType == NetworkSerialization.DataType.Unknown || arrayType == NetworkSerialization.DataType.Object) {
                net.namekdev.entity_tracker.utils.assert(pathIndex < treePath.size - 1)
                net.namekdev.entity_tracker.utils.assert(node.children == null)
                ++pathIndex

                // @Note: This may need some attention. `arrayEl` could be null, then model couldn't be defined.
                val arrayEl = array[pathIndex]
                val arrayElModel = _models!!.get(arrayEl!!::class)

                traverseObj = arrayEl
                node = arrayElModel
            }
            else if (NetworkSerialization.isSimpleType(arrayType)) {
                net.namekdev.entity_tracker.utils.assert(pathIndex == treePath.size - 1)
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