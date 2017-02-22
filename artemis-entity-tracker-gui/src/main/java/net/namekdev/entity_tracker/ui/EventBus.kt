package net.namekdev.entity_tracker.ui

import java.util.Vector

import net.namekdev.entity_tracker.connectors.DummyWorldUpdateListener
import net.namekdev.entity_tracker.connectors.WorldUpdateListener

class EventBus : DummyWorldUpdateListener() {
    private val _listeners = Vector<WorldUpdateListener>(10)


    fun registerListener(listener: WorldUpdateListener) {
        _listeners.add(listener)
    }

    fun unregisterListener(listener: WorldUpdateListener) {
        _listeners.remove(listener)
    }


    override fun updatedComponentState(entityId: Int, componentIndex: Int, valueTree: Any) {
        var i = 0
        val n = _listeners.size
        while (i < n) {
            _listeners[i].updatedComponentState(entityId, componentIndex, valueTree)
            ++i
        }
    }
}
