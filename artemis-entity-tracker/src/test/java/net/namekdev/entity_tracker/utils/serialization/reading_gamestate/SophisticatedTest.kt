package net.namekdev.entity_tracker.utils.serialization.reading_gamestate

import net.namekdev.entity_tracker.utils.sample.CyclicReferencesHidden
import net.namekdev.entity_tracker.utils.sample.EnumFullTestClass
import net.namekdev.entity_tracker.utils.serialization.NetworkDeserializer
import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector
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

}