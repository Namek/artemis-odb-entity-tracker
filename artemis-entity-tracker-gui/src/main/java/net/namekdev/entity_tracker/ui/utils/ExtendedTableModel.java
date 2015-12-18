package net.namekdev.entity_tracker.ui.utils;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

public class ExtendedTableModel extends DefaultTableModel {
	/**
	 * Mimics {@link #setValueAt(Object, int, int)}
	 * but doesn't notify listeners.
	 */
	public void updateValueAt(Object aValue, int row, int column) {
        Vector rowVector = (Vector)dataVector.elementAt(row);
        rowVector.setElementAt(aValue, column);
    }
}
