package net.namekdev.entity_tracker.connectors

import net.namekdev.entity_tracker.model.ComponentTypeInfo

/**
 *
 * @author Namek
 */
interface WorldUpdateListener<BitVectorType> {
	fun injectWorldController(controller: WorldController)
	val listeningBitset: Int
	fun addedSystem(index: Int, name: String, allTypes: BitVectorType?, oneTypes: BitVectorType?, notTypes: BitVectorType?)
	fun addedManager(name: String)
	fun addedComponentType(index: Int, info: ComponentTypeInfo)
	fun updatedEntitySystem(systemIndex: Int, entitiesCount: Int, maxEntitiesCount: Int)
	fun addedEntity(entityId: Int, components: BitVectorType)
	//	void changed(Entity e);
	fun deletedEntity(entityId: Int)

	fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any)

	companion object {
		const val ENTITY_ADDED = 1 shl 1
		const val ENTITY_DELETED = 1 shl 2
		//	const val CHANGED = 1 shl 3;
		const val ENTITY_SYSTEM_STATS = 1 shl 4
	}
}