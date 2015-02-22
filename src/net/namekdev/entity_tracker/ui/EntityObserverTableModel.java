package net.namekdev.entity_tracker.ui;

import javax.swing.table.DefaultTableModel;

public class EntityObserverTableModel extends DefaultTableModel {
	public EntityObserverTableModel(String observerTypeName) {
		addColumn("");
		addColumn(observerTypeName);
		addColumn("actives");
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

	public void addObserver(String name) {
		addRow(new Object[] { true, name, 0 });
	}
}
