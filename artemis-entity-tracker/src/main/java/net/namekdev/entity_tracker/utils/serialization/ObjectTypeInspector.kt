package net.namekdev.entity_tracker.utils.serialization

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.determineType
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.Companion.isSimpleType

import java.util.ArrayList
import java.util.Vector

import com.artemis.utils.reflect.ClassReflection
import com.artemis.utils.reflect.Field
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
                if (m.model!!.id == id)
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
    }

    val registeredModelsCount: Int
        get() = registeredModels.size

    fun getRegisteredModelByIndex(index: Int): ObjectModelNode {
        val model = registeredModels[index]
        return model.model!!
    }

    fun getModelById(id: Int): ObjectModelNode? {
        for (model in registeredModels) {
            if (model.model!!.id == id) {
                return model.model
            }
        }

        return null
    }


    /**
     * Returns tree description of class type.
     */
    fun inspect(type: Class<*>): ObjectModelNode {
        assert(NetworkSerialization.determineType(type) == DataType.Unknown)

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
            model.networkType = DataType.Object
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
                    val networkType = NetworkSerialization.determineType(fieldType)

                    if (networkType == Type.Unknown) {
                        val registeredChildModel = findModel(fieldType, type, root)

                        if (registeredChildModel == null) {
                            child = inspectLevels(fieldType, type, root, registeredModel)
                        }
                        else {
                            child = ObjectModelNode(registeredModelsAsCollection, ++lastId, root).copyFrom(
                                registeredChildModel.model!!
                            )
                            child.name = null

                            // TODO should we remember this model?
                            rememberType(fieldType, type, root, registeredModel)
                        }
                    }
                    else if (networkType == DataType.Enum) {
                        child = inspectEnum(fieldType as Class<Enum<*>>, type, registeredModel)
                    }
                    else {
                        child = ObjectModelNode(registeredModelsAsCollection, ++lastId, root)
                        child.networkType = networkType
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
        var arrayType = determineType(arrayElType)


        if (arrayType == DataType.Enum) {
            //			ObjectModelNode enumFieldModel = inspectEnum((Class<Enum>) arrayElType, fieldType, registeredModel);
            //			model.children = new Vector<>(1);
            //			model.children.addElement(enumFieldModel);
            //			rememberType(arrayElType, fieldType, enumFieldModel, registeredModel);

            //			arrayType = Type.Unknown;
        }
        else if (!isSimpleType(arrayType)) {
            //			model = inspectLevels(arrayElType, root);
            //
            //			if (model.networkType == TYPE_TREE) {
            //				arrayType = TYPE_TREE;
            //			}

            arrayType = if (arrayElType.isArray) DataType.Array else DataType.Object
        }// TODO probably that should inspect deeper anyway!

        model.networkType = DataType.Array
        model.childType = arrayType.ordinal.toShort()

        return model
    }

    private fun inspectEnum(enumType: Class<Enum<*>>, parentType: Class<*>, parentRegisteredModel: RegisteredModel): ObjectModelNode {
        // algorithm: will create enum field definition anyway,
        // but first check if there is a need to create a model for enum type (list of possible values)

        var registeredEnumTypeModel = findModel(enumType, null, null)

        if (registeredEnumTypeModel == null) {
            val enumTypeModel = ObjectModelNode(registeredModelsAsCollection, ++lastId, null)
            enumTypeModel.networkType = Type.EnumDescription
            enumTypeModel.name = enumType.simpleName

            val possibleValues = enumType.enumConstants
            enumTypeModel.children = Vector<ObjectModelNode>(possibleValues.size)
            registeredEnumTypeModel = rememberType(enumType, null, enumTypeModel, null)

            for (i in possibleValues.indices) {
                val enumValueModel = ObjectModelNode(registeredModelsAsCollection, ++lastId, enumTypeModel)
                enumValueModel.networkType = Type.EnumValue
                val value = possibleValues[i]

                // Note: we cut bytes here, it's not nice but let's believe that no one creates enums greater than 127.
                enumValueModel.childType = value.ordinal.toShort()
                enumValueModel.name = value.name
                enumTypeModel.children!!.addElement(enumValueModel)

                rememberType(null, enumType, enumValueModel, registeredEnumTypeModel)
            }
        }

        val enumFieldModel = ObjectModelNode(registeredModelsAsCollection, ++lastId, parentRegisteredModel.model)
        enumFieldModel.networkType = Type.Enum

        val enumModelRef = ObjectModelNode(registeredModelsAsCollection, registeredEnumTypeModel.model!!.id, enumFieldModel)
        enumFieldModel.children = Vector<ObjectModelNode>(1)
        enumFieldModel.children!!.addElement(enumModelRef)

        rememberType(enumType, parentType, enumFieldModel, parentRegisteredModel)

        return enumFieldModel
    }

    private fun findModel(type: Class<*>?, parentType: Class<*>?, parent: ObjectModelNode?): RegisteredModel? {
        for (registered in registeredModels) {
            val sameParentModel = parent == null && registered.model!!.parent == null || parent != null && parent == registered.model

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
                sb.append("\n    id: " + m.model!!.id)
                sb.append("\n    networkType: " + m.model!!.networkType)
                sb.append("\n    childType: " + m.model!!.childType)
                sb.append("\n    name: \"" + m.model!!.name + "\"")
                sb.append("\n    parent:")

                if (m.model!!.parent != null) {
                    sb.append(" (id=" + m.model!!.parent!!.id + ")")
                }
                else {
                    sb.append(" null")
                }

                sb.append("\n    children")

                if (m.model!!.children != null) {
                    sb.append(" (" + m.model!!.children!!.size + ")")

                    var i = 0
                    val n = m.model!!.children!!.size
                    while (i < n) {
                        val node = m.model!!.children!![i]
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
