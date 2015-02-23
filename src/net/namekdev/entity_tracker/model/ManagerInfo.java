package net.namekdev.entity_tracker.model;

import com.artemis.Manager;

public class ManagerInfo {
	public String name;
	public Manager manager;

	public ManagerInfo(String name, Manager manager) {
		this.name = name;
		this.manager = manager;
	}
}
