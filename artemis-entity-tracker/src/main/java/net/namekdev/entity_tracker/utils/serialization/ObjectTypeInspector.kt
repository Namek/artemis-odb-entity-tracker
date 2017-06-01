package net.namekdev.entity_tracker.utils.serialization

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.determineType

import java.util.ArrayList
import java.util.Vector

import net.namekdev.entity_tracker.utils.ReflectionUtils

class ObjectTypeInspector {
    private val registeredModels = ArrayList<RegisteredModel>()
    private var lastId = 0

    private val registeredModelsAsCollection = object : ObjectModelsCollection {

        override fun size(): Int {
            return registeredModelsCount
        }

        override fun get(index: Int): ObjectModelNode {
            return getRegisteredModelByIndex(index)
        }

        override fun get(type: Class<*>): ObjectModelNode {
            return inspect(type)
        }

        override fun getById(id: Int): ObjectModelNode? {
            for (m in registeredModels) {
                if (m.model.id == id)
                    return m.model
            }

            return null
        }

        override fun add(model: ObjectModelNode) {
            throw RuntimeException("this implementation shouldn't manually add models. Inspector should do that automatically.")
        }
    }


    private class RegisteredModel {
        var type: Class<*>? = null
        lateinit var model: ObjectModelNode
        var parentType: Class<*>? = null

        var parent: RegisteredModel? = null
        var children = ArrayList<RegisteredModel>()

        override fun toString(): String {
            return model.name ?: "null"
        }
    }

    val registeredModelsCount: Int
        get() = registeredModels.size

    fun getRegisteredModelByIndex(index: Int): ObjectModelNode {
        val model = registeredModels[index]
        return model.model
    }

    fun getModelById(id: Int): ObjectModelNode? {
        for (model in registeredModels) {
            if (model.model.id == id) {
                return model.model
            }
        }

        return null
    }


    /**
     * Returns tree description of class type.
     */
    fun inspect(type: Class<*>): ObjectModelNode {
        val dataType = NetworkSerialization.determineType(type).first
        assert(dataType == DataType.Unknown || dataType == DataType.Enum)

        return inspectLevels(type, null, null, null)
    }

    private fun inspectLevels(type: Class<*>, parentType: Class<*>?, parentOfRoot: ObjectModelNode?, parentRegisteredModel: RegisteredModel?): ObjectModelNode {
        var registeredModel = findModel(type, parentType, parentOfRoot)

        if (registeredModel != null) {
            return registeredModel.model
        }

        var root: ObjectModelNode

        if (!type.isArray) {
            val fields = ReflectionUtils.getDeclaredFields(type)
                .filter { !it.name.startsWith("this$") } // cover hidden field in non-static inner class

            val model = ObjectModelNode(registeredModelsAsCollection, ++lastId, /* TODO: it was: root*/ null)
            model.dataType = DataType.Object
            model.children = Vector<ObjectModelNode>(fields.size)

            registeredModel = rememberType(type, parentType, model, parentRegisteredModel)
            root = registeredModel.model

            for (field in fields) {
                val fieldType = field.type
                var child: ObjectModelNode? = null

                if (fieldType.isArray) {
                    child = inspectArrayType(fieldType, type, registeredModel)
                }
                else {
                    val (dataType, isTypePrimitive) = NetworkSerialization.determineType(fieldType)

                    if (dataType == DataType.Unknown) {
                        val registeredChildModel = findModel(fieldType, type, root)

                        if (registeredChildModel == null) {
                            child = inspectLevels(fieldType, type, root, registeredModel)
                        }
                        else {
                            child = ObjectModelNode(registeredModelsAsCollection, ++lastId, root).copyFrom(
                                registeredChildModel.model
                            )
                            child.name = null

                            // TODO should we remember this model?
                            rememberType(fieldType, type, root, registeredModel)
                        }
                    }
                    else if (dataType == DataType.Enum) {
                        child = inspectEnum(fieldType as Class<Enum<*>>, type, registeredModel)
                    }
                    else {
                        child = ObjectModelNode(registeredModelsAsCollection, ++lastId, root)
                        child.dataType = dataType
                        child.isTypePrimitive = isTypePrimitive
                    }
                }

                // Every object field has is a unique model
                // so make sure we have a new model here! Then give it a name.
                assert(child.name == null)
                child.name = field.name

                model.children!!.addElement(child)
            }

            return model
        }
        else {
            return inspectArrayType(type, parentType, parentRegisteredModel)
        }
    }

    private fun inspectArrayType(fieldType: Class<*>, parentType: Class<*>?, parentRegisteredModel: RegisteredModel?): ObjectModelNode {
        val model = ObjectModelNode(registeredModelsAsCollection, ++lastId, parentRegisteredModel?.model)
        val registeredModel = rememberType(fieldType, parentType, model, parentRegisteredModel)

        val arrayElType = fieldType.componentType
        var (arrayType, isArrayElTypePrimitive) = determineType(arrayElType)


        if (arrayType == DataType.Enum) {
            val enumFieldModel = inspectEnum(arrayElType as Class<Enum<*>>, fieldType, registeredModel)
            model.children = Vector<ObjectModelNode>(1)
            model.children!!.addElement(enumFieldModel)
//            rememberType(arrayElType, fieldType, enumFieldModel, registeredModel)
        }
        else if (arrayElType.isArray) {
            arrayType = DataType.Array
        }
        else if (arrayType == DataType.Unknown) {
            arrayType = DataType.Object
        }

        model.dataType = DataType.Array
        model.dataSubType = arrayType
        model.isSubTypePrimitive = isArrayElTypePrimitive

        if (model.dataSubType == DataType.Array) {
            val submodel = inspectArrayType(arrayElType, fieldType, registeredModel)
            model.children = Vector()
            model.children!!.addElement(submodel)
        }

        return model
    }

    private fun inspectEnum(enumType: Class<Enum<*>>, parentType: Class<*>, parentRegisteredModel: RegisteredModel): ObjectModelNode {
        // algorithm: always create enum field definition,
        // but first check if there is a need to create
        // a model for enum type (with list of possible values)

        var registeredEnumTypeModel = findModel(enumType, null, null)

        if (registeredEnumTypeModel == null) {
            val enumTypeModel = ObjectModelNode(registeredModelsAsCollection, ++lastId, null)
            enumTypeModel.dataType = DataType.EnumDescription
            enumTypeModel.name = enumType.simpleName

            val possibleValues = enumType.enumConstants
            enumTypeModel.children = Vector<ObjectModelNode>(possibleValues.size)
            registeredEnumTypeModel = rememberType(enumType, null, enumTypeModel, null)

            for (i in possibleValues.indices) {
                val value = possibleValues[i]
                val enumValueModel = ObjectModelNode(registeredModelsAsCollection, ++lastId, enumTypeModel)
                enumValueModel.dataType = DataType.EnumValue
                enumValueModel.enumValue = value.ordinal
                enumValueModel.name = value.name
                enumTypeModel.children!!.addElement(enumValueModel)

                rememberType(null, enumType, enumValueModel, registeredEnumTypeModel)
            }
        }

        val enumFieldModel = ObjectModelNode(registeredModelsAsCollection, ++lastId, parentRegisteredModel.model)
        enumFieldModel.dataType = DataType.Enum
        enumFieldModel.children = Vector<ObjectModelNode>(1)
        enumFieldModel.children!!.addElement(registeredEnumTypeModel.model)

        rememberType(enumType, parentType, enumFieldModel, parentRegisteredModel)

        return enumFieldModel
    }

    private fun findModel(type: Class<*>?, parentType: Class<*>?, parent: ObjectModelNode?): RegisteredModel? {
        for (registered in registeredModels) {
            val sameParentModel = parent === null && registered.model.parent === null || parent !== null && parent === registered.model

            // a != null && a == b || a == null && b == null

            if (registered.type != null && registered.type == type || registered.type == null && type == null) {
                var isCyclicModel = false

                val cur = findChildType(registered, type!!)
                isCyclicModel = cur != null

                if (sameParentModel || isCyclicModel) {
                    return registered
                }
            }
        }

        return null
    }

    private fun findChildType(registered: RegisteredModel, type: Class<*>): RegisteredModel? {
        val cur = registered
        for (child in cur.children) {
            if (child.parentType == type) {
                return child
            }
            else {
                return findChildType(child, type)
            }
        }

        return null
    }

    private fun rememberType(type: Class<*>?, parentType: Class<*>?, model: ObjectModelNode, parentRegisteredModel: RegisteredModel?): RegisteredModel {
        val found = this.registeredModels.find { m-> m.model.id == model.id }

        if (found != null) {
            return found
        }

        val newModel = RegisteredModel()
        newModel.type = type
        newModel.model = model
        newModel.parent = parentRegisteredModel
        newModel.parentType = parentType

        if (parentRegisteredModel != null) {
            parentRegisteredModel.children.add(newModel)
        }

        this.registeredModels.add(newModel)
        return newModel
    }

    override fun toString(): String {
        val sb = StringBuilder("[")

        for (m in this.registeredModels) {
            sb.append("\n{\n  type: " + toString(m.type))
            sb.append("\n  parentType:" + toString(m.parentType))
            sb.append("\n  model:")

            if (m.model == null) {
                sb.append(" null\n")
            }
            else {
                sb.append("\n    id: " + m.model.id)
                sb.append("\n    dataType: " + m.model.dataType)
                sb.append("\n    dataSubType: " + m.model.dataSubType)
                sb.append("\n    name: \"" + m.model.name + "\"")
                sb.append("\n    parent:")

                if (m.model.parent != null) {
                    sb.append(" (id=" + m.model.parent!!.id + ")")
                }
                else {
                    sb.append(" null")
                }

                sb.append("\n    children")

                if (m.model.children != null) {
                    sb.append(" (" + m.model.children!!.size + ")")

                    var i = 0
                    val n = m.model.children!!.size
                    while (i < n) {
                        val node = m.model.children!![i]
                        sb.append("\n    {\n      id: " + node.id)
                        sb.append("\n      name: " + node.name!!)
                        sb.append("\n    }")
                        ++i
                    }
                }
                else {
                    sb.append(": null")
                }
            }

            sb.append("\n}")
        }

        sb.append("\n]")

        return sb.toString()
    }

    private fun toString(obj: Any?): String {
        if (obj == null)
            return "null"

        return obj.toString()
    }
}
