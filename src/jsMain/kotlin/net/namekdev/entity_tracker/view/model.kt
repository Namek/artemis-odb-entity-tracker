package net.namekdev.entity_tracker.view

import net.namekdev.entity_tracker.connectors.IWorldUpdateListener
import net.namekdev.entity_tracker.model.AspectInfo_Common
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.model.SystemInfo_Common
import net.namekdev.entity_tracker.utils.*
import net.namekdev.entity_tracker.utils.serialization.ValueTree


typealias SystemInfo = SystemInfo_Common<CommonBitVector>
typealias AspectInfo = AspectInfo_Common<CommonBitVector>

class WatchedEntity(var entityId: Int?, var componentIndex: Int, var valueTree: ValueTree?, var watchEnabled: Boolean = true)

class ECSModel : IWorldUpdateListener<CommonBitVector> {
    val entityComponents = ValueContainer(mutableMapOf<Int, CommonBitVector>()).named("ECSModel.entityComponents")
    val componentTypes = ValueContainer(mutableListOf<ComponentTypeInfo>()).named("ECSModel.componentTypes")
    val allSystems = ValueContainer(mutableListOf<SystemInfo>()).named("ECSModel.allSystems")


    fun setComponentType(index: Int, info: ComponentTypeInfo) {
        componentTypes.update { it.add(index, info) }
    }

    fun addEntity(entityId: Int, components: CommonBitVector) {
        entityComponents.update { it[entityId] = components }
    }

    fun removeEntity(entityId: Int) {
        entityComponents.update { it.remove(entityId) }
    }

    fun setComponentTypeOnEntities(componentIndex: Int, entityIds: IntArray, isSet: Boolean) {
        entityComponents.update {
            for (eid in entityIds) {
                it[eid]?.set(componentIndex, isSet)
            }
        }
    }

    fun getEntityComponents(entityId: Int): CommonBitVector =
        entityComponents()[entityId]!!


    fun getComponentTypeInfo(index: Int): ComponentTypeInfo =
        componentTypes().get(index)

    fun clear() {
        componentTypes.update { it.clear() }
        entityComponents.update { it.clear() }
        allSystems.update { it.clear() }
    }

    override fun worldDisconnected() {
        clear()
    }

    override fun addedSystem(
        index: Int,
        name: String,
        allTypes: CommonBitVector?,
        oneTypes: CommonBitVector?,
        notTypes: CommonBitVector?,
        isEnabled: Boolean
    ) {
        val aspectInfo = AspectInfo(allTypes, oneTypes, notTypes)
        val actives = if (aspectInfo.isEmpty) null else CommonBitVector()
        val systemInfo = SystemInfo(index, name, aspectInfo, actives, isEnabled)

        allSystems.update { it.add(index, systemInfo) }
    }

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
        setComponentType(index, info)
    }

    override fun updatedSystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int, isEnabled: Boolean) {
        allSystems.update {
            val system = it[index]
            system.entitiesCount = entitiesCount
            system.maxEntitiesCount = maxEntitiesCount
            system.isEnabled = isEnabled
        }
    }

    override fun addedEntity(entityId: Int, components: CommonBitVector) {
        addEntity(entityId, components)
    }

    override fun deletedEntity(entityId: Int) {
        removeEntity(entityId)
    }

    override fun addedComponentTypeToEntities(componentIndex: Int, entityIds: IntArray) {
        setComponentTypeOnEntities(componentIndex, entityIds, true)
    }

    override fun removedComponentTypeFromEntities(componentIndex: Int, entityIds: IntArray) {
        setComponentTypeOnEntities(componentIndex, entityIds, false)
    }
}
