package org.jdesktop.swingx;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;

import net.namekdev.entity_tracker.utils.ReflectionUtils;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableCellEditor;
import org.jdesktop.swingx.treetable.TreeTableModel;

import com.artemis.utils.reflect.ClassReflection;
import com.artemis.utils.reflect.Method;

/**
 * {@link JXTreeTable} implementation extended by by-value-type cell edition.
 *
 * @author Namek
 */
public class DynamicJXTreeTable extends JXTreeTable {
	protected TableCellRenderer renderer;
	protected TreeTableCellEditor hierarchicalEditor;
	protected ICellClassGetter cellClassGetter;


	public static interface ICellClassGetter {
		Class<?> getCellClass(Object node, int column);
	}

	public DynamicJXTreeTable(TreeTableModel treeModel, ICellClassGetter cellRendererGetter) {
		// most of this constructor is trying to mimic super(TreeTableCellRenderer renderer)

		super();

		try {
			TreeTableCellRenderer_public renderer = new TreeTableCellRenderer_public(treeModel);
			TreeTableModelAdapterExt adapter = new TreeTableModelAdapterExt(renderer);

			setModel(adapter);

			// call: init(renderer)
			Class<?> TreeTableCellRendererClass = TreeTableCellRenderer_public.class.getSuperclass();
			Method init = ClassReflection.getDeclaredMethod(JXTreeTable.class, "init", TreeTableCellRendererClass);
			init.setAccessible(true);
			init.invoke(this, renderer);

			// call: initActions()
			Method initActions = ClassReflection.getDeclaredMethod(JXTreeTable.class, "initActions");
			initActions.setAccessible(true);
			initActions.invoke(this);

			// disable sorting
			super.setSortable(false);
			super.setAutoCreateRowSorter(false);
			super.setRowSorter(null);
			// no grid
			setShowGrid(false, false);

			hierarchicalEditor = new TreeTableCellEditor(renderer);
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}

        // now our custom initialization
		init(cellRendererGetter);
	}

	private void init(ICellClassGetter cellClassGetter) {
		this.cellClassGetter = cellClassGetter;

		Object renderer = ReflectionUtils.getHiddenFieldValue(JXTreeTable.class, "renderer", this);
		this.renderer = (TableCellRenderer) renderer;

		Object editor = ReflectionUtils.getHiddenFieldValue(JXTreeTable.class, "hierarchicalEditor", this);
		this.hierarchicalEditor = (TreeTableCellEditor) editor;
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		if (isHierarchical(column)) {
            return this.renderer;
        }

		TableCellRenderer renderer = null;

		TreePath path = getPathForRow(row);

		if (path != null) {
			Object node = path.getLastPathComponent();
			Class<?> type = cellClassGetter.getCellClass(node, column);

			if (type != null) {
				renderer = getDefaultRenderer(type);
			}
		}

        if (renderer == null) {
        	renderer = super.getCellRenderer(row, column);
        }

        return renderer;
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (isHierarchical(column)) {
			return hierarchicalEditor;
		}

		TableCellEditor editor = null;

		TreePath path = getPathForRow(row);

		if (path != null) {
			Object node = path.getLastPathComponent();
			Class<?> type = cellClassGetter.getCellClass(node, column);


			if (type != null) {
				editor = getDefaultEditor(type);
			}
		}

		if (editor == null) {
			editor = super.getCellEditor(row, column);
		}

		return editor;
	}
}
