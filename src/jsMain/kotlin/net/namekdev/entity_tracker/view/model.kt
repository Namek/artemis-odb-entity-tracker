package net.namekdev.entity_tracker.view

import net.namekdev.entity_tracker.model.AspectInfo_Common
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.model.SystemInfo_Common
import net.namekdev.entity_tracker.utils.CommonBitVector
import net.namekdev.entity_tracker.utils.MemoContainer
import net.namekdev.entity_tracker.utils.serialization.ValueTree


typealias SystemInfo = SystemInfo_Common<CommonBitVector>
typealias AspectInfo = AspectInfo_Common<CommonBitVector>
data class CurrentComponent(val entityId: Int, val componentIndex: Int, val valueTree: ValueTree)

class ECSModel {
    val entityComponents = MemoContainer(mutableMapOf<Int, CommonBitVector>())
    val componentTypes = MemoContainer(mutableListOf<ComponentTypeInfo>())
    val allSystems = mutableListOf<SystemInfo>()
    val allManagersNames = mutableListOf<String>()


    fun setComponentType(index: Int, info: ComponentTypeInfo) {
        componentTypes().add(index, info)
    }

    fun addEntity(entityId: Int, components: CommonBitVector) {
        entityComponents()[entityId] = components
    }

    fun removeEntity(entityId: Int) {
        entityComponents().remove(entityId)
    }

    fun getEntityComponents(entityId: Int): CommonBitVector =
        entityComponents()[entityId]!!


    fun getComponentTypeInfo(index: Int): ComponentTypeInfo =
        componentTypes().get(index)

    fun clear() {
        componentTypes().clear()
        entityComponents().clear()
    }
}
