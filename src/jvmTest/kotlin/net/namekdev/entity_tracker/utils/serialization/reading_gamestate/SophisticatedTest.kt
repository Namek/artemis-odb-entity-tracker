package net.namekdev.entity_tracker.utils.serialization.reading_gamestate

import net.namekdev.entity_tracker.utils.sample.CyclicReferencesHidden
import net.namekdev.entity_tracker.utils.sample.DeepArray
import net.namekdev.entity_tracker.utils.sample.DeepArrayImplicit
import net.namekdev.entity_tracker.utils.sample.EnumFullTestClass
import net.namekdev.entity_tracker.utils.sample.OuterClass
import net.namekdev.entity_tracker.utils.sample.OuterClass2
import net.namekdev.entity_tracker.utils.sample.RepeatingModelsTestClass
import net.namekdev.entity_tracker.utils.serialization.*
import net.namekdev.entity_tracker.utils.serialization.DataType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 *
 */
class SophisticatedTest {
    lateinit var serializer: JvmSerializer
    lateinit var deserializer: JvmDeserializer
    lateinit var inspector: ObjectTypeInspector


    @Before
    fun setup() {
        serializer = JvmSerializer().beginPacket()
        deserializer = JvmDeserializer()
        inspector = serializer.inspector
    }

    @Test
    fun compare_two_enum_class_models() {
        // create first model, serialize it and retrieve a potential copy of it
        val model = inspector.inspect(EnumFullTestClass::class.java)
        serializer.addDataDescriptionOrRef(model)
        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)
        val model2 = deserializer.readDataDescription()

        // that should be equal
        assertTrue(model2.equals(model))

        // now, let's modify one of them and compare again, should be different
        val enumDescrModel = model2.children!![2].children!![0].children!![0]
        val thirdEnumValue = enumDescrModel.children!![2]
        val newThirdEnumValue = ObjectModelNode(0, null).copyFrom(thirdEnumValue)
        newThirdEnumValue.enumValue = 999
        enumDescrModel.children!!.set(2, newThirdEnumValue)

        assertFalse(model2.equals(model))
    }

    @Test
    fun deserialize_inner_game_state() {
        val gameState = GameState()
//        gameState.objects = arrayOf(GameObject(), GameObject(), GameObject(), GameObject())

        val serializer = JvmSerializer().beginPacket()
        val inspector = serializer.inspector
        val model = inspector.inspect(GameState::class.java)


        serializer.addDataDescriptionOrRef(model)
        serializer.addObject(model, gameState)

        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val model2 = deserializer.readDataDescription()
        assertTrue(model2.equals(model))

        val result = deserializer.readObject(model2)
        assertTrue(result.model!!.equals(model))
        assertTrue(result.model!!.equals(model2))
    }

    @Test
    fun deserializeInnerGameState_auto() {
        val gameState = GameState()
        gameState.objects = arrayOf(GameObject(), GameObject(), GameObject(), GameObject())

        val serializer = JvmSerializer().beginPacket()
        val model = inspector.inspect(GameState::class.java)
        serializer.addObject(gameState)
        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val result = deserializer.readObject()!!
        assertTrue(result.model!!.equals(model))

        // boolean omg
        assertEquals(gameState.omg, result.values[1] as Boolean)

        // arrayOf GameObjects
        val objects = result.values[0] as ValueTree

        assert(objects.model!!.isArray)
        for (i in 0..gameState.objects!!.size-1) {
            val origObj = gameState.objects!![i]
            val obj = objects.values[i] as ValueTree

            val objPos = obj.values[0] as ValueTree
            assertEquals(origObj.pos.x, objPos.values[0])
            assertEquals(origObj.pos.y, objPos.values[1])
            assertEquals(origObj.pos.z, objPos.values[2])

            val objSize = obj.values[1] as ValueTree
            assertEquals(origObj.size.x, objSize.values[0])
            assertEquals(origObj.size.y, objSize.values[1])
        }
    }

    @Test
    fun serialize_cyclic_reference() {
        val node = CyclicReferencesHidden()
        node.children = arrayOf(node)

        serializer.addObject(node)

        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val result = deserializer.readObject()!!
        val children = result.values[0] as ValueTree
        assert(children.values[0] === result)
    }

    @Test
    fun inspect_repeating_models() {
        val pureRepeatedModel = serializer.inspector.inspect(RepeatingModelsTestClass.RepeatedClass::class.java)
        val model = serializer.inspector.inspect(RepeatingModelsTestClass::class.java)

        val aModel = model.ch(0)
        val bModel = model.ch(1)
        val cModel = model.ch(2)
        val dModel = cModel.ch(0)

        // both fields are different models
        assertNotEquals(aModel, bModel)

        // neither of those fields model is same as a model for the type
        assertNotEquals(aModel, pureRepeatedModel)
        assertNotEquals(bModel, pureRepeatedModel)

        // model in another subclass is also different
        assertNotEquals(dModel, aModel)
        assertNotEquals(dModel, bModel)
        assertNotEquals(dModel, pureRepeatedModel)
    }

    @Test
    fun inspect_inner_class() {
        val outerClassModel = serializer.inspector.inspect(OuterClass::class.java)
        assertEquals(1, outerClassModel.children!!.size)
        assertNull(outerClassModel.parent)

        val aModel = outerClassModel.children!![0]
        assertEquals(1, aModel.children!!.size)

        val bModel = aModel.children!![0]
        assertEquals(1, bModel.children!!.size)

        val cModel = bModel.children!![0]
        assertEquals(2, cModel.children!!.size)

        val dModel = cModel.children!![1]
        assertEquals(DataType.Boolean, dModel.dataType)

        val _aModel = cModel.children!![0]

        assertNotEquals(outerClassModel.id, _aModel.id)
        assertEquals(cModel, _aModel.parent)
    }

    @Test
    fun deserialize_inner_class() {
        val obj = OuterClass()
        serializer.addObject(obj)
        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val result = deserializer.readObject()!!
        val a = (result.values[0] as ValueTree)
        val b = (a.values[0] as ValueTree)
        val c = (b.values[0] as ValueTree)
        val d = c.values[1] as Boolean
        assertEquals(true, d)
        val _a = (c.values[0] as ValueTree)

        // models has to be different because model of OuterClass is a different thing than a field of type OuterClass
        assert(_a !== a)
        assertNotEquals(a.model, _a.model)
    }

    @Test
    fun inspect_inner_class_2() {
        val model = serializer.inspector.inspect(OuterClass2::class.java)
        val aModel = model.ch(0)
        val bModel = model.ch(1)
        val cModel = aModel.ch(0)
        val dModel = cModel.ch(0)
        val a1Model = cModel.ch(1)
        val b1Model = cModel.ch(2)
        val c1Model = cModel.ch(3)
        val gModel = cModel.ch(4)

        // both models are of the same type but represent fields in different classes
        assertNotEquals(bModel, cModel)
        assertNotEquals(a1Model, aModel)
        assertNotEquals(b1Model, cModel)
        assertNotEquals(b1Model, bModel)
        assertNotEquals(b1Model, c1Model)
        assertNotEquals(c1Model, bModel)

        // OuterClass2: class model vs field model
        assertNotEquals(model, dModel)
    }

    @Test
    fun deserialize_inner_class_2() {
        val obj = OuterClass2()
        serializer.addObject(obj)
        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val result = deserializer.readObject()!!
        val a = result.values[0] as ValueTree
        val b = result.values[1] as ValueTree
        val c = a.values[0] as ValueTree
        assertEquals(b, c)

        assertEquals(null, c.values[0])//a1
        assertEquals(null, c.values[1])//b1
        assertEquals(null, c.values[2])//c1
        assertEquals(null, c.values[3])//d
        assertEquals(true, c.values[4])//g
    }

    @Test
    fun inspect_deep_arrays() {
        val model = serializer.inspector.inspect(DeepArray::class.java)
        assertEquals(DataType.Object, model.dataType)
        assertEquals(1, model.ch().size)

        // int[][][][]
        val arrModel = model.ch(0)
        assertEquals(DataType.Array, arrModel.dataType)
        assertEquals(DataType.Array, arrModel.dataSubType)
        assertEquals(1, arrModel.ch().size)

        // int[][][]
        val arr2Model = arrModel.ch(0)
        assertEquals(DataType.Array, arr2Model.dataType)
        assertEquals(DataType.Array, arr2Model.dataSubType)
        assertEquals(1, arr2Model.ch().size)

        // int[][]
        val arr3Model = arr2Model.ch(0)
        assertEquals(DataType.Array, arr3Model.dataType)
        assertEquals(DataType.Array, arr3Model.dataSubType)
        assertEquals(1, arr3Model.ch().size)

        // int[]
        val arr4Model = arr3Model.ch(0)
        assertEquals(DataType.Array, arr4Model.dataType)
        assertEquals(DataType.Int, arr4Model.dataSubType)
        assertEquals(true, arr4Model.isSubTypePrimitive)
        assertNull(arr4Model.children)

        serializer.addDataDescriptionOrRef(model)
        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)
        val model2 = deserializer.readDataDescription()
        assert(model2.equals(model))
    }

    @Test
    fun inspect_deep_implicit_arrays() {
        val model = serializer.inspector.inspect(DeepArrayImplicit::class.java)
        assertEquals(DataType.Object, model.dataType)
        assertEquals(1, model.ch().size)

        val arrModel = model.ch(0)
        assertEquals(DataType.Object, arrModel.dataType)
        assertEquals(DataType.Undefined, arrModel.dataSubType)
        assertNotNull(arrModel.children)
        assertEquals(0, arrModel.ch().size)

        serializer.addDataDescriptionOrRef(model)
        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)
        val model2 = deserializer.readDataDescription()
        assert(model2.equals(model))
    }

    @Test
    fun deserialize_deep_arrays() {
        val obj = DeepArray()
        serializer.addObject(obj)

        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)
        val result = deserializer.readObject()!!

        validate_deep_array_values(obj.arr, result)
    }

    @Test
    fun deserialize_deep_implicit_arrays() {
        val obj = DeepArrayImplicit()
        serializer.addObject(obj)

        val serialized = serializer.endPacket()
        deserializer.setSource(serialized.buffer, 0, serialized.size)
        val result = deserializer.readObject()!!

        validate_deep_array_values(obj.arr as Array<Array<Array<IntArray>>>, result)
    }

    private fun validate_deep_array_values(origArr: Array<Array<Array<IntArray>>>, result: ValueTree) {
        assertEquals(1, result.model!!.ch().size)
        val arr = result.values[0] as ValueTree
        assertEquals(2, arr.values.size)
        val arrLeft = arr.values[0] as ValueTree
        val arrRight = arr.values[1] as ValueTree

        // left
        assertEquals(1, arrLeft.values.size)
        val arrLeft0 = arrLeft.values[0] as ValueTree
        assertEquals(1, arrLeft0.values.size)
        val arrLeft0_0 = arrLeft0.values[0] as ValueTree
        assertEquals(2, arrLeft0_0.values.size)
        assertEquals(origArr[0][0][0][0], arrLeft0_0.values[0] as Int)
        assertEquals(origArr[0][0][0][1], arrLeft0_0.values[1] as Int)

        // right
        assertEquals(2, arrRight.values.size)
        assertNull(arrRight.values[1])
        val arrRight0 = arrRight.values[0] as ValueTree
        assertEquals(1, arrRight0.values.size)
        val arrRight0_0 = arrRight0.values[0] as ValueTree
        assertEquals(1, arrRight0_0.values.size)
        assertEquals(origArr[1][0][0][0], arrRight0_0.values[0] as Int)
    }


    inner class GameState {
        var objects: Array<GameObject>? = null
        var omg: Boolean = false
    }

    inner class GameObject {
        var pos = Vector3(1f, 2f, 3f)
        var size = Vector2(10f, 5f)
    }

    inner class Vector3(var x: Float, var y: Float, var z: Float)
    inner class Vector2(var x: Float, var y: Float)


    fun ObjectModelNode.ch(): Array<ObjectModelNode> {
        return this.children!!
    }
    fun ObjectModelNode.ch(index: Int): ObjectModelNode {
        return this.children!![index]
    }
}