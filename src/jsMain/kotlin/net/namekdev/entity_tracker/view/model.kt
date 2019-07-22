package net.namekdev.entity_tracker.view

import net.namekdev.entity_tracker.model.AspectInfo_Common
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.model.SystemInfo_Common
import net.namekdev.entity_tracker.utils.CommonBitVector
import net.namekdev.entity_tracker.utils.ValueContainer
import net.namekdev.entity_tracker.utils.named
import net.namekdev.entity_tracker.utils.serialization.ValueTree


typealias SystemInfo = SystemInfo_Common<CommonBitVector>
typealias AspectInfo = AspectInfo_Common<CommonBitVector>
data class CurrentComponent(val entityId: Int, val componentIndex: Int, val valueTree: ValueTree)

class ECSModel(notifyChanged: () -> Unit) {
    val entityComponents = ValueContainer(mutableMapOf<Int, CommonBitVector>()).named("ECSModel.entityComponents")
    val componentTypes = ValueContainer(mutableListOf<ComponentTypeInfo>()).named("ECSModel.componentTypes")
    val allSystems = ValueContainer(mutableListOf<SystemInfo>()).named("ECSModel.allSystems")
    val allManagersNames = ValueContainer(mutableListOf<String>()).named("ECSModel.allManagersNames")


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
