package net.namekdev.entity_tracker.model;

import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode;


public class ComponentTypeInfo {
	/** Only available on server side. */
	public Class<?> type;

	public String name;
	public int index;
	public ObjectModelNode model;


	public ComponentTypeInfo(String name) {
		this.name = name;
	}

	public ComponentTypeInfo(Class<?> type) {
		this.type = type;
		this.name = type.getSimpleName();
	}
}
