package net.namekdev.entity_tracker.utils.serialization;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;
import static org.junit.Assert.*;
import net.namekdev.entity_tracker.utils.sample.GameObject;
import net.namekdev.entity_tracker.utils.sample.GameState;
import net.namekdev.entity_tracker.utils.sample.Vector2;
import net.namekdev.entity_tracker.utils.sample.Vector3;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer.SerializeResult;

import org.junit.Before;
import org.junit.Test;

public class SerializeCustomClassTest {
	NetworkDeserializer deserializer;
	ObjectTypeInspector inspector1;
	ObjectTypeInspector inspectorMulti;


	@Before
	public void setup() {
		deserializer = new NetworkDeserializer();
		inspector1 = new ObjectTypeInspector.OneLevel();
		inspectorMulti = new ObjectTypeInspector.MultiLevel();
	}

	@Test
	public void inspect_vectors() {
		ObjectModelNode model = inspector1.inspect(Vector3.class);
		assertTrue(model.children != null && model.children.size() == 3);
		assertEquals("x", model.children.get(0).name);
		assertEquals("y", model.children.get(1).name);
		assertEquals("z", model.children.get(2).name);

		model = inspector1.inspect(Vector2.class);
		assertTrue(model.children != null && model.children.size() == 2);
		assertEquals("x", model.children.get(0).name);
		assertEquals("y", model.children.get(1).name);
	}

	@Test
	public void inspect_gamestate_multi_level() {
		GameState gameState = new GameState();
		gameState.objects = new GameObject[] {
			new GameObject(), new GameObject()
		};

		ObjectModelNode model = inspectorMulti.inspect(GameState.class);

		// GameState
		assertEquals(TYPE_TREE, model.networkType);
		assertNotNull(model.children);
		assertFalse(model.isArray);
		assertEquals(1, model.children.size());

		// GameState.objects (GameObject[])
		ObjectModelNode objects = model.children.elementAt(0);
		assertEquals("objects", objects.name);
		assertEquals(TYPE_ARRAY, objects.networkType);
		assertTrue(objects.isArray);
		assertNotNull(objects.children);
		assertEquals(2, objects.children.size());
		assertEquals(TYPE_TREE, objects.arrayType);

		// GameState.objects[i].pos (Vector3)
		ObjectModelNode pos1 = objects.children.elementAt(0);
		assertVector3(pos1, "pos");

		// GameState.objects[i].size (Vector2)
		ObjectModelNode size1 = objects.children.elementAt(1);
		assertVector2(size1, "size");
	}

	private void assertVector3(ObjectModelNode node, String name) {
		assertEquals(name, node.name);
		assertEquals(TYPE_TREE, node.networkType);
		assertFalse(node.isArray);
		assertNotNull(node.children);

		// GameState.objects[0].pos -> x, y, z (floats)
		assertFloat(node.children.elementAt(0));
		assertFloat(node.children.elementAt(1));
		assertFloat(node.children.elementAt(2));
	}

	private void assertVector2(ObjectModelNode node, String name) {
		assertEquals(name, node.name);
		assertEquals(TYPE_TREE, node.networkType);
		assertFalse(node.isArray);
		assertNotNull(node.children);

		// Vector2 -> x, y (floats)
		assertFloat(node.children.elementAt(0));
		assertFloat(node.children.elementAt(1));
	}

	private void assertFloat(ObjectModelNode node) {
		assertEquals(TYPE_FLOAT, node.networkType);
		assertFalse(node.isArray);
		assertNull(node.children);
	}

	@Test
	public void deserialize_vector3_one_level() {
		testVector3(inspector1);
	}

	@Test
	public void deserialize_vector3_multi_level() {
		testVector3(inspectorMulti);
	}

	private void testVector3(ObjectTypeInspector inspector) {
		NetworkSerializer serializer = new NetworkSerializer().reset();

		Vector3 vector = new Vector3(4, 5, 6);
		ObjectModelNode model = inspector.inspect(vector.getClass());
		int id = 198;

		serializer.addObjectDescription(model, id);
		serializer.addObject(model, vector);

		byte[] buffer = serializer.getResult().buffer;
		deserializer.setSource(buffer, 0, serializer.getResult().size);

		ObjectModelNode model2 = deserializer.readObjectDescription();
		model.rootId = id;
		assertEquals(model, model2);

		ValueTree result = deserializer.readObject(model2);

		assertEquals(3, result.values.length);
		assertEquals(vector.x, result.values[0]);
		assertEquals(vector.y, result.values[1]);
		assertEquals(vector.z, result.values[2]);
	}

	@Test
	public void deserialize_arrays_one_level() {
		Float[] floats = new Float[] { 0f, 1f, 2f };
		String[] strings = new String[] { "asd", "omg", "this is a test?" };

		testArray(floats, inspector1);
		testArray(strings, inspector1);
	}

	@Test
	public void deserialize_arrays_multi_level() {
		Float[] floats = new Float[] { 0f, 1f, 2f };
		String[] strings = new String[] { "asd", "omg", "this is a test?" };

		testArray(floats, inspectorMulti);
		testArray(strings, inspectorMulti);
	}

	private void testArray(Object[] arr, ObjectTypeInspector inspector) {
		NetworkSerializer serializer = new NetworkSerializer().reset();
		ObjectModelNode model = inspector.inspect(arr.getClass());
		int modelId = 123;
		serializer.addObjectDescription(model, modelId);
		SerializeResult serialized = serializer.getResult();
		deserializer.setSource(serialized.buffer, 0, serialized.size);

		ObjectModelNode model2 = deserializer.readObjectDescription();
		model.rootId = modelId;
		assertEquals(model, model2);
	}

	@Test
	public void deserialize_gamestate_multi_level() {
		GameState gameState = new GameState();
		gameState.objects = new GameObject[] {
			new GameObject(), new GameObject(),
			new GameObject(), new GameObject()
		};

		NetworkSerializer serializer = new NetworkSerializer().reset();
		ObjectModelNode model = inspectorMulti.inspect(GameState.class);
		int id = 1734552;


		serializer.addObjectDescription(model, id);
		serializer.addObject(model, gameState);

		SerializeResult serialized = serializer.getResult();
		deserializer.setSource(serialized.buffer, 0, serialized.size);

		ObjectModelNode model2 = deserializer.readObjectDescription();
		model.rootId = id;
		assertEquals(model, model2);

		ValueTree result = deserializer.readObject(model2, true);

		// Test deserialized result
		assertNull(result.parent);
		Object[] gameStateFields = result.values;
		assertEquals(1, gameStateFields.length);

		ValueTree objectsField = (ValueTree) gameStateFields[0];
		assertNotNull(objectsField.parent);
		assertEquals(result, objectsField.parent);

		Object[] objects = (objectsField).values;
		assertEquals(gameState.objects.length, objects.length);

		int n = objects.length;
		for (int i = 0; i < n; ++i) {
			ValueTree gameObj = (ValueTree) objects[i];
			assertNotNull(gameObj.parent);
			assertEquals(objectsField, gameObj.parent);
			assertEquals(2, gameObj.values.length);

			ValueTree posField = (ValueTree) gameObj.values[0];
			assertNotNull(posField.parent);
			assertEquals(gameObj, posField.parent);
			Object[] pos = posField.values;

			ValueTree sizeField = (ValueTree) gameObj.values[1];
			assertNotNull(sizeField.parent);
			assertEquals(gameObj, sizeField.parent);
			Object[] size = sizeField.values;

			Vector3 pos0 = gameState.objects[i].pos;
			Vector2 size0 = gameState.objects[i].size;

			assertEquals(pos0.x, pos[0]);
			assertEquals(pos0.y, pos[1]);
			assertEquals(pos0.z, pos[2]);

			assertEquals(size0.x, size[0]);
			assertEquals(size0.y, size[1]);
		}
	}

	@Test
	public void set_values_for_object_model() {
		ObjectModelNode model;
		float y = 55f;

		Vector3 vect = new Vector3(6, 5, 4);
		model = inspectorMulti.inspect(vect.getClass());
		model.setValue(vect, new int[] { 1 }/*vert.y*/, y);
		assertEquals(y, vect.y, 0.01f);

		GameObject obj = new GameObject();
		model = inspectorMulti.inspect(obj.getClass());
		model.setValue(obj, new int[] { 1, 1 }/*obj.size.y*/, y);
		assertEquals(y, obj.size.y, 0.01f);

		GameState gs = new GameState();
		gs.objects = new GameObject[] { new GameObject(), new GameObject(), new GameObject() };
		model = inspectorMulti.inspect(gs.getClass());
		model.setValue(gs, new int[] { 0, 2, 0, 1 }/*gs.objects[2].pos.y*/, y);
		assertEquals(y, gs.objects[2].pos.y, 0.01f);
	}
}
