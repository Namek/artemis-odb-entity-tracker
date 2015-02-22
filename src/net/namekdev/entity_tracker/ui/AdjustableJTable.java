package net.namekdev.entity_tracker.ui;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;

import net.namekdev.entity_tracker.ui.utils.TableColumnAdjuster;

public class AdjustableJTable extends JTable {
	protected TableColumnAdjuster adjuster;

	public AdjustableJTable() {
		adjuster = new TableColumnAdjuster(this);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		super.tableChanged(e);

		if (adjuster != null) {
			adjuster.adjustColumns();
		}
	}
}
