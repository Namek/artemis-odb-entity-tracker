package net.namekdev.entity_tracker.ui.model;

import javax.swing.table.DefaultTableModel;

public class SystemTableModel extends DefaultTableModel {
	public SystemTableModel() {
		addColumn("");
		addColumn("system");
		addColumn("entities");
		addColumn("max entities");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case 0: return Boolean.class;
			case 1: return String.class;
			default: return Integer.class;
		}
	}

	public boolean isCellEditable(int row, int column) {
		return column == 0;
	}

	public void setSystem(int index, String name, boolean hasAspect) {
		for (int i = getRowCount(); i <= index; ++i) {
			addRow(new Object[] { true, "", hasAspect ? 0 : null });
		}

		setValueAt(name, index, 1);
	}

	public void updateSystem(int index, int entitiesCount, int maxEntitiesCount) {
		setValueAt(entitiesCount, index, 2);
		setValueAt(maxEntitiesCount, index, 3);
	}

	public String getSystemName(int index) {
		return (String) getValueAt(index, 1);
	}

	public boolean getSystemState(int index) {
		return (boolean) getValueAt(index, 0);
	}

	public void clear() {
		setRowCount(0);
	}
}
