package net.namekdev.entity_tracker.connectors;

public interface WorldController {
	void setSystemState(String name, boolean isOn);
	void setManagerState(String name, boolean isOn);
	void requestComponentState(int entityId, int componentIndex);
	void setComponentFieldValue(int entityId, int componentIndex, int fieldIndex, Object value);
}
