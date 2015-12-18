package net.namekdev.entity_tracker.ui.model;

import javax.swing.table.DefaultTableModel;

public class BaseSystemTableModel extends DefaultTableModel {
	public BaseSystemTableModel() {
		addColumn("");
		addColumn("system");
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

	public void setSystem(int index, String name) {
		for (int i = getRowCount(); i <= index; ++i) {
			addRow(new Object[] { true, "" });
		}

		setValueAt(name, index, 1);
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
