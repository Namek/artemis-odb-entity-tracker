package net.namekdev.entity_tracker

import java.util.HashMap

import net.namekdev.entity_tracker.connectors.WorldController
import net.namekdev.entity_tracker.connectors.WorldUpdateListener
import net.namekdev.entity_tracker.utils.ArrayPool
import net.namekdev.entity_tracker.utils.ReflectionUtils
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector
import net.namekdev.entity_tracker.model.*
import net.namekdev.entity_tracker.utils.serialization.setValue

import com.artemis.Aspect
import com.artemis.BaseComponentMapper
import com.artemis.BaseEntitySystem
import com.artemis.Component
import com.artemis.ComponentManager
import com.artemis.ComponentMapper
import com.artemis.ComponentType
import com.artemis.ComponentTypeFactory
import com.artemis.Entity
import com.artemis.EntitySubscription
import com.artemis.EntitySubscription.SubscriptionListener
import com.artemis.Manager
import com.artemis.utils.Bag
import com.artemis.utils.BitVector
import com.artemis.utils.IntBag
import com.artemis.utils.reflect.Method
import com.artemis.utils.reflect.ReflectionException

/**
 * @author Namek
 */
class EntityTracker @JvmOverloads constructor(
    private val componentInspector: ObjectTypeInspector = ObjectTypeInspector(),
    listener: WorldUpdateListener<BitVector>? = null
) : Manager(), WorldController {
    private var updateListener: WorldUpdateListener<BitVector>? = null

    val systemsInfo = Bag<SystemInfo>()
    val systemsInfoByName: MutableMap<String, SystemInfo> = HashMap()

    val managersInfo = Bag<ManagerInfo>()
    val managersInfoByName: MutableMap<String, ManagerInfo> = HashMap()
    val allComponentTypesInfoByClass: MutableMap<Class<Component>, ComponentTypeInfo> = HashMap()
    val allComponentTypesInfo = Bag<ComponentTypeInfo>()
    val allComponentMappers = Bag<BaseComponentMapper<Component>>()


    protected lateinit var entity_getComponentBits: Method
    protected lateinit var typeFactory: ComponentTypeFactory
    protected lateinit var allComponentTypes: Bag<ComponentType>


    private var _notifiedComponentTypesCount = 0
    private val _objectArrPool = ArrayPool(Any::class.java)

    constructor(listener: WorldUpdateListener<BitVector>) : this(ObjectTypeInspector(), listener) {}

    init {
        setUpdateListener(listener)
    }

    fun setUpdateListener(listener: WorldUpdateListener<BitVector>?) {
        this.updateListener = listener
        listener?.injectWorldController(this)
    }


    override fun initialize() {
		entity_getComponentBits = ReflectionUtils.getHiddenMethod(Entity::class.java, "getComponentBits")
        typeFactory = ReflectionUtils.getHiddenFieldValue(ComponentManager::class.java, "typeFactory", world.componentManager) as ComponentTypeFactory
        allComponentTypes = ReflectionUtils.getHiddenFieldValue(ComponentTypeFactory::class.java, "types", typeFactory) as Bag<ComponentType>

        find42UnicornManagers()
    }

    private fun find42UnicornManagers() {
        val systems = world.systems
        var index = 0
        run {
            var i = 0
            val n = systems.size()
            while (i < n) {
                val system = systems.get(i)

                if (system is Manager) {
                    ++i
                    continue
                }

                val systemType = system.javaClass
                val systemName = systemType.simpleName
                var aspect: Aspect? = null
                var actives: BitVector? = null
                var subscription: EntitySubscription? = null

                if (system is BaseEntitySystem) {
                    subscription = system.subscription
                    aspect = subscription!!.aspect
                    actives = subscription.activeEntityIds
                }

                val aspectInfo = AspectInfo(aspect?.allSet, aspect?.oneSet, aspect?.exclusionSet)
                val info = SystemInfo(index, systemName, system, aspect, aspectInfo, actives, subscription)
                systemsInfo.add(info)
                systemsInfoByName.put(systemName, info)

                if (subscription != null) {
                    listenForEntitySetChanges(info)
                }

                updateListener!!.addedSystem(index++, systemName, aspectInfo.allTypes, aspectInfo.oneTypes, aspectInfo.exclusionTypes)
                ++i
            }
        }

        var i = 0
        val n = systems.size()
        while (i < n) {
            val system = systems.get(i)

            if (system !is Manager) {
                ++i
                continue
            }

            val managerType = system.javaClass
            val managerName = managerType.simpleName

            val info = ManagerInfo(managerName, system)
            managersInfo.add(info)
            managersInfoByName.put(managerName, info)

            updateListener!!.addedManager(managerName)
            ++i
        }
    }

    private fun listenForEntitySetChanges(info: SystemInfo) {
        info.subscription!!.addSubscriptionListener(object : SubscriptionListener {
            override fun removed(entities: IntBag) {
                info.entitiesCount -= entities.size()

                if (updateListener != null && updateListener!!.listeningBitset and WorldUpdateListener.ENTITY_SYSTEM_STATS != 0) {
                    updateListener!!.updatedEntitySystem(info.index, info.entitiesCount, info.maxEntitiesCount)
                }
            }

            override fun inserted(entities: IntBag) {
                info.entitiesCount += entities.size()

                if (info.entitiesCount > info.maxEntitiesCount) {
                    info.maxEntitiesCount = info.entitiesCount
                }

                if (updateListener != null && updateListener!!.listeningBitset and WorldUpdateListener.ENTITY_SYSTEM_STATS != 0) {
                    updateListener!!.updatedEntitySystem(info.index, info.entitiesCount, info.maxEntitiesCount)
                }
            }
        })
    }

    override fun added(e: Entity?) {
        if (updateListener == null) {
            return
        }

        if (updateListener!!.listeningBitset and WorldUpdateListener.ENTITY_ADDED == 0) {
            return
        }

        var componentBitVector: BitVector? = null
        try {
            componentBitVector = entity_getComponentBits.invoke(e) as BitVector
        }
        catch (exc: ReflectionException) {
            throw RuntimeException(exc)
        }

        if (componentBitVector.length() > _notifiedComponentTypesCount) {
            inspectNewComponentTypesAndNotify()
        }

        updateListener!!.addedEntity(e!!.id, componentBitVector)
    }

    override fun deleted(e: Entity?) {
        if (updateListener == null || updateListener!!.listeningBitset and WorldUpdateListener.ENTITY_DELETED == 0) {
            return
        }

        updateListener!!.deletedEntity(e!!.id)
    }

    private fun inspectNewComponentTypesAndNotify() {
        val index = _notifiedComponentTypesCount
        val n = allComponentTypes.size()

        for (i in index..n - 1) {
            val type = ReflectionUtils.getHiddenFieldValue(ComponentType::class.java, "type", allComponentTypes.get(i)) as Class<Component>

            val info = inspectComponentType(type)
            info.index = i

            allComponentTypesInfoByClass.put(type, info)
            allComponentTypesInfo.set(i, info)
            allComponentMappers.set(i, ComponentMapper.getFor(type, world))

            updateListener!!.addedComponentType(i, info)
            ++_notifiedComponentTypesCount
        }
    }

    private fun inspectComponentType(type: Class<Component>): ComponentTypeInfo {
        val info = ComponentTypeInfo_Server(type)
        info.model = componentInspector.inspect(type)

        return info
    }


    //////////////////////////////////////
    // World Controller interface

    override fun setSystemState(name: String, isOn: Boolean) {
        val info = systemsInfoByName[name]!!
        info.system.isEnabled = isOn
    }

    override fun setManagerState(name: String, isOn: Boolean) {
        val info = managersInfoByName[name]!!
        info.manager.isEnabled = isOn
    }

    override fun requestComponentState(entityId: Int, componentIndex: Int) {
        val info = allComponentTypesInfo.get(componentIndex)
        val mapper = allComponentMappers.get(componentIndex)

        val component = mapper.get(entityId)
        updateListener!!.updatedComponentState(entityId, componentIndex, component)
    }

    override fun setComponentFieldValue(entityId: Int, componentIndex: Int, treePath: IntArray, value: Any) {
        val info = allComponentTypesInfo.get(componentIndex)
        val mapper = allComponentMappers.get(componentIndex)

        val component = mapper.get(entityId)
        info.model.setValue(component, treePath, value)
    }
}
