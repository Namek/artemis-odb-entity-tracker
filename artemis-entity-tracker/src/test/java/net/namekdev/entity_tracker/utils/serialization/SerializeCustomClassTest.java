package net.namekdev.entity_tracker.utils.serialization;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.TYPE_ARRAY;
import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.TYPE_ENUM;
import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.TYPE_FLOAT;
import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.TYPE_OBJECT;
import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.TYPE_UNKNOWN;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.namekdev.entity_tracker.utils.sample.ArrayTestClass;
import net.namekdev.entity_tracker.utils.sample.CyclicClass;
import net.namekdev.entity_tracker.utils.sample.CyclicClassIndirectly;
import net.namekdev.entity_tracker.utils.sample.EnumTestClass;
import net.namekdev.entity_tracker.utils.sample.EnumTestClass.TestEnum;
import net.namekdev.entity_tracker.utils.sample.GameObject;
import net.namekdev.entity_tracker.utils.sample.GameState;
import net.namekdev.entity_tracker.utils.sample.Vector2;
import net.namekdev.entity_tracker.utils.sample.Vector3;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer.SerializationResult;

public class SerializeCustomClassTest {
	NetworkSerializer serializer;
	NetworkDeserializer deserializer;
	ObjectTypeInspector inspector;


	@Before
	public void setup() {
		serializer = NetworkSerialization.createSerializer(); 
		deserializer = new NetworkDeserializer();
		inspector = new ObjectTypeInspector();
	}

	@Test
	public void inspect_vectors() {
		ObjectModelNode model = inspector.inspect(Vector3.class);
		assertTrue(model.children != null && model.children.size() == 3);
		assertEquals("x", model.children.get(0).name);
		assertEquals("y", model.children.get(1).name);
		assertEquals("z", model.children.get(2).name);

		model = inspector.inspect(Vector2.class);
		assertTrue(model.children != null && model.children.size() == 2);
		assertEquals("x", model.children.get(0).name);
		assertEquals("y", model.children.get(1).name);
	}

	// TODO rename to serialize_gamestate() and create separate inspect_gamestate()
	// that will serialize only info about basic structure
	/*@Test
	public void inspect_gamestate() {
		GameState gameState = new GameState();
		gameState.objects = new GameObject[] {
			new GameObject(), new GameObject()
		};

		ObjectModelNode model = inspector.inspect(GameState.class);

		// GameState
		assertEquals(TYPE_TREE, model.networkType);
		assertNotNull(model.children);
		assertFalse(model.isArray());
		assertEquals(1, model.children.size());

		// GameState.objects (GameObject[])
		ObjectModelNode objects = model.children.elementAt(0);
		assertEquals("objects", objects.name);
		assertEquals(TYPE_ARRAY, objects.networkType);
		assertTrue(objects.isArray());
		assertNotNull(objects.children);
		assertEquals(2, objects.children.size());
		assertEquals(TYPE_TREE, objects.arrayType);

		// GameState.objects[i].pos (Vector3)
		ObjectModelNode pos1 = objects.children.elementAt(0);
		assertVector3(pos1, "pos");

		// GameState.objects[i].size (Vector2)
		ObjectModelNode size1 = objects.children.elementAt(1);
		assertVector2(size1, "size");
	}*/
	
	private void checkGameStateInspection(ObjectTypeInspector inspector) {
		int a = 5;
//		assertEquals(TYPE_OBJECT, model.networkType);
		// TODO!
	}
	
	@Test
	public void inspect_gamestate() {
		GameState gameState = new GameState();
		gameState.objects = new GameObject[] {
			new GameObject(), new GameObject()
		};
		ObjectModelNode model = inspector.inspect(gameState.getClass());
		checkGameStateInspection(inspector);
	}
	
	@Test
	public void serialize_gamestate() {
		GameState gameState = new GameState();
		gameState.objects = new GameObject[] {
			new GameObject(), new GameObject(), new GameObject()
		};
		
		serializer.addObject(gameState);
		checkGameStateInspection(serializer.inspector);

		SerializationResult res = serializer.getResult();
		deserializer.setSource(res.buffer, 0, res.size);
		ValueTree deserializedGameState = deserializer.readObject();
		
		assertNull(deserializedGameState.parent);
		
		// there is only one field - "objects"
		assertEquals(1, deserializedGameState.values.length);
		assert(deserializedGameState.values[0] instanceof ValueTree);
		
		ValueTree objects = (ValueTree) deserializedGameState.values[0];
		assertEquals(gameState.objects.length, objects.values.length);
		assertEquals(deserializedGameState, objects.parent);

		for (int i = 0; i < objects.values.length; ++i) {
			GameObject originalGameObject = gameState.objects[i];
			ValueTree gameObject = (ValueTree) objects.values[i];
			
			assertEquals(objects, gameObject.parent);
			
			// two fields: pos, size
			assertEquals(2, gameObject.values.length);
			ValueTree posField = (ValueTree) gameObject.values[0];
			ValueTree sizeField = (ValueTree) gameObject.values[1];
			
			assertEquals(gameObject, posField.parent);
			assertEquals(gameObject, sizeField.parent);
			
			assertEquals(3, posField.values.length);
			assertEquals(2, sizeField.values.length);
			
			assertEquals(originalGameObject.pos.x, posField.values[0]);
			assertEquals(originalGameObject.pos.y, posField.values[1]);
			assertEquals(originalGameObject.pos.z, posField.values[2]);
			
			assertEquals(originalGameObject.size.x, sizeField.values[0]);
			assertEquals(originalGameObject.size.y, sizeField.values[1]);
		}
	}

	private void assertVector3(ObjectModelNode node, String name) {
		assertEquals(name, node.name);
		assertEquals(TYPE_OBJECT, node.networkType);
		assertFalse(node.isArray());
		assertNotNull(node.children);

		// GameState.objects[0].pos -> x, y, z (floats)
		assertFloat(node.children.elementAt(0));
		assertFloat(node.children.elementAt(1));
		assertFloat(node.children.elementAt(2));
	}

	private void assertVector2(ObjectModelNode node, String name) {
		assertEquals(name, node.name);
		assertEquals(TYPE_OBJECT, node.networkType);
		assertFalse(node.isArray());
		assertNotNull(node.children);

		// Vector2 -> x, y (floats)
		assertFloat(node.children.elementAt(0));
		assertFloat(node.children.elementAt(1));
	}

	private void assertFloat(ObjectModelNode node) {
		assertEquals(TYPE_FLOAT, node.networkType);
		assertFalse(node.isArray());
		assertNull(node.children);
	}

	@Test
	public void deserialize_vector3() {
		testVector3(inspector);
	}

	private void testVector3(ObjectTypeInspector inspector) {
		NetworkSerializer serializer = new NetworkSerializer().reset();

		Vector3 vector = new Vector3(4, 5, 6);
		ObjectModelNode model = inspector.inspect(vector.getClass());

		serializer.addObjectDescription(model);
		serializer.addObject(model, vector);

		byte[] buffer = serializer.getResult().buffer;
		deserializer.setSource(buffer, 0, serializer.getResult().size);

		ObjectModelNode model2 = deserializer.readObjectDescription();
		assertEquals(model, model2);

		ValueTree result = deserializer.readObject(model2);

		assertEquals(3, result.values.length);
		assertEquals(vector.x, result.values[0]);
		assertEquals(vector.y, result.values[1]);
		assertEquals(vector.z, result.values[2]);
	}

	@Test
	public void deserialize_simple_arrays() {
		Float[] floats = new Float[] { 0f, 1f, 2f };
		String[] strings = new String[] { "asd", "omg", "this is a test?" };

		testArray(floats, inspector);
		testArray(strings, inspector);
	}

	private void testArray(Object[] arr, ObjectTypeInspector inspector) {
		NetworkSerializer serializer = new NetworkSerializer().reset();
		ObjectModelNode model = inspector.inspect(arr.getClass());
		serializer.addObjectDescription(model);
		SerializationResult serialized = serializer.getResult();
		deserializer.setSource(serialized.buffer, 0, serialized.size);

		ObjectModelNode model2 = deserializer.readObjectDescription();
		assertEquals(model, model2);
	}

	@Test
	public void deserialize_gamestate() {
		GameState gameState = new GameState();
		gameState.objects = new GameObject[] {
			new GameObject(), new GameObject(),
			new GameObject(), new GameObject()
		};

		NetworkSerializer serializer = new NetworkSerializer().reset();
		ObjectModelNode model = inspector.inspect(GameState.class);


		serializer.addObjectDescription(model);
		serializer.addObject(model, gameState);

		SerializationResult serialized = serializer.getResult();
		deserializer.setSource(serialized.buffer, 0, serialized.size);

		ObjectModelNode model2 = deserializer.readObjectDescription();
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
		model = inspector.inspect(vect.getClass());
		model.setValue(vect, new int[] { 1 }/*vert.y*/, y);
		assertEquals(y, vect.y, 0.01f);

		GameObject obj = new GameObject();
		model = inspector.inspect(obj.getClass());
		model.setValue(obj, new int[] { 1, 1 }/*obj.size.y*/, y);
		assertEquals(y, obj.size.y, 0.01f);

		GameState gs = new GameState();
		gs.objects = new GameObject[] { new GameObject(), new GameObject(), new GameObject() };
		model = inspector.inspect(gs.getClass());
		model.setValue(gs, new int[] { 0, 2, 0, 0, 1 }/*gs.objects[2].pos.y*/, y);
		assertEquals(y, gs.objects[2].pos.y, 0.000001f);
	}
	
	@Test
	public void inspect_and_update_enum_fields() {
		EnumTestClass obj = new EnumTestClass();
		ObjectModelNode model = inspector.inspect(obj.getClass());
		
		TestEnum newVal = TestEnum.Third;
		model.setValue(obj, new int[] { 0 }, newVal);
		assertEquals(obj.getEnumUndefined(), newVal);
		
		assertEquals(obj.getEnumValued(), TestEnum.First);
		model.setValue(obj, new int[] { 1 }, newVal);
		assertEquals(obj.getEnumValued(), newVal);
	}
	
	@Test
	public void inspect_enum_names() {
		EnumTestClass obj = new EnumTestClass();
		TestEnum possibleValues[] = TestEnum.class.getEnumConstants();
		ObjectModelNode model = inspector.inspect(obj.getClass());
		ObjectModelNode enumModel = model.children.elementAt(0);

		assertEquals(TYPE_ENUM, enumModel.networkType);

		for (int i = 0; i < possibleValues.length; ++i) {
			// TODO this is a special case for enums, normally we don't do this.
			// Is this gonna last or not?
			// I think not. Probably, we'll need a global register for enums and simply reference it.
			// TYPE_ENUM_DESCRIPTION will be needed then.
//			ObjectModelNode val = enumModel.children.elementAt(i);
//			assertEquals(possibleValues[i].name(), val.name);
			
			assert(false);
		}
	}
	
	@Test
	public void serialize_enums() {
		Object obj = new EnumTestClass();
		
		// TODO!!
		assertTrue(false);

		// now put it through serializer
		NetworkSerializer serializer = new NetworkSerializer().reset();
		// serializer.defineEnums(/*strings and respective values here*/)
//		serializer.addEnum(/*just the integer?*/)
	}
	
	@Test
	public void inspect_cyclic_reference() {
		CyclicClass obj = new CyclicClass();
		obj.other = new CyclicClass();
		obj.other.other = obj;
		
		ObjectModelNode model = inspector.inspect(obj.getClass());
		assertEquals(2, model.children.size());
		assertEquals(model.id, model.children.elementAt(0).id);
		assert(model != model.children.elementAt(0));
	}
	
	@Test
	public void inspect_indirectly_cyclic_class() {
		CyclicClassIndirectly obj = new CyclicClassIndirectly();
		obj.obj = new CyclicClassIndirectly.OtherClass();
		obj.obj.obj = obj;
		obj.obj.obj.obj = new CyclicClassIndirectly.OtherClass();
		
		ObjectModelNode model = inspector.inspect(obj.getClass());
		assertEquals(2, model.children.size());
		ObjectModelNode fieldModel = model.children.elementAt(0);
		assertEquals(model.id, fieldModel.children.elementAt(0).id);
	}
	
	@Test
	public void inspect_indirectly_cyclic_class_in_array() {
		CyclicClassIndirectly obj = new CyclicClassIndirectly();
		obj.arr = new CyclicClassIndirectly.ArrayClass();
		obj.arr.objs = new CyclicClassIndirectly[] {
			obj
		};
		
		ObjectModelNode model = inspector.inspect(obj.getClass());
		assertEquals(2, model.children.size());
		ObjectModelNode arrFieldModel = model.children.elementAt(1);
		ObjectModelNode objsFieldModel = arrFieldModel.children.elementAt(0);
		assert(objsFieldModel.isArray());
		assertNotEquals(model.id, objsFieldModel.id);
		assertEquals(model.id, objsFieldModel.arrayType);
	}
	
	@Test
	public void fail_to_inspect_simple_types() {
		Object[] testSubjects = new Object[] {
			"asd",		// String
			5,			// Integer
		};
		
		for (Object testSubject : testSubjects) {
			ObjectModelNode model = null;
			try {
				model = inspector.inspect(testSubject.getClass());
			}
			catch (Error exc) { }
			finally {
				assertEquals(null, model);
			}
		}
	}
	
	@Test
	public void inspect_array_field_containing_various_objects() {
		ArrayTestClass obj = new ArrayTestClass();
		obj.array = new Object[] {
			new Vector2(5, 6),
			new Vector3(7, 8, 9)
		};
		ObjectModelNode model = inspector.inspect(obj.getClass());
		assert(!model.isArray());
		assertEquals(1, model.children.size());
		
		ObjectModelNode arrayModel = model.children.elementAt(0);
		assert(arrayModel.isArray());
		assert(arrayModel.children == null);
		assertEquals("array", arrayModel.name);
	}
	
	@Test
	public void deserialize_array_of_various_objects() {
		Object[] array = new Object[] {
			new Vector2(5, 6),
			new Vector3(7, 8, 9)
		};

		ObjectModelNode model = inspector.inspect(array.getClass());
		assert(model.isArray());
		assertEquals(null, model.children);
		assertEquals(TYPE_UNKNOWN, model.arrayType);
		
		
		// Vector2
//		ObjectModelNode v2Model = fieldModel.children.elementAt(0);
//		assertEquals(2, v2Model.children.size());
//		assertEquals("x", v2Model.children.elementAt(0).name);
//		assertEquals("y", v2Model.children.elementAt(1).name);
//		
//		// Vector3
//		ObjectModelNode v3Model = fieldModel.children.elementAt(1);
//		assertEquals(3, v3Model.children.size());
//		assertEquals("x", v3Model.children.elementAt(0).name);
//		assertEquals("y", v3Model.children.elementAt(1).name);
//		assertEquals("z", v3Model.children.elementAt(1).name);
	}
}
