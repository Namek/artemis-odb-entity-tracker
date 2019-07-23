package net.namekdev.entity_tracker.view

import net.namekdev.entity_tracker.model.AspectInfo_Common
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.model.SystemInfo_Common
import net.namekdev.entity_tracker.utils.*
import net.namekdev.entity_tracker.utils.serialization.ValueTree


typealias SystemInfo = SystemInfo_Common<CommonBitVector>
typealias AspectInfo = AspectInfo_Common<CommonBitVector>
data class CurrentComponent(val entityId: Int, val componentIndex: Int, val valueTree: ValueTree)

class ECSModel(latestRenderSessionGetter: () -> RenderSession) {
    val entityComponents = ListenableValueContainer(mutableMapOf<Int, CommonBitVector>()).named("ECSModel.entityComponents")
    val componentTypes = ListenableValueContainer(mutableListOf<ComponentTypeInfo>()).named("ECSModel.componentTypes")
    val allSystems = ListenableValueContainer(mutableListOf<SystemInfo>()).named("ECSModel.allSystems")
    val allManagersNames = ListenableValueContainer(mutableListOf<String>()).named("ECSModel.allManagersNames")


    fun setComponentType(index: Int, info: ComponentTypeInfo) {
        componentTypes.update { it.add(index, info) }
    }

    fun addEntity(entityId: Int, components: CommonBitVector) {
        entityComponents.update { it[entityId] = components }
    }

    fun removeEntity(entityId: Int) {
        entityComponents.update { it.remove(entityId) }
    }

    fun getEntityComponents(entityId: Int): CommonBitVector =
        entityComponents()[entityId]!!


    fun getComponentTypeInfo(index: Int): ComponentTypeInfo =
        componentTypes().get(index)

    fun clear() {
        componentTypes.update { it.clear() }
        entityComponents.update { it.clear() }
    }
}
