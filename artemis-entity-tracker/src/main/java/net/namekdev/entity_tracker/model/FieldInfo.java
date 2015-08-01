package net.namekdev.entity_tracker.model;

import net.namekdev.entity_tracker.utils.serialization.NetworkSerialization;

import com.artemis.utils.reflect.Field;

public class FieldInfo {
	/** Only available on server side. */
	public Field field;

	public boolean isAccessible;
	public String fieldName;
	public String classType;
	public boolean isArray;
	public int valueType;


	public static FieldInfo reflectField(Field field) {
		FieldInfo info = new FieldInfo();
		Class<?> type = field.getType();

		info.field = field;
		info.isAccessible = field.isAccessible();
		info.fieldName = field.getName();
		info.classType = type.getSimpleName();
		info.isArray = type.isArray();
		info.valueType = NetworkSerialization.determineNetworkType(type);

		return info;
	}
}
