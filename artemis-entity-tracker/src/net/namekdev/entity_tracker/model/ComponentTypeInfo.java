package net.namekdev.entity_tracker.model;

import java.util.Vector;


public class ComponentTypeInfo {
	/** Only available on server side. */
	public Class<?> type;

	public String name;
	public int index;
	public final Vector<FieldInfo> fields = new Vector<FieldInfo>();


	public ComponentTypeInfo(String name) {
		this.name = name;
	}

	public ComponentTypeInfo(Class<?> type) {
		this.type = type;
		this.name = type.getSimpleName();
	}
}
