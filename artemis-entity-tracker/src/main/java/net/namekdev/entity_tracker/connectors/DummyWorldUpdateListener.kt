package net.namekdev.entity_tracker.connectors

import com.artemis.utils.BitVector
import net.namekdev.entity_tracker.model.ComponentTypeInfo

open abstract class DummyWorldUpdateListener : WorldUpdateListener {
    override fun injectWorldController(controller: WorldController) {}

    override val listeningBitset: Int
        get() = 0

    override fun addedSystem(index: Int, name: String, allTypes: BitVector, oneTypes: BitVector, notTypes: BitVector) {}

    override fun addedManager(name: String) {}

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {}

    override fun updatedEntitySystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int) {}

    override fun addedEntity(entityId: Int, components: BitVector) {}

    override fun deletedEntity(entityId: Int) {}

    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {}
}
