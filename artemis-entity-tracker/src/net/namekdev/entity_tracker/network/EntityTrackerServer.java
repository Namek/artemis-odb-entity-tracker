package net.namekdev.entity_tracker.network;

import java.net.SocketAddress;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.artemis.utils.Bag;

import net.namekdev.entity_tracker.connectors.UpdateListener;
import net.namekdev.entity_tracker.network.base.RawConnectionCommunicator;
import net.namekdev.entity_tracker.network.base.RawConnectionCommunicatorProvider;
import net.namekdev.entity_tracker.network.base.RawConnectionOutputListener;
import net.namekdev.entity_tracker.network.base.Server;
import net.namekdev.entity_tracker.utils.tuple.Tuple2;
import net.namekdev.entity_tracker.utils.tuple.Tuple4;

/**
 * Server listening to new clients, useful to pass into Entity Tracker itself.
 * Collects data to gather world state for incoming connections.
 *
 * @author Namek
 */
public class EntityTrackerServer extends Server implements UpdateListener {
	private Bag<EntityTrackerCommunicator> _listeners = new Bag<EntityTrackerCommunicator>();

	private Bag<String> _managers = new Bag<String>();
	private Bag<Tuple4<String, BitSet, BitSet, BitSet>> _systems = new Bag<Tuple4<String,BitSet,BitSet,BitSet>>();
	private Bag<String> _componentTypes = new Bag<String>();
	private Map<Integer, BitSet> _entities = new HashMap<Integer,BitSet>();


	public EntityTrackerServer() {
		super();
		super.clientListenerProvider = _communicatorProvider;
	}


	@Override
	public int getListeningBitset() {
		// TODO
		return ENTITY_ADDED | ENTITY_DELETED;
	}

	@Override
	public void addedSystem(String name, BitSet allTypes, BitSet oneTypes, BitSet notTypes) {
		for (int i = 0, n = _listeners.size(); i < n; ++i) {
			EntityTrackerCommunicator communicator = _listeners.get(i);
			communicator.addedSystem(name, allTypes, oneTypes, notTypes);
		}
		_systems.add(Tuple4.create(name, allTypes, oneTypes, notTypes));
	}

	@Override
	public void addedManager(String name) {
		for (int i = 0, n = _listeners.size(); i < n; ++i) {
			EntityTrackerCommunicator communicator = _listeners.get(i);
			communicator.addedManager(name);
		}
		_managers.add(name);
	}

	@Override
	public void addedComponentType(int index, String name) {
		for (int i = 0, n = _listeners.size(); i < n; ++i) {
			EntityTrackerCommunicator communicator = _listeners.get(i);
			communicator.addedComponentType(index, name);
		}
		_componentTypes.set(index, name);
	}

	@Override
	public void addedEntity(int entityId, BitSet components) {
		for (int i = 0, n = _listeners.size(); i < n; ++i) {
			EntityTrackerCommunicator communicator = _listeners.get(i);
			communicator.addedEntity(entityId, components);
		}
		_entities.put(entityId, components);
	}

	@Override
	public void deletedEntity(int entityId) {
		for (int i = 0, n = _listeners.size(); i < n; ++i) {
			EntityTrackerCommunicator communicator = _listeners.get(i);
			communicator.deletedEntity(entityId);
		}
		_entities.remove(entityId);
	}

	// TODO handle disconnection!

	private RawConnectionCommunicatorProvider _communicatorProvider = new RawConnectionCommunicatorProvider() {
		@Override
		public RawConnectionCommunicator getListener(String remoteName) {
			// Server requests communicator for given remote.

			EntityTrackerCommunicator newCommunicator = new EntityTrackerCommunicator() {

				@Override
				public void connected(SocketAddress remoteAddress, RawConnectionOutputListener output) {
					super.connected(remoteAddress, output);

					for (int i = 0; i < _systems.size(); ++i) {
						Tuple4<String, BitSet, BitSet, BitSet> system = _systems.get(i);
						addedSystem(system.item1, system.item2, system.item3, system.item4);
					}

					for (int i = 0; i < _managers.size(); ++i) {
						addedManager(_managers.get(i));
					}

					for (int i = 0; i < _componentTypes.size(); ++i) {
						addedComponentType(i, _componentTypes.get(i));
					}

					for (Entry<Integer, BitSet> entity : _entities.entrySet()) {
						addedEntity(entity.getKey(), entity.getValue());
					}
				}

			};
			_listeners.add(newCommunicator);

			return newCommunicator;
		}
	};
}
