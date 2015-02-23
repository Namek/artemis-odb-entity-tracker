package net.namekdev.entity_tracker.ui.model;

import java.util.BitSet;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

public class EntityTableModel extends DefaultTableModel {
	private int componentTypesCount = 0;


	public EntityTableModel() {
		super(new Object[][] {}, new String[] { "  entity id  " });
	}

	public void addComponentType(String name) {
		componentTypesCount++;

		// FIXME hack: spaces are to add some padding
		addColumn("  " + name + "  ");
	}

	public void addEntity(int entityId, BitSet components) {
		// TODO check if bitset isn't greater than before model header columns

		Vector<Object> row = new Vector<Object>(components.length() + 1);
		row.add(entityId);

		for (int i = 0, n = components.size(); i < n; ++i) {
			row.add(components.get(i));
		}

		this.addRow(row);
	}

	public void removeEntity(int entityId) {
		for (int i = 0, n = getRowCount(); i < n; ++i) {
			Integer val = (Integer) getValueAt(i, 0);

			if (val == entityId) {
				removeRow(i);
				break;
			}
		}
	}

	public Class<?> getColumnClass(int columnIndex) {
		return columnIndex == 0 ? Integer.class : Boolean.class;
	}

	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
