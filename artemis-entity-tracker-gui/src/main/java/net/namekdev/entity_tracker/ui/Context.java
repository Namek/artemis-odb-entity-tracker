package net.namekdev.entity_tracker.ui;

import net.namekdev.entity_tracker.connectors.WorldController;

public class Context {
	public WorldController worldController;
	public final EventBus eventBus = new EventBus();
}
