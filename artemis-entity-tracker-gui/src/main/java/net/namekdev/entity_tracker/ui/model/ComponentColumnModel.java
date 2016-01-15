package net.namekdev.entity_tracker.ui.model;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

public class ComponentColumnModel extends DefaultTableColumnModel {
	public static final int ORDER_ASC = 1;
	public static final int ORDER_DESC = 2;
	public static final int ORDER_MODEL = 3;

	protected int currentOrder = ORDER_ASC;


	public ComponentColumnModel() {
	}

	/**
	 * Sets order type and sorts entities due to this order type.
	 *
	 * @param orderType {@link #ORDER_ASC}, {@link #ORDER_DESC} or {@link #ORDER_MODEL}.
	 */
	public void setCurrentOrder(int orderType) {
		this.currentOrder = orderType;
		sortColumns();
	}

	/**
	 * Get current order type.
	 *
	 * @see #setCurrentOrder(int)
	 */
	public int getCurrentOrder() {
		return this.currentOrder;
	}


	/**
	 * Adds column as always and runs column sorting for it.
	 */
	@Override
	public void addColumn(TableColumn aColumn) {
		super.addColumn(aColumn);
		sortColumns();
	}


	private void sortColumns() {
		final int n = getColumnCount();

		// start at i=1, because i=0 is "entity id" column, which should not be moved
		for (int i = 1; i < n; ++i) {
			int mostLeftColIndex = i;
			TableColumn col1 = getColumn(i);
			String mostLeftColName = col1.getHeaderValue().toString();

			for (int j = i+1; j < n; ++j) {
				TableColumn col2 = getColumn(j);
				String col2Name = col2.getHeaderValue().toString();

				boolean performSwitch = false;

				if (currentOrder == ORDER_MODEL) {
					performSwitch = col2.getModelIndex() < col1.getModelIndex();
				}
				else {
					int cmp = mostLeftColName.compareTo(col2Name);
					performSwitch = currentOrder == ORDER_DESC && cmp < 0 || currentOrder == ORDER_ASC && cmp > 0;
				}

				if (performSwitch) {
					mostLeftColIndex = j;
					mostLeftColName = col2Name;
				}
			}

			if (mostLeftColIndex != i) {
				switchColumns(mostLeftColIndex, i);
			}
		}
	}

	protected void switchColumns(int index1, int index2) {
		TableColumn col1 = tableColumns.elementAt(index1);
		TableColumn col2 = tableColumns.elementAt(index2);

		tableColumns.set(index1, col2);
		tableColumns.set(index2, col1);

//		fireColumnMoved(new TableColumnModelEvent(this, columnIndex,
//                newIndex));
	}
}
