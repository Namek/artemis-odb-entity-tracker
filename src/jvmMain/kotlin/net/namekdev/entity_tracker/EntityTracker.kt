package net.namekdev.entity_tracker

import com.artemis.*
import com.artemis.Aspect.all
import java.util.HashMap

import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener
import net.namekdev.entity_tracker.utils.ArrayPool
import net.namekdev.entity_tracker.utils.ReflectionUtils
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector
import net.namekdev.entity_tracker.model.*
import net.namekdev.entity_tracker.utils.serialization.setValue

import com.artemis.EntitySubscription.SubscriptionListener
import com.artemis.utils.Bag
import com.artemis.utils.BitVector
import com.artemis.utils.IntBag
import com.artemis.utils.reflect.Method
import com.artemis.utils.reflect.ReflectionException
import net.namekdev.entity_tracker.connectors.IWorldControlListener
import net.namekdev.entity_tracker.utils.serialization.DataType
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode_Server

/**
 * @author Namek
 */
class EntityTracker @JvmOverloads constructor(
    private val componentInspector: ObjectTypeInspector = ObjectTypeInspector(),
    private val updateListener: IWorldUpdateListener<BitVector>,
    var worldControlListener: IWorldControlListener? = null
) : BaseSystem(), IWorldController, SubscriptionListener {
    val systemsInfo = Bag<SystemInfo>()
    val systemsInfoByName: MutableMap<String, SystemInfo> = HashMap()

    val allComponentTypesInfoByClass: MutableMap<Class<Component>, ComponentTypeInfo_Server> = HashMap()
    val allComponentTypesInfo = Bag<ComponentTypeInfo_Server>()
    val allComponentMappers = Bag<BaseComponentMapper<Component>>()
    val watchedComponents = mutableListOf<WatchedComponent>()
    var prevTime = System.nanoTime()
    val targetFps = 10


    protected lateinit var entity_getComponentBits: Method
    protected lateinit var typeFactory: ComponentTypeFactory
    protected lateinit var allComponentTypes: Bag<ComponentType>
    protected lateinit var world_invocationStrategy_disabled: BitVector
    protected val world_invocationStrategy_disabled_previously = BitVector()
    private val tmpBitVector = BitVector()


    private var _notifiedComponentTypesCount = 0
    private val _objectArrPool = ArrayPool(Any::class.java)

    constructor(worldUpdateListener: IWorldUpdateListener<BitVector>, worldControlListener: IWorldControlListener? = null)
        : this(ObjectTypeInspector(), worldUpdateListener, worldControlListener)

    init {
        updateListener.injectWorldController(this)
    }

    override fun initialize() {
        world.aspectSubscriptionManager
            .get(all())
            .addSubscriptionListener(this)

		entity_getComponentBits = ReflectionUtils.getHiddenMethod(Entity::class.java, "getComponentBits")
        typeFactory = ReflectionUtils.getHiddenFieldValue(ComponentManager::class.java, "typeFactory", world.componentManager) as ComponentTypeFactory
        allComponentTypes = ReflectionUtils.getHiddenFieldValue(ComponentTypeFactory::class.java, "types", typeFactory) as Bag<ComponentType>

        val world_invocationStrategy = ReflectionUtils.getHiddenFieldValue(World::class.java, "invocationStrategy", world) as SystemInvocationStrategy
        world_invocationStrategy_disabled = ReflectionUtils.getHiddenFieldValue(SystemInvocationStrategy::class.java, "disabled", world_invocationStrategy) as BitVector
        world_invocationStrategy_disabled_previously.or(world_invocationStrategy_disabled)

        discoverSystems()
    }

    private fun discoverSystems() {
        val systems = world.systems
        for ((index, i) in (0 until systems.size()).withIndex()) {
            val system = systems.get(i)
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
            val info = SystemInfo(index, systemName, system, aspect, aspectInfo, actives, subscription, system.isEnabled)
            systemsInfo.add(info)
            systemsInfoByName.put(systemName, info)

            if (subscription != null) {
                listenForEntitySetChanges(info)
            }

            updateListener.addedSystem(index, systemName, aspectInfo.allTypes, aspectInfo.oneTypes, aspectInfo.exclusionTypes, isEnabled)
        }
    }

    private fun listenForEntitySetChanges(info: SystemInfo) {
        info.subscription!!.addSubscriptionListener(object : SubscriptionListener {
            override fun removed(entities: IntBag) {
                info.entitiesCount -= entities.size()
                updateListener.updatedSystem(info.index, info.entitiesCount, info.maxEntitiesCount, info.isEnabled)
            }

            override fun inserted(entities: IntBag) {
                info.entitiesCount += entities.size()

                if (info.entitiesCount > info.maxEntitiesCount) {
                    info.maxEntitiesCount = info.entitiesCount
                }

                updateListener.updatedSystem(info.index, info.entitiesCount, info.maxEntitiesCount, info.isEnabled)
            }
        })
    }

    override fun checkProcessing(): Boolean {
        tmpBitVector.clear()
        tmpBitVector.or(world_invocationStrategy_disabled_previously)
        tmpBitVector.xor(world_invocationStrategy_disabled)

        var i = tmpBitVector.nextSetBit(0)
        val n = tmpBitVector.length()
        while (i in 0 until n) {
            val s = systemsInfo[i]
            updateListener.updatedSystem(s.index, s.entitiesCount, s.maxEntitiesCount, s.system.isEnabled)

            i = tmpBitVector.nextSetBit(i + 1)
        }

        world_invocationStrategy_disabled_previously.clear()
        world_invocationStrategy_disabled_previously.or(world_invocationStrategy_disabled)

        if (watchedComponents.isEmpty())
            return false

        val minDiffToProcess = 1000000000 / targetFps
        val curTime = System.nanoTime()
        val diff = curTime - prevTime
        return if (diff >= minDiffToProcess) {
            prevTime = curTime
            true
        }
        else false
    }

    override fun processSystem() {
        synchronized(watchedComponents) {
            for (wc in watchedComponents) {
                requestComponentState(wc.entityId, wc.componentIndex)
            }
        }
    }

    override fun inserted(entities: IntBag) {
        val ids = entities.data
        val size = entities.size()
        var i = 0
        while (size > i) {
            val entityId = ids[i]
            val entity = world.getEntity(entityId)

            var componentBitVector: BitVector? = null
            try {
                componentBitVector = entity_getComponentBits.invoke(entity) as BitVector
            }
            catch (exc: ReflectionException) {
                throw RuntimeException(exc)
            }

            if (componentBitVector.length() > _notifiedComponentTypesCount) {
                inspectNewComponentTypesAndNotify()
            }

            updateListener.addedEntity(entityId, componentBitVector)

            ++i
        }
    }

    override fun removed(entities: IntBag) {
        val ids = entities.data
        val size = entities.size()
        var i = 0
        while (size > i) {
            val entityId = ids[i]

            synchronized(watchedComponents) {
                watchedComponents.removeAll {
                    it.clientId == 0 && it.entityId == entityId
                }
            }

            updateListener.deletedEntity(entityId)

            ++i
        }
    }

    private fun inspectNewComponentTypesAndNotify() {
        val index = _notifiedComponentTypesCount
        val n = allComponentTypes.size()

        for (i in index until n) {
            val type = ReflectionUtils.getHiddenFieldValue(ComponentType::class.java, "type", allComponentTypes.get(i)) as Class<Component>

            val info = inspectComponentType(type)
            info.index = i

            allComponentTypesInfoByClass.put(type, info)
            allComponentTypesInfo.set(i, info)
            allComponentMappers.set(i, ComponentMapper.getFor(type, world))

            updateListener.addedComponentType(i, info)
            ++_notifiedComponentTypesCount

            world.aspectSubscriptionManager.get(Aspect.all(type))
                .addSubscriptionListener(object: SubscriptionListener {
                    override fun inserted(entities: IntBag) {
                        val entityIds = entities.data.sliceArray(IntRange(0, entities.size() - 1))
                        updateListener.addedComponentTypeToEntities(i, entityIds)
                    }

                    override fun removed(entities: IntBag) {
                        val entityIds = entities.data.sliceArray(IntRange(0, entities.size() - 1))
                        updateListener.removedComponentTypeFromEntities(i, entityIds)
                    }
                })
        }
    }

    private fun inspectComponentType(type: Class<Component>): ComponentTypeInfo_Server {
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

    override fun requestComponentState(entityId: Int, componentIndex: Int) {
        //val info = allComponentTypesInfo.get(componentIndex)
        val mapper = allComponentMappers.get(componentIndex)
        val component = mapper.getSafe(entityId, null)

        if (component != null)
            updateListener.updatedComponentState(entityId, componentIndex, component)
        else
            updateListener.removedComponentTypeFromEntities(componentIndex, IntArray(entityId))
    }

    override fun setComponentFieldValue(entityId: Int, componentIndex: Int, treePath: IntArray, newValueDataType: DataType, newValue: Any?) {
        val info = allComponentTypesInfo.get(componentIndex)
        val mapper = allComponentMappers.get(componentIndex)

        val component = mapper.get(entityId)
        (info.model as ObjectModelNode_Server).setValue(component, treePath, newValue)

        worldControlListener?.let {
            it.onComponentFieldValueChanged(entityId, componentIndex, treePath, newValue)
        }
    }

    override fun setComponentStateWatcher(entityId: Int, componentIndex: Int, enabled: Boolean) {
        synchronized(watchedComponents) {
            val wcIndex = watchedComponents.indexOfFirst { wc ->
                wc.clientId == 0 && wc.entityId == entityId && wc.componentIndex == componentIndex
            }
            if (enabled && wcIndex < 0) {
                // what's interesting, we don't check if such entity even exists... and what if it doesn't?
                watchedComponents.add(WatchedComponent(0, entityId, componentIndex))
            }
            else if (!enabled && wcIndex >= 0)
                watchedComponents.removeAt(wcIndex)

            Unit
        }
    }

    override fun deleteEntity(entityId: Int) {
        world.delete(entityId)
    }
}

data class WatchedComponent(val clientId: Int, val entityId: Int, val componentIndex: Int)
