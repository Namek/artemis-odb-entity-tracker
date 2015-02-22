package net.namekdev.entity_tracker.connectors;

import java.util.BitSet;

public interface UpdateListener {
	public static final int ADDED = 1 << 1;
	public static final int DELETED = 1 << 2;
//	public static final int CHANGED = 1 << 3;


	int getListeningBitset();

	void addedComponentType(String name);

	void added(int entityId, BitSet components);

//	void changed(Entity e);

	void deleted(int entityId);
}
