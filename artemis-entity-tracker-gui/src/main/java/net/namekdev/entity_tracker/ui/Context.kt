package net.namekdev.entity_tracker.ui

import net.namekdev.entity_tracker.connectors.WorldController

class Context {
    var worldController: WorldController? = null
    val eventBus = EventBus()
}
