package net.namekdev.entity_tracker.connectors;

public interface WorldController {
	void setSystemState(String name, boolean isOn);
	void requestComponentState(int entityId, int componentIndex);
	void setComponentValue(int entityId, int componentIndex, Object value);
}
