package net.namekdev.entity_tracker.ui.utils;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class SelectionListener implements ListSelectionListener {
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			// we're not interested in unselect event for previous row
			return;
		}

		int row = e.getLastIndex();

		Object source = e.getSource();
		if (source instanceof DefaultListSelectionModel) {
			final DefaultListSelectionModel selection = (DefaultListSelectionModel) e.getSource();
			row = selection.getAnchorSelectionIndex();
		}
		else if (source instanceof JList) {
			JList<?> list = (JList<?>) source;
			row = list.getSelectedIndex();
		}

		rowSelected(row);
	}

	public abstract void rowSelected(int index);
}
