package net.namekdev.entity_tracker.connectors

import net.namekdev.entity_tracker.model.ComponentTypeInfo

abstract class DummyWorldUpdateListener<BitVectorType> : WorldUpdateListener<BitVectorType> {
    override fun injectWorldController(controller: WorldController) {}

    override val listeningBitset: Int
        get() = 0

    override fun addedSystem(index: Int, name: String, allTypes: BitVectorType?, oneTypes: BitVectorType?, notTypes: BitVectorType?) {}

    override fun addedManager(name: String) {}

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {}

    override fun updatedEntitySystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int) {}

    override fun addedEntity(entityId: Int, components: BitVectorType) {}

    override fun deletedEntity(entityId: Int) {}

    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {}
}
