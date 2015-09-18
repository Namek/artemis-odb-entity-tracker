package net.namekdev.entity_tracker.ui;

import java.util.Vector;

import net.namekdev.entity_tracker.connectors.DummyWorldUpdateListener;
import net.namekdev.entity_tracker.connectors.WorldUpdateListener;

public class EventBus extends DummyWorldUpdateListener {
	private final Vector<WorldUpdateListener> _listeners = new Vector<WorldUpdateListener>(10);


	public void registerListener(WorldUpdateListener listener) {
		_listeners.add(listener);
	}

	public void unregisterListener(WorldUpdateListener listener) {
		_listeners.remove(listener);
	}


	@Override
	public void updatedComponentState(int entityId, int componentIndex, Object valueTree) {
		for (int i = 0, n = _listeners.size(); i < n; ++i) {
			_listeners.get(i).updatedComponentState(entityId, componentIndex, valueTree);
		}
	}
}
