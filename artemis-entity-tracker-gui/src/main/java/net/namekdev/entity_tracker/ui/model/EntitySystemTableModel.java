package net.namekdev.entity_tracker.ui.model;

import net.namekdev.entity_tracker.utils.IndexBiMap;

public class EntitySystemTableModel extends BaseSystemTableModel {
	protected IndexBiMap _systemIndexMap = new IndexBiMap(100);
	protected int _entitySystemsCount = 0;

	
	public EntitySystemTableModel() {
		super();
		addColumn("entities");
		addColumn("max entities");
	}
	

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case 2:
			case 3:
				return Integer.class;
			default:
				return super.getColumnClass(columnIndex);
		}
	}
	
	@Override
	public int getSystemIndex(int rowIndex) {
		return _systemIndexMap.getGlobalIndex(rowIndex);
	}

	@Override
	public void setSystem(int systemIndex, String name) {
		// we assume here that this system is called 
		// by order of system indices.

		_systemIndexMap.ensureSize(systemIndex+1);
		int rowIndex = _entitySystemsCount++;
		_systemIndexMap.set(rowIndex, systemIndex);

		super.setSystem(rowIndex, name);
		setValueAt(0, rowIndex, 2);
		setValueAt(0, rowIndex, 3);
	}
	
	/**
	 * Update system state without firing events.
	 */
	public void updateSystemState(int systemIndex, boolean enabled) {
		int rowIndex = _systemIndexMap.getLocalIndex(systemIndex);
		
		if (rowIndex >= 0) {
			updateValueAt(enabled, rowIndex, 0);
		}
	}

	public void updateSystem(int systemIndex, int entitiesCount, int maxEntitiesCount) {
		int rowIndex = _systemIndexMap.getLocalIndex(systemIndex);
		setValueAt(entitiesCount, rowIndex, 2);
		setValueAt(maxEntitiesCount, rowIndex, 3);
	}

	@Override
	public String getSystemName(int systemIndex) {
		int rowIndex = _systemIndexMap.getLocalIndex(systemIndex);
		return super.getSystemName(rowIndex);
	}

	@Override
	public boolean getSystemState(int systemIndex) {
		int rowIndex = _systemIndexMap.getLocalIndex(systemIndex);
		return super.getSystemState(rowIndex);
	}	
}
