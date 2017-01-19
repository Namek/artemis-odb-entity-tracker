package net.namekdev.entity_tracker.utils.serialization;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;

import java.util.Vector;

import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Field;

public class ObjectTypeInspector {
	/**
	 * Returns tree description of type.
	 *
	 * @return return {@code null} to ignore type (it won't be de/serialized)
	 */
	public ObjectModelNode inspect(Class<?> type) {
		return inspectLevels(type, Integer.MAX_VALUE);
	}

	protected ObjectModelNode inspectLevels(Class<?> type, int leftLevels) {
		Field[] fields = ClassReflection.getDeclaredFields(type);

		ObjectModelNode root = new ObjectModelNode();
		root.networkType = TYPE_TREE;
		root.children = new Vector<>(fields.length);

		for (Field field : fields) {
			Class<?> fieldType = field.getType();
			ObjectModelNode child = new ObjectModelNode();

			if (fieldType.isArray()) {
				Class<?> arrayElType = fieldType.getComponentType();
				byte arrayType = determineSimpleType(arrayElType);

				if (!isSimpleType(arrayType)) {
					child = inspectLevels(arrayElType, leftLevels - 1);

					if (child.networkType == TYPE_TREE) {
						arrayType = TYPE_TREE;
					}
				}

				child.networkType = TYPE_ARRAY;
				child.isArray = true;
				child.arrayType = arrayType;
			}
			else {
				child.networkType = NetworkSerialization.determineSimpleType(fieldType);

				if (child.networkType == TYPE_UNKNOWN && leftLevels > 0) {
					child = inspectLevels(fieldType, leftLevels - 1);
					child.networkType = TYPE_TREE;
				}
			}

			child.name = field.getName();

			root.children.addElement(child);
		}

		return root;
	}
}
