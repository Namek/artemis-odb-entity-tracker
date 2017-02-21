package net.namekdev.entity_tracker.ui.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import com.artemis.utils.BitVector;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.utils.Array;

public class EntityTableModel extends DefaultTableModel {
	private Map<Integer, BitVector> _entityComponents = new HashMap<>();
	private Array<ComponentTypeInfo> _componentTypes = new Array<ComponentTypeInfo>(50);


	public EntityTableModel() {
		super(new Object[][] {}, new String[] { "  entity id  " });
	}

	public void setComponentType(int index, ComponentTypeInfo info) {
		_componentTypes.set(index, info);

		for (int i = getColumnCount(); i <= index+1; ++i) {
			addColumn("");
		}

		columnIdentifiers.set(index+1, info.getName());
		fireTableStructureChanged();
	}

	public void addEntity(int entityId, BitVector components) {
		// TODO check if bitset isn't greater than before model header columns

		Vector<Object> row = new Vector<Object>(components.length() + 1);
		row.add(entityId);

		for (int i = 0, n = components.length(); i < n; ++i) {
			row.add(components.get(i));
		}

		this.addRow(row);
		_entityComponents.put(entityId, components);
	}

	public void removeEntity(int entityId) {
		for (int i = 0, n = getRowCount(); i < n; ++i) {
			Integer val = (Integer) getValueAt(i, 0);

			if (val == entityId) {
				removeRow(i);
				break;
			}
		}
		_entityComponents.remove(entityId);
	}

	public BitVector getEntityComponents(int entityId) {
		return _entityComponents.get(entityId);
	}

	public ComponentTypeInfo getComponentTypeInfo(int index) {
		return _componentTypes.get(index);
	}

	public Class<?> getColumnClass(int columnIndex) {
		return columnIndex == 0 ? Integer.class : Boolean.class;
	}

	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public void clear() {
		_componentTypes.clear();
		_entityComponents.clear();
		setRowCount(0);

		columnIdentifiers.setSize(1);
		fireTableStructureChanged();
	}
}
