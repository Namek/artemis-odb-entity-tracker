package net.namekdev.entity_tracker.connectors;

import com.artemis.utils.BitVector;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;

/**
 *
 * @author Namek
 */
public interface WorldUpdateListener {
	public static final int ENTITY_ADDED = 1 << 1;
	public static final int ENTITY_DELETED = 1 << 2;
//	public static final int CHANGED = 1 << 3;
	public static final int ENTITY_SYSTEM_STATS = 1 << 4;


	void injectWorldController(WorldController controller);

	int getListeningBitset();

	void addedSystem(int index, String name, BitVector allTypes, BitVector oneTypes, BitVector notTypes);

	void addedManager(String name);

	void addedComponentType(int index, ComponentTypeInfo info);

	void updatedEntitySystem(int index, int entitiesCount, int maxEntitiesCount);

	void addedEntity(int entityId, BitVector components);

//	void changed(Entity e);

	void deletedEntity(int entityId);

	void updatedComponentState(int entityId, int componentIndex, Object[] values);


}
