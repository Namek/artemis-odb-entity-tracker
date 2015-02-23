package net.namekdev.entity_tracker.ui.model;

import javax.swing.table.DefaultTableModel;

public class ManagerTableModel extends DefaultTableModel {
	public ManagerTableModel() {
		addColumn("");
		addColumn("manager");
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case 0: return Boolean.class;
			case 1: return String.class;
			default: return null;
		}
	}

	public boolean isCellEditable(int row, int column) {
		return column == 0;
	}

	public void addManager(String name) {
		addRow(new Object[] { true, name });
	}
}