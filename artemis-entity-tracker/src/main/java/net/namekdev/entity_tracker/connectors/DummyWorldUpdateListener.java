package net.namekdev.entity_tracker.connectors;

import java.util.BitSet;

import net.namekdev.entity_tracker.model.ComponentTypeInfo;

public class DummyWorldUpdateListener implements WorldUpdateListener {
	@Override
	public void injectWorldController(WorldController controller) {
	}

	@Override
	public int getListeningBitset() {
		return 0;
	}

	@Override
	public void addedSystem(int index, String name, BitSet allTypes, BitSet oneTypes, BitSet notTypes) {
	}

	@Override
	public void addedManager(String name) {
	}

	@Override
	public void addedComponentType(int index, ComponentTypeInfo info) {
	}

	@Override
	public void updatedEntitySystem(int index, int entitiesCount, int maxEntitiesCount) {
	}

	@Override
	public void addedEntity(int entityId, BitSet components) {
	}

	@Override
	public void deletedEntity(int entityId) {
	}

	@Override
	public void updatedComponentState(int entityId, int componentIndex, Object[] values) {
	}
}
