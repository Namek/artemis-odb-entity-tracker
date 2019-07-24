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
import net.namekdev.entity_tracker.utils.tuple.Tuple3


/**
 * Server listening to new clients, useful to pass into Entity Tracker itself.
 * Collects data to gather world state for incoming connections.
 *
 * @author Namek
 */
class ArtemisWorldSerializer(server: IServer, inspector: ObjectTypeInspector) : IWorldUpdateListener<BitVector> {
    private lateinit var _worldController: IWorldController
    private val _listeners = Bag<EntityTrackerCommunicator>()

    private val _managers = Bag<String>()
    private val _systems = Bag<Tuple3<Int, String, AspectInfo_Common<BitVector>>>()
    private val _componentTypes = Bag<ComponentTypeInfo>()
    private val _entities = HashMap<Int, BitVector>()
    private val _entitySystemsEntitiesCount = Bag<Int>()
    private val _entitySystemsMaxEntitiesCount = Bag<Int>()


    override fun injectWorldController(controller: IWorldController) {
        _worldController = controller
    }

    override fun addedSystem(index: Int, name: String, allTypes: BitVector?, oneTypes: BitVector?, notTypes: BitVector?) {
        var i = 0
        val n = _listeners.size()
        while (i < n) {
            val communicator = _listeners.get(i)
            communicator.addedSystem(index, name, allTypes, oneTypes, notTypes)
            ++i
        }
        _systems.add(Tuple3.create(index, name, AspectInfo_Common(allTypes, oneTypes, notTypes)))
    }

    override fun addedManager(name: String) {
        var i = 0
        val n = _listeners.size()
        while (i < n) {
            val communicator = _listeners.get(i)
            communicator.addedManager(name)
            ++i
        }
        _managers.add(name)
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

    override fun updatedEntitySystem(systemIndex: Int, entitiesCount: Int, maxEntitiesCount: Int) {
        var i = 0
        val n = _listeners.size()
        while (i < n) {
            val communicator = _listeners.get(i)
            communicator.updatedEntitySystem(systemIndex, entitiesCount, maxEntitiesCount)
            ++i
        }
        _entitySystemsEntitiesCount.set(systemIndex, entitiesCount)
        _entitySystemsMaxEntitiesCount.set(systemIndex, maxEntitiesCount)
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
                        var i = 0
                        val n = _systems.size()
                        while (i < n) {
                            val system = _systems.get(i)
                            val aspects = system.item3
                            addedSystem(system.item1, system.item2, aspects.allTypes, aspects.oneTypes, aspects.exclusionTypes)
                            ++i
                        }
                    }

                    run {
                        var i = 0
                        val n = _managers.size()
                        while (i < n) {
                            addedManager(_managers.get(i))
                            ++i
                        }
                    }

                    run {
                        var i = 0
                        val n = _componentTypes.size()
                        while (i < n) {
                            addedComponentType(i, _componentTypes.get(i))
                            ++i
                        }
                    }

                    var i = 0
                    val n = _systems.size()
                    while (i < n) {
                        if (_entitySystemsEntitiesCount.get(i) != null) {
                            val entitiesCount = _entitySystemsEntitiesCount.get(i)
                            val maxEntitiesCount = _entitySystemsMaxEntitiesCount.get(i)
                            updatedEntitySystem(i, entitiesCount, maxEntitiesCount)
                        }
                        ++i
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
