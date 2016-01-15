package net.namekdev.entity_tracker.ui.partials;

import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import net.namekdev.entity_tracker.ui.model.ComponentColumnModel;
import net.namekdev.entity_tracker.ui.model.EntityTableModel;
import net.namekdev.entity_tracker.ui.utils.VerticalTableHeaderCellRenderer;

public class EntityTable extends JTable {
	private EntityTableModel tableModel;
	private ComponentColumnModel columnModel;
	private	JButton btnComponentOrderingToggle;


	public EntityTable(EntityTableModel tableModel) {
		this.tableModel = tableModel;

		setAutoCreateRowSorter(true);
		setShowVerticalLines(false);
		setFillsViewportHeight(true);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JTableHeader tableHeader = getTableHeader();
		tableHeader.setDefaultRenderer(new VerticalTableHeaderCellRenderer());

		setColumnModel(columnModel = new ComponentColumnModel());
		setModel(tableModel);

		// set max width for first column "entity id"
		TableColumn entityIdColumn = getColumnModel().getColumn(0);
		entityIdColumn.setMaxWidth(10);

		// add button that can toggle ordering of component columns
		btnComponentOrderingToggle = new SortToggleButton();

		LayoutManager l = new BoxLayout(tableHeader, BoxLayout.Y_AXIS);
		tableHeader.setLayout(l);
		tableHeader.add(btnComponentOrderingToggle);
	}


	private class SortToggleButton extends JButton {
		static final String arrowsUnicode = "\u21C6";

		public SortToggleButton() {
			setBorder(null);

			setBorder(new EmptyBorder(3, 5, 3, 5));
			setFocusable(true);
			setFocusPainted(false);

			refreshText();
			addActionListener(sortingToggleClickListener);
		}

		private void refreshText() {
			setText(arrowsUnicode + " " + columnModel.getCurrentOrderName());
		}

		private ActionListener sortingToggleClickListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				columnModel.toggleOrdering();
				refreshText();
			}
		};
	}
}
