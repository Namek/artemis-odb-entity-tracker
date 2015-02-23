package net.namekdev.entity_tracker.ui.model;

import javax.swing.table.DefaultTableModel;

public class SystemTableModel extends DefaultTableModel {
	public SystemTableModel() {
		addColumn("");
		addColumn("system");
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

	public void addSystem(String name) {
		addRow(new Object[] { true, name, 0 });
	}
}
