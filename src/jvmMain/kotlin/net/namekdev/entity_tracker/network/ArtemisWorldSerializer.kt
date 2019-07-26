package net.namekdev.entity_tracker.network

import java.util.HashMap

import com.artemis.Component
import com.artemis.utils.Bag
import com.artemis.utils.BitVector
import net.namekdev.entity_tracker.connectors.IWorldController
import net.namekdev.entity_tracker.connectors.IWorldUpdateListener
import net.namekdev.entity_tracker.model.AspectInfo_Common
import net.namekdev.entity_tracker.model.ComponentTypeInfo
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector


/**
 * Server listening to new clients, useful to pass into Entity Tracker itself.
 * Collects data to gather world state for incoming connections.
 *
 * @author Namek
 */
class ArtemisWorldSerializer(server: IServer, inspector: ObjectTypeInspector) : IWorldUpdateListener<BitVector> {
    class SystemData(
        val worldIndex: Int,
        val name: String,
        val aspect: AspectInfo_Common<BitVector>,
        var isEnabled: Boolean,
        var entitiesCount: Int,
        var maxEntitiesCount: Int
    )

    class ManagerData(val name: String, var isEnabled: Boolean)

    private lateinit var _worldController: IWorldController
    private val _listeners = Bag<EntityTrackerCommunicator>()

    private val _systems = Bag<SystemData>()
    private val _componentTypes = Bag<ComponentTypeInfo>()
    private val _entities = HashMap<Int, BitVector>()


    override fun injectWorldController(controller: IWorldController) {
        _worldController = controller
    }

    override fun addedSystem(index: Int, name: String, allTypes: BitVector?, oneTypes: BitVector?, notTypes: BitVector?, isEnabled: Boolean) {
        for (l in _listeners) {
            l.addedSystem(index, name, allTypes, oneTypes, notTypes, isEnabled)
        }
        _systems.set(index, SystemData(index, name, AspectInfo_Common(allTypes, oneTypes, notTypes), isEnabled, 0, 0))
    }

    override fun addedComponentType(index: Int, info: ComponentTypeInfo) {
        var i = 0
        val n = _listeners.size()
        while (i < n) {
            val communicator = _listeners.get(i)
            communicator.addedComponentType(index, info)
            ++i
        }
        _componentTypes.set(index, info)
    }

    override fun updatedSystem(index: Int, entitiesCount: Int, maxEntitiesCount: Int, isEnabled: Boolean) {
        for (l in _listeners) {
            l.updatedSystem(index, entitiesCount, maxEntitiesCount, isEnabled)
        }
        _systems[index]?.let {
            it.entitiesCount = entitiesCount
            it.maxEntitiesCount = maxEntitiesCount
            it.isEnabled = isEnabled
        }
    }

    override fun addedEntity(entityId: Int, components: BitVector) {
        var i = 0
        val n = _listeners.size()
        while (i < n) {
            val communicator = _listeners.get(i)
            communicator.addedEntity(entityId, components)
            ++i
        }
        _entities.put(entityId, components)
    }

    override fun deletedEntity(entityId: Int) {
        var i = 0
        val n = _listeners.size()
        while (i < n) {
            val communicator = _listeners.get(i)
            communicator.deletedEntity(entityId)
            ++i
        }
        _entities.remove(entityId)
    }

    override fun addedComponentTypeToEntities(componentIndex: Int, entityIds: IntArray) {
        for (eid in entityIds) {
            var bits = _entities[eid]
            if (bits == null) {
                bits = BitVector()
                bits.set(componentIndex)
                _entities[eid] = bits
            }

            bits.set(componentIndex, true)
        }

        for (l in _listeners) {
            l.addedComponentTypeToEntities(componentIndex, entityIds)
        }
    }

    override fun removedComponentTypeFromEntities(componentIndex: Int, entityIds: IntArray) {
        for (eid in entityIds) {
            val bits = _entities[eid]
            bits?.set(componentIndex, false)
        }

        for (l in _listeners) {
            l.removedComponentTypeFromEntities(componentIndex, entityIds)
        }
    }

    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
        // `valueTree` is going to be serialized in next layer
        assert(valueTree is Component)

        var i = 0
        val n = _listeners.size()
        while (i < n) {
            val communicator = _listeners.get(i)
            communicator.updatedComponentState(entityId, componentIndex, valueTree)
            ++i
        }
    }

    private val _communicatorProvider = object : RawConnectionCommunicatorProvider {
        override fun getListener(remoteName: String): RawConnectionCommunicator {
            // Server requests communicator for given remote.

            val newCommunicator = object : EntityTrackerCommunicator(inspector) {
                override fun connected(identifier: String, output: RawConnectionOutputListener) {
                    super.connected(identifier, output)
                    injectWorldController(_worldController)


                    run {
                        val n = _systems.size()
                        for (i in 0 until n) {
                            val system = _systems[i]
                            val aspects = system.aspect
                            addedSystem(i, system.name, aspects.allTypes, aspects.oneTypes, aspects.exclusionTypes, system.isEnabled)
                        }
                    }

                    run {
                        val n = _componentTypes.size()
                        for (i in 0 until n) {
                            addedComponentType(i, _componentTypes.get(i))
                        }
                    }

                    val n = _systems.size()
                    for (i in 0 until n) {
                        val system = _systems[i]
                        if (system != null) {
                            updatedSystem(i, system.entitiesCount, system.maxEntitiesCount, system.isEnabled)
                        }
                    }

                    for ((key, value) in _entities) {
                        addedEntity(key, value)
                    }
                }

                override fun worldDisconnected() {
                    _listeners.remove(this)
                }
            }
            _listeners.add(newCommunicator)

            return newCommunicator
        }
    }

    init {
        server.setClientCommunicatorProvider(_communicatorProvider)
    }
}
