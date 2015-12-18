package net.namekdev.entity_tracker.ui.model;

public class ManagerTableModel extends BaseSystemTableModel {
	public ManagerTableModel() {
		super();
		columnIdentifiers.set(1, "manager");
	}

	public void addManager(String name) {
		addRow(new Object[] { true, name });
	}

	public String getManagerName(int index) {
		return (String) getValueAt(index, 1);
	}

	public boolean getManagerState(int index) {
		return (boolean) getValueAt(index, 0);
	}

	public void clear() {
		setRowCount(0);
	}
}