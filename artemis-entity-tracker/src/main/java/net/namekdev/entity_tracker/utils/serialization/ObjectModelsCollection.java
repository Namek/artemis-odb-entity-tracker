package net.namekdev.entity_tracker.utils.serialization;

public interface ObjectModelsCollection {
	void add(ObjectModelNode model);
	ObjectModelNode get(Class<?> type);
	ObjectModelNode get(int index);
	int size();
}
