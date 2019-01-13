package net.namekdev.entity_tracker.connectors

import net.namekdev.entity_tracker.connectors.IWorldUpdateListener.Companion.LISTEN_TO_EVERYTHING
import net.namekdev.entity_tracker.model.ComponentTypeInfo

/**
 *
 * @author Namek
 */
interface IWorldUpdateListener<BitVectorType> {
	fun injectWorldController(controller: IWorldController)
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

		const val LISTEN_TO_EVERYTHING = ENTITY_ADDED or ENTITY_DELETED or ENTITY_SYSTEM_STATS
	}
}

class WorldUpdateListenerImpl<BitVectorType> : IWorldUpdateListener<BitVectorType> {
	override fun injectWorldController(controller: IWorldController) {}

	override val listeningBitset: Int
		get() = LISTEN_TO_EVERYTHING

	override fun addedSystem(index: Int, name: String, allTypes: BitVectorType?, oneTypes: BitVectorType?, notTypes: BitVectorType?) {}

	override fun addedManager(name: String) {}

	override fun addedComponentType(index: Int, info: ComponentTypeInfo) {}

	override fun updatedEntitySystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int) {}

	override fun addedEntity(entityId: Int, components: BitVectorType) {}

	override fun deletedEntity(entityId: Int) {}

	override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {}
}


class WorldUpdateMultiplexer<BitVectorType>(val listeners: List<IWorldUpdateListener<BitVectorType>> = listOf())
	: IWorldUpdateListener<BitVectorType>
{
	override fun injectWorldController(controller: IWorldController) {
		for (l in listeners) l.injectWorldController(controller)
	}

	// TODO this does not fit to IWorldUpdateListener, probably move it to worldController?
	override val listeningBitset: Int
		get() = LISTEN_TO_EVERYTHING

	override fun addedSystem(
		index: Int,
		name: String,
		allTypes: BitVectorType?,
		oneTypes: BitVectorType?,
		notTypes: BitVectorType?
	) {
		for (l in listeners) l.addedSystem(index, name, allTypes, oneTypes, notTypes)
	}

	override fun addedManager(name: String) {
		for (l in listeners) l.addedManager(name)
	}

	override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
		for (l in listeners) l.addedComponentType(index, info)
	}

	override fun updatedEntitySystem(systemIndex: Int, entitiesCount: Int, maxEntitiesCount: Int) {
		for (l in listeners) l.updatedEntitySystem(systemIndex, entitiesCount, maxEntitiesCount)
	}

	override fun addedEntity(entityId: Int, components: BitVectorType) {
		for (l in listeners) l.addedEntity(entityId, components)
	}

	override fun deletedEntity(entityId: Int) {
		for (l in listeners) l.deletedEntity(entityId)
	}

	override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
		for (l in listeners) l.updatedComponentState(entityId, componentIndex, valueTree)
	}

}