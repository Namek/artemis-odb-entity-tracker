package net.namekdev.entity_tracker.ui.model;

import java.util.ArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.namekdev.entity_tracker.ui.listener.ChangingSystemEnabledStateListener;
import net.namekdev.entity_tracker.ui.utils.ExtendedTableModel;

public class BaseSystemTableModel extends ExtendedTableModel {
	private final ArrayList<ChangingSystemEnabledStateListener> _listeners = new ArrayList<>();


	public BaseSystemTableModel() {
		addColumn("");
		addColumn("system");

		addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getColumn() != 0) {
					return;
				}
				
				int rowIndex = e.getFirstRow();
				int systemIndex = getSystemIndex(rowIndex);
				String systemName = getSystemName(systemIndex);
				boolean enabled = getSystemState(systemIndex);

				for (ChangingSystemEnabledStateListener l : _listeners) {
					l.onChangingSystemEnabledState(BaseSystemTableModel.this, systemIndex, systemName, enabled);
				}
			}
		});
	}
	
	public void addChangingSystemEnabledStateListener(ChangingSystemEnabledStateListener listener) {
		_listeners.add(listener);
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
	
	public int getSystemIndex(int rowIndex) {
		return rowIndex;
	}

	public void setSystem(int systemIndex, String name) {
		for (int i = getRowCount(); i <= systemIndex; ++i) {
			addRow(new Object[] { true, "" });
		}

		setValueAt(name, systemIndex, 1);
	}
	
	/**
	 * Update system state without firing events.
	 */
	public void updateSystemState(int systemIndex, boolean enabled) {
		updateValueAt(enabled, systemIndex, 0);
	}

	public String getSystemName(int systemIndex) {
		return (String) getValueAt(systemIndex, 1);
	}

	public boolean getSystemState(int systemIndex) {
		return (boolean) getValueAt(systemIndex, 0);
	}

	public void clear() {
		setRowCount(0);
	}
}
