package net.namekdev.entity_tracker.model;

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization;
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode;

import com.artemis.utils.reflect.Field;

public class FieldInfo {
	/** Only available on server side. */
	public Field field;

	public boolean isAccessible;
	public String fieldName;
	public String classType;
	public boolean isArray;
	public byte valueType;

	/** Available when type of field is not a simple type or array. */
	public ObjectModelNode treeDesc;


	public static FieldInfo reflectField(Field field) {
		FieldInfo info = new FieldInfo();
		Class<?> type = field.getType();

		info.field = field;
		info.isAccessible = field.isAccessible();
		info.fieldName = field.getName();
		info.classType = type.getSimpleName();
		info.isArray = type.isArray();
		info.valueType = NetworkSerialization.determineSimpleType(type);

		return info;
	}
}
