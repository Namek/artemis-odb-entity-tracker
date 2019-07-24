package net.namekdev.entity_tracker.connectors

import net.namekdev.entity_tracker.model.ComponentTypeInfo

/**
 *
 * @author Namek
 */
interface IWorldUpdateListener<BitVectorType> {
	fun injectWorldController(controller: IWorldController) { }
    fun worldDisconnected() { }

    fun addedSystem(index: Int, name: String, allTypes: BitVectorType?, oneTypes: BitVectorType?, notTypes: BitVectorType?) { }
	fun addedManager(name: String) { }
	fun addedComponentType(index: Int, info: ComponentTypeInfo) { }
	fun updatedEntitySystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int) { }
	fun addedEntity(entityId: Int, components: BitVectorType) { }
	fun deletedEntity(entityId: Int) { }
	fun addedComponentTypeToEntities(componentIndex: Int, entityIds: IntArray) { }
	fun removedComponentTypeFromEntities(componentIndex: Int, entityIds: IntArray) { }
	fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) { }
}


class WorldUpdateMultiplexer<BitVectorType>(val listeners: MutableList<IWorldUpdateListener<BitVectorType>> = mutableListOf())
	: IWorldUpdateListener<BitVectorType>
{
	override fun injectWorldController(controller: IWorldController) {
		for (l in listeners) l.injectWorldController(controller)
	}

    override fun worldDisconnected() {
        for (l in listeners) l.worldDisconnected()
    }

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

	override fun addedComponentTypeToEntities(componentIndex: Int, entityIds: IntArray) {
		for (l in listeners) l.addedComponentTypeToEntities(componentIndex, entityIds)
	}

	override fun removedComponentTypeFromEntities(componentIndex: Int, entityIds: IntArray) {
		for (l in listeners) l.removedComponentTypeFromEntities(componentIndex, entityIds)
	}
}