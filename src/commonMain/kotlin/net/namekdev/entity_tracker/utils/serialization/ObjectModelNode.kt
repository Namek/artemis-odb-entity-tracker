package net.namekdev.entity_tracker.utils.serialization

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*


/**
 * Describes a structure of class or class field.
 * Allows to hierarchically get or set a value.
 *
 * It does not describe a structure of a specific object hierarchy.
 * AutoSizedArray fields will be just described as a definition,
 * independently of the array's content.
 */
open class ObjectModelNode(
    id: Int,

    // when it's null it defines a class, otherwise it's field
    var parent: ObjectModelNode?
) {
    var id = -1
    var name: String? = null

    var children: Array<ObjectModelNode>? = null

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

    fun arrayDimensions(): Int {
        var depth = 1
        var m = this
        while (m.children != null) {
            m = m.children!![0]
            ++depth
        }
        return depth
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

            for (i in 0 until children.size) {
                val a = children[i]
                val b = otherChildren[i]

                if ((a == null && b != null) || (a != null && b == null)) {
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
        this.children = other.children?.copyOf()
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
        return "id=" + this.id.toString() + ": " + type + ":: name=" + (this.name ?: "")
    }

    fun extractArraySubTypeModel(): ObjectModelNode {
        val model = ObjectModelNode(-1, this)
        model.dataType = dataSubType
        model.isTypePrimitive = isSubTypePrimitive

        return model
    }
}
