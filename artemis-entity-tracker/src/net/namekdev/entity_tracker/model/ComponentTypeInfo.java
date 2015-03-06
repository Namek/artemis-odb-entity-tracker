package net.namekdev.entity_tracker.model;

import java.util.Vector;


public class ComponentTypeInfo {
	public String name;
	public final Vector<FieldInfo> fields = new Vector<FieldInfo>();


	public ComponentTypeInfo() { }

	public ComponentTypeInfo(String name) {
		this.name = name;
	}

}
