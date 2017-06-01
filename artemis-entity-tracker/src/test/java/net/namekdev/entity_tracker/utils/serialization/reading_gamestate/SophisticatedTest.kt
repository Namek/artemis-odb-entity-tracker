package net.namekdev.entity_tracker.utils.serialization.reading_gamestate

import net.namekdev.entity_tracker.utils.sample.CyclicReferencesHidden
import net.namekdev.entity_tracker.utils.sample.DeepArray
import net.namekdev.entity_tracker.utils.sample.DeepArrayImplicit
import net.namekdev.entity_tracker.utils.sample.EnumFullTestClass
import net.namekdev.entity_tracker.utils.sample.OuterClass
import net.namekdev.entity_tracker.utils.sample.OuterClass2
import net.namekdev.entity_tracker.utils.serialization.NetworkDeserializer
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.DataType
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector
import net.namekdev.entity_tracker.utils.serialization.ValueTree
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import java.util.*

/**
 *
 */
class SophisticatedTest {
    lateinit var serializer: NetworkSerializer
    lateinit var deserializer: NetworkDeserializer
    lateinit var inspector: ObjectTypeInspector


    @Before
    fun setup() {
        serializer = NetworkSerialization.createSerializer()
        deserializer = NetworkDeserializer()
        inspector = serializer.inspector
    }

    @Test
    fun compare_two_enum_class_models() {
        // create first model, serialize it and retrieve a potential copy of it
        val model = inspector.inspect(EnumFullTestClass::class.java)
        serializer.addDataDescriptionOrRef(model)
        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)
        val model2 = deserializer.readDataDescription()

        // that should be equal
        assertTrue(model2.equals(model))

        // now, let's modify one of them and compare again, should be different
        val enumDescrModel = model2.children!![2].children!![0].children!![0]
        val thirdEnumValue = enumDescrModel.children!![2]
        val newThirdEnumValue = ObjectModelNode(null, 0, null).copyFrom(thirdEnumValue)
        newThirdEnumValue.enumValue = 999
        enumDescrModel.children!!.set(2, newThirdEnumValue)

        assertFalse(model2.equals(model))
    }

    @Test
    fun deserialize_inner_game_state() {
        val gameState = GameState()
        gameState.objects = arrayOf(GameObject(), GameObject(), GameObject(), GameObject())

        val serializer = NetworkSerializer().reset()
        val inspector = serializer.inspector
        val model = inspector.inspect(GameState::class.java)


        serializer.addDataDescriptionOrRef(model)
        serializer.addObject(model, gameState)

        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val model2 = deserializer.readDataDescription()
        assertTrue(model2.equals(model))

        val result = deserializer.readObject(model2, true)
        assertTrue(result.model!!.equals(model))
        assertTrue(result.model!!.equals(model2))
    }

    @Test
    fun deserializeInnerGameState_auto() {
        val gameState = GameState()
        gameState.objects = arrayOf(GameObject(), GameObject(), GameObject(), GameObject())

        val serializer = NetworkSerializer().reset()
        val model = inspector.inspect(GameState::class.java)


        serializer.addObject(gameState)

        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val result = deserializer.readObject(true)
        assertTrue(result.model!!.equals(model))

//        val reading = deserializer.startReadingData(model2)
    }

    @Test
    fun serialize_cyclic_reference() {
        val node = CyclicReferencesHidden()
        node.children = arrayOf(node)

        serializer.addObject(node)

        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val result = deserializer.readObject(true)
    }

    @Test
    fun inspect_inner_class() {
        val outerClassModel = serializer.inspector.inspect(OuterClass::class.java)
        assertEquals(1, outerClassModel.children!!.size)
        assertNull(outerClassModel.parent)

        val aModel = outerClassModel.children!![0]
        assertEquals(1, aModel.children!!.size)
//        assertEquals(aModel)

        val bModel = aModel.children!![0]
        assertEquals(1, bModel.children!!.size)

        val cModel = bModel.children!![0]
        assertEquals(2, cModel.children!!.size)

        val _aModel = cModel.children!![0]

        // TODO what to assert here?

        val dModel = cModel.children!![1]
        assertEquals(DataType.Boolean, dModel.dataType)


//        assertEquals(DataType.DescriptionRef, _aModel.dataType)

        // it should be a reference to same model but it's different
        assertNotEquals(outerClassModel.id, _aModel.id)
        assertNotNull(_aModel.parent)
        assertEquals(cModel.id, _aModel.parent!!.id)
    }

    @Test
    fun deserialize_inner_class() {
        val obj = OuterClass()
        serializer.addObject(obj)
        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val result = deserializer.readObject(true)
        val a = (result.values[0] as ValueTree)
        val b = (a.values[0] as ValueTree)
        val c = (b.values[0] as ValueTree)
        val d = c.values[0] as Boolean
        assertEquals(true, d)
    }

    @Test
    fun deserialize_inner_class_2() {
        val obj = OuterClass2()
        serializer.addObject(obj)
        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)

        val result = deserializer.readObject(true)
        // TODO
        assert(false)
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
    }

    @Test
    fun deserialize_deep_arrays() {
        val obj = DeepArray()
        serializer.addObject(obj)
        // TODO
        assert(false)
    }

    @Test
    fun inspect_deep_implicit_arrays() {
        val model = serializer.inspector.inspect(DeepArrayImplicit::class.java)
        assertEquals(DataType.Object, model.dataType)
        assertEquals(1, model.children!!.size)

        val arrModel = model.children!![0]
        assertEquals(DataType.Array, arrModel.dataType)
        assertEquals(DataType.Int, arrModel.dataSubType)
        assertEquals(true, arrModel.isSubTypePrimitive)
        assertNull(arrModel.children)
    }

    @Test
    fun deserialize_deep_implicit_arrays() {
        serializer.addObject(DeepArrayImplicit())

        val serialized = serializer.result
        deserializer.setSource(serialized.buffer, 0, serialized.size)
        val result = deserializer.readObject(true)

        assert(false)
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


    fun ObjectModelNode.ch(): Vector<ObjectModelNode> {
        return this.children!!
    }
    fun ObjectModelNode.ch(index: Int): ObjectModelNode {
        return this.children!![index]
    }
}