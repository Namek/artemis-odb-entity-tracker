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
	public void setSystem(int index, String name) {
		// we assume here that this system is called 
		// by order of system indices.

		_systemIndexMap.ensureSize(index+1);
		int localIndex = _entitySystemsCount++;
		_systemIndexMap.set(localIndex, index);

		super.setSystem(localIndex, name);
		setValueAt(0, localIndex, 2);
		setValueAt(0, localIndex, 3);
	}

	public void updateSystem(int index, int entitiesCount, int maxEntitiesCount) {
		int localIndex = _systemIndexMap.getLocalIndex(index);
		setValueAt(entitiesCount, localIndex, 2);
		setValueAt(maxEntitiesCount, localIndex, 3);
	}

	@Override
	public String getSystemName(int index) {
		int localIndex = _systemIndexMap.getLocalIndex(index);
		return super.getSystemName(localIndex);
	}

	@Override
	public boolean getSystemState(int index) {
		int localIndex = _systemIndexMap.getLocalIndex(index);
		return super.getSystemState(index);
	}	
}
