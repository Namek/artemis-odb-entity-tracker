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

enum class AspectPartType {
    All, One, Exclude
}
enum class ComponentTypeFilter {
    Include, Exclude
}
enum class MatchType {
    Match, AntiMatch, NoMatch
}

enum class WorldViewLayout {
    Entities__Systems_Component,
    Entities_Component__Systems
}

class ECSModel : IWorldUpdateListener<CommonBitVector> {
    val entityComponents = ValueContainer(IdMap<CommonBitVector>()).named("ECSModel.entityComponents")
    val componentTypes = ValueContainer(mutableListOf<ComponentTypeInfo>()).named("ECSModel.componentTypes")
    val allSystems = ValueContainer(mutableListOf<SystemInfo>()).named("ECSModel.allSystems")

    val highlightedComponentTypes = ValueContainer(mutableMapOf<Int, AspectPartType>())
    val entityFilterByComponentType = ValueContainer(mutableMapOf<Int, ComponentTypeFilter>())
    val worldViewLayout = ValueContainer<WorldViewLayout>(WorldViewLayout.Entities_Component__Systems)


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

    val tmpInts = mutableListOf<Int>()
    fun setHighlightedComponentTypes(aspect: AspectInfo_Common<CommonBitVector>, shouldSet: Boolean) {
        if (aspect.isEmpty) {
            return
        }

        highlightedComponentTypes.update {
            for (types in arrayOf(aspect.allTypes!!, aspect.oneTypes!!, aspect.exclusionTypes!!)) {
                for (i in types.toIntBag(tmpInts)) {
                    if (shouldSet)
                        it.put(i, AspectPartType.All)
                    else
                        it.remove(i)
                }
            }
        }
    }

    fun toggleComponentTypeFilter(componentTypeIndex: Int) {
        entityFilterByComponentType.update { filters ->
            when (filters[componentTypeIndex]) {
                null ->
                    filters[componentTypeIndex] = ComponentTypeFilter.Include

                ComponentTypeFilter.Include ->
                    filters[componentTypeIndex] = ComponentTypeFilter.Exclude

                ComponentTypeFilter.Exclude ->
                    filters.remove(componentTypeIndex)
            }
        }
    }

    // check if aspect is fully set in filter
    fun scanAspectMatchToFilter(aspect: AspectInfo_Common<CommonBitVector>): MatchType {
        if (aspect.isEmpty) {
            // this case is actually undefined, empty aspect should be checked against filter, because what should it filter against?
            return MatchType.NoMatch
        }

        var expect: MatchType? = null

        val filters = entityFilterByComponentType()
        for (types in arrayOf(aspect.allTypes!!, aspect.oneTypes!!)) {
            for (i in types.toIntBag(tmpInts)) {
                if (expect == null) {
                    expect = when (filters[i]) {
                        ComponentTypeFilter.Include -> MatchType.Match
                        ComponentTypeFilter.Exclude -> MatchType.AntiMatch
                        null -> MatchType.NoMatch
                    }
                }
                else {
                    val m = when (filters[i]) {
                        ComponentTypeFilter.Include -> MatchType.Match
                        ComponentTypeFilter.Exclude -> MatchType.AntiMatch
                        null -> MatchType.NoMatch
                    }

                    if (m != expect) {
                        return MatchType.NoMatch
                    }
                }
            }
        }
        for (i in aspect.exclusionTypes!!.toIntBag(tmpInts)) {
            if (expect == null) {
                expect = when (filters[i]) {
                    ComponentTypeFilter.Include -> MatchType.AntiMatch
                    ComponentTypeFilter.Exclude -> MatchType.Match
                    null -> MatchType.NoMatch
                }
            }
            else {
                val m = when (filters[i]) {
                    ComponentTypeFilter.Include -> MatchType.AntiMatch
                    ComponentTypeFilter.Exclude -> MatchType.Match
                    null -> MatchType.NoMatch
                }

                if (m != expect) {
                    return MatchType.NoMatch
                }
            }
        }

        return expect ?: MatchType.NoMatch
    }

    fun setFilterByAspect(aspect: AspectInfo_Common<CommonBitVector>, shouldSet: MatchType) {
        if (aspect.isEmpty) {
            return
        }

        entityFilterByComponentType.update { filters ->
            for (types in arrayOf(aspect.allTypes!!, aspect.oneTypes!!)) {
                for (i in types.toIntBag(tmpInts)) {
                    when (shouldSet) {
                        MatchType.Match ->
                            filters.put(i, ComponentTypeFilter.Include)
                        MatchType.AntiMatch ->
                            filters.put(i, ComponentTypeFilter.Exclude)
                        MatchType.NoMatch ->
                            filters.remove(i)
                    }
                }
            }

            for (i in aspect.exclusionTypes!!.toIntBag(tmpInts)) {
                when (shouldSet) {
                    MatchType.Match ->
                        filters.put(i, ComponentTypeFilter.Exclude)
                    MatchType.AntiMatch ->
                        filters.put(i, ComponentTypeFilter.Include)
                    MatchType.NoMatch ->
                        filters.remove(i)
                }
            }
        }
    }

    fun switchFilterAySpect(aspect: AspectInfo_Common<CommonBitVector>) {
        val currentMatch = scanAspectMatchToFilter(aspect)
        val newMatch = when (currentMatch) {
            MatchType.NoMatch -> MatchType.Match
            MatchType.Match -> MatchType.AntiMatch
            MatchType.AntiMatch -> MatchType.NoMatch
        }
        setFilterByAspect(aspect, newMatch)
    }
}
