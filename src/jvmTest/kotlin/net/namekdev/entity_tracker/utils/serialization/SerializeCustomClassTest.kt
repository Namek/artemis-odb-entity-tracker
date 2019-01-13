package net.namekdev.entity_tracker.utils.serialization

import org.junit.Assert.*

import org.junit.Before
import org.junit.Test

import net.namekdev.entity_tracker.utils.ReflectionUtils
import net.namekdev.entity_tracker.utils.sample.ArrayTestClass
import net.namekdev.entity_tracker.utils.sample.CyclicClass
import net.namekdev.entity_tracker.utils.sample.CyclicClassIndirectly
import net.namekdev.entity_tracker.utils.sample.EnumArrayTestClass
import net.namekdev.entity_tracker.utils.sample.EnumFullTestClass
import net.namekdev.entity_tracker.utils.sample.EnumFieldTestClass
import net.namekdev.entity_tracker.utils.sample.GameObject
import net.namekdev.entity_tracker.utils.sample.GameState
import net.namekdev.entity_tracker.utils.sample.TestEnum
import net.namekdev.entity_tracker.utils.sample.Vector2
import net.namekdev.entity_tracker.utils.sample.Vector3
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.DataType

class SerializeCustomClassTest {
    lateinit var serializer: JvmSerializer
    lateinit var deserializer: JvmDeserializer
    lateinit var inspector: ObjectTypeInspector


    @Before
    fun setup() {
        serializer = JvmSerializer()
        deserializer = JvmDeserializer()
        inspector = ObjectTypeInspector()
    }

    private fun serializeAndDeserialize(obj: Any): ValueTree {
        serializer.addObject(obj)
        val res = serializer.result
        deserializer.setSource(res.buffer, 0, res.size)
        val value = deserializer.readObject()

        return value!!
    }

    @Test
    fun inspect_vectors() {
        var model = inspector.inspect(Vector3::class.java)
        assertTrue(model.children != null && model.children!!.size == 3)
        assertEquals("x", model.children!![0].name)
        assertEquals("y", model.children!![1].name)
        assertEquals("z", model.children!![2].name)

        model = inspector.inspect(Vector2::class.java)
        assertTrue(model.children != null && model.children!!.size == 2)
        assertEquals("x", model.children!![0].name)
        assertEquals("y", model.children!![1].name)
    }

    @Test
    fun inspect_gamestate() {
        val gameState = GameState()
        gameState.objects = arrayOf(GameObject(), GameObject())
        val model = inspector.inspect(gameState.javaClass)


        // GameState
        assertEquals(DataType.Object, model.dataType)
        assertNotNull(model.children)
        assertFalse(model.isArray)
        assertEquals(1, model.children!!.size.toLong())

        // GameState.objects (GameObject[])
        val objects = model.children!!.elementAt(0)
        assertEquals("objects", objects.name)
        assertEquals(DataType.Array, objects.dataType)
        assertTrue(objects.isArray)
        assertNull(objects.children)
        assertEquals(DataType.Object, objects.arrayType())
    }

    @Test
    fun serialize_gamestate() {
        val gameState = GameState()
        gameState.objects = arrayOf(GameObject(), GameObject(), GameObject())

        serializer.addObject(gameState)

        val res = serializer.result
        deserializer.setSource(res.buffer, 0, res.size)
        val deserializedGameState = deserializer.readObject()!!

        assertNull(deserializedGameState.parent)

        // there is only one field - "objects"
        assertEquals(1, deserializedGameState.values.size.toLong())
        assert(deserializedGameState.values[0] is ValueTree)

        val objects = deserializedGameState.values[0] as ValueTree
        assertEquals(gameState.objects.size.toLong(), objects.values.size.toLong())
        assertEquals(deserializedGameState, objects.parent)

        for (i in objects.values.indices) {
            val originalGameObject = gameState.objects[i]
            val gameObject = objects.values[i] as ValueTree

            assertEquals(objects, gameObject.parent)

            // two fields: pos, size
            assertEquals(2, gameObject.values.size.toLong())
            val posField = gameObject.values[0] as ValueTree
            val sizeField = gameObject.values[1] as ValueTree

            assertEquals(gameObject, posField.parent)
            assertEquals(gameObject, sizeField.parent)

            assertEquals(3, posField.values.size.toLong())
            assertEquals(2, sizeField.values.size.toLong())

            assertEquals(originalGameObject.pos.x, posField.values[0])
            assertEquals(originalGameObject.pos.y, posField.values[1])
            assertEquals(originalGameObject.pos.z, posField.values[2])

            assertEquals(originalGameObject.size.x, sizeField.values[0])
            assertEquals(originalGameObject.size.y, sizeField.values[1])
        }
    }

    private fun assertVector3(node: ObjectModelNode, name: String) {
        assertEquals(name, node.name)
        assertEquals(DataType.Object, node.dataType)
        assertFalse(node.isArray)
        assertNotNull(node.children)

        // GameState.objects[0].pos -> x, y, z (floats)
        assertFloat(node.children!!.elementAt(0))
        assertFloat(node.children!!.elementAt(1))
        assertFloat(node.children!!.elementAt(2))
    }

    private fun assertVector2(node: ObjectModelNode, name: String) {
        assertEquals(name, node.name)
        assertEquals(DataType.Object, node.dataType)
        assertFalse(node.isArray)
        assertNotNull(node.children)

        // Vector2 -> x, y (floats)
        assertFloat(node.children!!.elementAt(0))
        assertFloat(node.children!!.elementAt(1))
    }

    private fun assertFloat(node: ObjectModelNode) {
        assertEquals(DataType.Float, node.dataType)
        assertFalse(node.isArray)
        assertNull(node.children)
    }

    @Test
    fun deserialize_vector3() {
        testVector3(inspector)
    }

    private fun testVector3(inspector: ObjectTypeInspector) {
        val serializer = JvmSerializer().reset()

        val vector = Vector3(4f, 5f, 6f)
        val model = inspector.inspect(vector.javaClass)

        serializer.addDataDescriptionOrRef(model)
        serializer.addObject(model, vector)

        val buffer = serializer.result.buffer
        deserializer.setSource(buffer, 0, serializer.result.size)

        val model2 = deserializer.readDataDescription()
        assertEquals(model, model2)

        val result = deserializer.readObject(model2)

        assertEquals(3, result.values.size.toLong())
        assertEquals(vector.x, result.values[0])
        assertEquals(vector.y, result.values[1])
        assertEquals(vector.z, result.values[2])
    }

    @Test
    fun deserialize_simple_arrays() {
        val floats = arrayOf(0f, 1f, 2f)
        val strings = arrayOf("asd", "omg", "this is a test?")

        testArray(floats as Array<Any>, inspector)
        testArray(strings as Array<Any>, inspector)
    }

    private fun testArray(arr: Array<Any>, inspector: ObjectTypeInspector) {
        val serializer = JvmSerializer().reset()
        val model = inspector.inspect(arr.javaClass)
        serializer.addDataDescriptionOrRef(model)
        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val model2 = deserializer.readDataDescription()
        assertEquals(model, model2)
    }

    @Test
    fun deserialize_gamestate() {
        val gameState = GameState()
        gameState.objects = arrayOf(GameObject(), GameObject(), GameObject(), GameObject())

        val serializer = JvmSerializer(inspector).reset()
        val model = inspector.inspect(GameState::class.java)


        serializer.addDataDescriptionOrRef(model)
        serializer.addObject(model, gameState)

        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val model2 = deserializer.readDataDescription()
        assertEquals(model, model2)

        val result = deserializer.readObject(model2)

        // Test deserialized result
        assertNull(result.parent)
        val gameStateFields = result.values
        assertEquals(1, gameStateFields.size.toLong())

        val objectsField = gameStateFields[0] as ValueTree
        assertNotNull(objectsField.parent)
        assertEquals(result, objectsField.parent)

        val objects = objectsField.values
        assertEquals(gameState.objects.size.toLong(), objects.size.toLong())

        val n = objects.size
        for (i in 0..n - 1) {
            val gameObj = objects[i] as ValueTree
            assertNotNull(gameObj.parent)
            assertEquals(objectsField, gameObj.parent)
            assertEquals(2, gameObj.values.size.toLong())

            val posField = gameObj.values[0] as ValueTree
            assertNotNull(posField.parent)
            assertEquals(gameObj, posField.parent)
            val pos = posField.values

            val sizeField = gameObj.values[1] as ValueTree
            assertNotNull(sizeField.parent)
            assertEquals(gameObj, sizeField.parent)
            val size = sizeField.values

            val pos0 = gameState.objects[i].pos
            val size0 = gameState.objects[i].size

            assertEquals(pos0.x, pos[0])
            assertEquals(pos0.y, pos[1])
            assertEquals(pos0.z, pos[2])

            assertEquals(size0.x, size[0])
            assertEquals(size0.y, size[1])
        }
    }

    @Test
    fun set_values_for_object_model() {
        var model: ObjectModelNode
        val y = 55f

        val vect = Vector3(6f, 5f, 4f)
        model = inspector.inspect(vect.javaClass)
        model.setValue(vect, intArrayOf(1)/*vert.y*/, y)
        assertEquals(y, vect.y, 0.01f)

        val obj = GameObject()
        model = inspector.inspect(obj.javaClass)
        model.setValue(obj, intArrayOf(1, 1)/*obj.size.y*/, y)
        assertEquals(y, obj.size.y, 0.01f)

        val gs = GameState()
        gs.objects = arrayOf(GameObject(), GameObject(), GameObject())
        model = inspector.inspect(gs.javaClass)
        model.setValue(gs, intArrayOf(0, 2, 0, 0, 1)/*gs.objects[2].pos.y*/, y)
        assertEquals(y, gs.objects[2].pos.y, 0.000001f)
    }

    @Test
    fun inspect_and_update_enum_fields() {
        val obj = EnumFullTestClass()
        val model = inspector.inspect(obj.javaClass)

        val newVal = TestEnum.Third
        model.setValue(obj, intArrayOf(0), newVal)
        assertEquals(newVal, obj.enumUndefined)

        assertEquals(TestEnum.First, obj.enumValued)
        model.setValue(obj, intArrayOf(1), newVal)
        assertEquals(newVal, obj.enumValued)
    }

    @Test
    fun inspect_and_update_enum_fields_by_null() {
        val obj = EnumFullTestClass()
        val model = inspector.inspect(obj.javaClass)

        model.setValue(obj, intArrayOf(0), null)
        assertEquals(null, obj.enumUndefined)

        assertEquals(TestEnum.First, obj.enumValued)
        model.setValue(obj, intArrayOf(1), null)
        assertEquals(null, obj.enumValued)
    }

    @Test
    fun inspect_and_update_enum_fields_by_value_provided_as_integer()
    {
        val obj = EnumFullTestClass()
        val model = inspector.inspect(obj.javaClass)

        val newVal = TestEnum.Third.ordinal
        model.setValue(obj, intArrayOf(0), newVal)
        assertEquals(newVal, (obj.enumUndefined as TestEnum).ordinal)

        assertEquals(TestEnum.First, obj.enumValued)
        model.setValue(obj, intArrayOf(1), newVal)
        assertEquals(newVal, (obj.enumValued as TestEnum).ordinal)
    }

    @Test
    fun inspect_and_update_enum_fields_by_value_provided_as_short_int()
    {
        val obj = EnumFullTestClass()
        val model = inspector.inspect(obj.javaClass)

        val newVal = TestEnum.Third.ordinal.toShort()
        model.setValue(obj, intArrayOf(0), newVal)
        assertEquals(newVal, (obj.enumUndefined as TestEnum).ordinal.toShort())

        assertEquals(TestEnum.First, obj.enumValued)
        model.setValue(obj, intArrayOf(1), newVal)
        assertEquals(newVal, (obj.enumValued as TestEnum).ordinal.toShort())
    }

    @Test
    fun inspect_and_update_enum_fields_by_value_provided_as_byte()
    {
        val obj = EnumFullTestClass()
        val model = inspector.inspect(obj.javaClass)

        val newVal = TestEnum.Third.ordinal.toByte()
        model.setValue(obj, intArrayOf(0), newVal)
        assertEquals(newVal, (obj.enumUndefined as TestEnum).ordinal.toByte())

        assertEquals(TestEnum.First, obj.enumValued)
        model.setValue(obj, intArrayOf(1), newVal)
        assertEquals(newVal, (obj.enumValued as TestEnum).ordinal.toByte())
    }

    @Test
    fun inspect_names_of_enum_fields() {
        val obj = EnumFullTestClass()
        val model = inspector.inspect(obj.javaClass)
        val enumFieldModel = model.children!!.elementAt(1)
        val enumArrayFieldModel = model.children!!.elementAt(2)

        // check valued field
        checkEnumFieldInspection(enumFieldModel)

        // check array field
        assert(enumArrayFieldModel.isArray)
        assertEquals(DataType.Enum, enumArrayFieldModel.arrayType())

        checkEnumFieldInspection(enumArrayFieldModel.children!!.elementAt(0))
    }

    private fun checkEnumFieldInspection(enumFieldModel: ObjectModelNode) {
        val possibleValues = TestEnum::class.java.enumConstants

        assertEquals(DataType.Enum, enumFieldModel.dataType)
        val enumDescrModel = inspector.getModelById(enumFieldModel.enumModelId())
        assertEquals(TestEnum::class.java.simpleName, enumDescrModel!!.name)

        assertEquals(possibleValues.size.toLong(), enumDescrModel.children!!.size.toLong())
        for (i in possibleValues.indices) {
            val valModel = enumDescrModel.children!!.elementAt(i)
            val value = possibleValues[i]
            assertEquals(value.name, valModel.name)
            assertEquals(value.ordinal, valModel.enumValue)
        }
    }

    @Test
    fun serialize_enum_fields() {
        val obj = EnumFieldTestClass()
        val value = serializeAndDeserialize(obj)

        assertEquals(obj.enumUndefined, value.values[0])
        assertEquals((obj.enumValued as TestEnum).ordinal, value.values[1])
    }

    @Test
    fun serialize_enum_array() {
        val obj = EnumArrayTestClass()
        val value = serializeAndDeserialize(obj)

        assertEquals((obj.enums[0] as TestEnum).ordinal, (value.values[0] as ValueTree).values[0])
        assertEquals((obj.enums[1] as TestEnum).ordinal, (value.values[0] as ValueTree).values[1])
        assertEquals((obj.enums[2] as TestEnum).ordinal, (value.values[0] as ValueTree).values[2])
    }

    @Test
    fun serialize_all_enums() {
        val obj = EnumFullTestClass()
        val value = serializeAndDeserialize(obj)

        assertEquals(obj.enumUndefined, value.values[0])
        assertEquals((obj.enumValued as TestEnum).ordinal, value.values[1])
        assertEquals((obj.enums[0] as TestEnum).ordinal, (value.values[2] as ValueTree).values[0])
        assertEquals((obj.enums[1] as TestEnum).ordinal, (value.values[2] as ValueTree).values[1])
        assertEquals((obj.enums[2] as TestEnum).ordinal, (value.values[2] as ValueTree).values[2])

        val deserializedModels = ReflectionUtils.getHiddenFieldValue(deserializer.javaClass.superclass, "_models", deserializer) as ObjectModelsCollection
        val deserializedModelCount = deserializedModels.size()
        assertEquals(serializer.inspector.registeredModelsCount, deserializedModelCount)
    }

    @Test
    fun inspect_cyclic_reference() {
        val obj = CyclicClass()
        obj.other = CyclicClass()
        obj.other.other = obj

        val model = inspector.inspect(obj.javaClass)
        assertEquals(2, model.children!!.size.toLong())
        assertNotEquals(model.id.toLong(), model.children!!.elementAt(0).id.toLong())
        assert(model !== model.children!!.elementAt(0))
    }

    @Test
    fun inspect_indirectly_cyclic_class() {
        val obj = CyclicClassIndirectly()
        obj.obj = CyclicClassIndirectly.OtherClass()
        obj.obj.obj = obj
        obj.obj.obj.obj = CyclicClassIndirectly.OtherClass()

        val model = inspector.inspect(obj.javaClass)
        assertEquals(2, model.children!!.size.toLong())
        val fieldModel = model.children!!.elementAt(0)
        assertNotEquals(model.id.toLong(), fieldModel.children!!.elementAt(0).id.toLong())
    }

    @Test
    fun inspect_indirectly_cyclic_class_in_array() {
        val obj = CyclicClassIndirectly()
        obj.arr = CyclicClassIndirectly.ArrayClass()
        obj.arr.objs = arrayOf(obj)

        val model = inspector.inspect(obj.javaClass)
        assertEquals(2, model.children!!.size.toLong())
        val arrFieldModel = model.children!!.elementAt(1)
        val objsFieldModel = arrFieldModel.children!!.elementAt(0)
        assert(objsFieldModel.isArray)
        assertNotEquals(model.id.toLong(), objsFieldModel.id.toLong())
        assertEquals(DataType.Object, objsFieldModel.arrayType())

        // Note: the dependency is indeed cyclic, however array of `CyclicClassIndirectly`
        // is just an array, it could contain anything else that inherits this class.
        // Thus, we can't assume that childType of this filed is a concrete type,
        // it's rather just an Object.
        // Therefore, we do NOT assert: model.id == objsFieldModel.arrayType()
    }

    @Test
    fun fail_to_inspect_simple_types() {
        val testSubjects = arrayOf(
            "asd", // String
            5// Integer
        )

        for (testSubject in testSubjects) {
            var model: ObjectModelNode? = null
            try {
                model = inspector.inspect(testSubject.javaClass)
            }
            catch (exc: Error) {
            } finally {
                assertEquals(null, model)
            }
        }
    }

    @Test
    fun inspect_array_field_containing_various_objects() {
        val obj = ArrayTestClass()
        obj.array = arrayOf(Vector2(5f, 6f), Vector3(7f, 8f, 9f))
        val model = inspector.inspect(obj.javaClass)
        assert(!model.isArray)
        assertEquals(1, model.children!!.size.toLong())

        val arrayModel = model.children!!.elementAt(0)
        assert(arrayModel.isArray)
        assert(arrayModel.children == null)
        assertEquals("array", arrayModel.name)
    }

    @Test
    fun deserialize_array_of_various_objects() {
        val v2 = Vector2(5f, 6f)
        val v3 = Vector3(7f, 8f, 9f)
        val array = arrayOf(v2, v3)

        val model = inspector.inspect(array.javaClass)
        assert(model.isArray)
        assertEquals(null, model.children)
        assertEquals(DataType.Object, model.arrayType())

        // it's array of a priori unknown objects so models for Vector2 and Vector3
        // are not available at this point:
        assertEquals(1, inspector.registeredModelsCount.toLong())
        var v2Model = inspector.getModelById(model.id + 1)
        var v3Model = inspector.getModelById(model.id + 2)

        assertNull(v2Model)
        assertNull(v3Model)


        // but now, we will serialize the array, then check for Vector2/3 models:
        serializer = JvmSerializer(inspector).reset()
        serializer.addArray(array)
        assertEquals(3 /*array + Vectors without fields */, inspector.registeredModelsCount.toLong())

        // Vector2
        v2Model = inspector.inspect(Vector2::class.java)
        assertEquals(2, v2Model.children!!.size.toLong())
        assertEquals("x", v2Model.children!!.elementAt(0).name)
        assertEquals("y", v2Model.children!!.elementAt(1).name)

        // Vector3
        v3Model = inspector.inspect(Vector3::class.java)
        assertEquals(3, v3Model.children!!.size.toLong())
        assertEquals("x", v3Model.children!!.elementAt(0).name)
        assertEquals("y", v3Model.children!!.elementAt(1).name)
        assertEquals("z", v3Model.children!!.elementAt(2).name)

        // now deserialize the array
        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val arr = deserializer.readArray()!!
        assertEquals(array.size.toLong(), arr.values.size.toLong())

        val v2d = arr.values[0] as ValueTree
        val v3d = arr.values[1] as ValueTree

        assertEquals(2, v2d.values.size.toLong())
        assertEquals(3, v3d.values.size.toLong())

        assert(v2.x == v2d.values[0] as Float)
        assert(v2.y == v2d.values[1] as Float)
        assert(v3.x == v3d.values[0] as Float)
        assert(v3.y == v3d.values[1] as Float)
        assert(v3.z == v3d.values[2] as Float)
    }

    @Test
    fun inspect_class_without_parent_class_this() {
        val model = inspector.inspect(InnerClass_GameState::class.java)
        assertEquals(2, model.children!!.size)
    }

    inner class InnerClass_GameState {
        var objects: Array<GameObject>? = null
        var omg: Boolean = false
    }

    inner class InnerClass_GameObject {
        var pos = Vector3(1f, 2f, 3f)
        var size = Vector2(10f, 5f)
    }
}
