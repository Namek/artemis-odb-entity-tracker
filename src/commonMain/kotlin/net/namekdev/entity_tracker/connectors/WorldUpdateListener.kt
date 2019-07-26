package net.namekdev.entity_tracker.connectors

import net.namekdev.entity_tracker.model.ComponentTypeInfo

/**
 *
 * @author Namek
 */
interface IWorldUpdateListener<BitVectorType> {
	fun injectWorldController(controller: IWorldController) { }
    fun worldDisconnected() { }

    fun addedSystem(
		index: Int,
		name: String,
		allTypes: BitVectorType?,
		oneTypes: BitVectorType?,
		notTypes: BitVectorType?,
		isEnabled: Boolean
	) { }
	fun addedComponentType(index: Int, info: ComponentTypeInfo) { }
	fun updatedSystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int, isEnabled: Boolean) { }
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
		notTypes: BitVectorType?,
		isEnabled: Boolean
	) {
		for (l in listeners) l.addedSystem(index, name, allTypes, oneTypes, notTypes, isEnabled)
	}

	override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
		for (l in listeners) l.addedComponentType(index, info)
	}

	override fun updatedSystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int, isEnabled: Boolean) {
		for (l in listeners) l.updatedSystem(index, entitiesCount, maxEntitiesCount, isEnabled)
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