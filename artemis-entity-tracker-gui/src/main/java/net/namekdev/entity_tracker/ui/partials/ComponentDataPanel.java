package net.namekdev.entity_tracker.ui.partials;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;

import net.miginfocom.swing.MigLayout;
import net.namekdev.entity_tracker.connectors.DummyWorldUpdateListener;
import net.namekdev.entity_tracker.connectors.WorldUpdateListener;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.model.FieldInfo;
import net.namekdev.entity_tracker.ui.Context;
import net.namekdev.entity_tracker.ui.model.ComponentTreeTableModel;


public class ComponentDataPanel extends JPanel {
	private Context _appContext;
	private ComponentTypeInfo _info;
	private int _entityId;
	private Vector<JComponent> _components;

	private ComponentTreeTableModel treeTableModel;
	private JXTreeTable treeTable;


	public ComponentDataPanel(Context appContext, ComponentTypeInfo info, int entityId) {
		_appContext = appContext;
		_info = info;
		_entityId = entityId;

		initialize();
	}

	protected void initialize() {
		treeTableModel = new ComponentTreeTableModel();
		treeTable = new JXTreeTable(treeTableModel);
		treeTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode();
		DefaultMutableTreeTableNode propNumber = new DefaultMutableTreeTableNode(10);
		root.add(propNumber);
		root.add(new DefaultMutableTreeTableNode(10));
		root.add(new DefaultMutableTreeTableNode(10));
		propNumber.setParent(root);

		/*DefaultMutableTreeNode incomeNode = new DefaultMutableTreeNode(new TableRowData("Income","25000","5000","300000",true));
    	incomeNode.add(new DefaultMutableTreeNode(new TableRowData("Salary1","250001","50001","3000001",false)));
    	incomeNode.add(new DefaultMutableTreeNode(new TableRowData("Salary2","250002","50002","3000002",false)));
    	incomeNode.add(new DefaultMutableTreeNode(new TableRowData("Salary3","250003","50003","3000003",false)));
    	incomeNode.add(new DefaultMutableTreeNode(new TableRowData("Salary4","250004","50004","3000004",false)));
    	incomeNode.add(new DefaultMutableTreeNode(new TableRowData("Salary5","250005","50005","3000005",false)));

    	root.add(incomeNode);*/


		treeTableModel.setRoot(root);
		add(treeTable);

		/*
		MigLayout layout = new MigLayout("wrap", "[right][0:pref,grow]", "");
		setLayout(layout);

		int size = _info.fields.size();
		_components = new Vector<JComponent>(size);

		for (int i = 0; i < size; ++i) {
			FieldInfo field = _info.fields.get(i);

			JLabel label = new JLabel(field.fieldName);
			JComponent value = null;

			if (field.valueType == TYPE_BOOLEAN) {
				JCheckBox checkbox = new JCheckBox();
				value = checkbox;
				setupCheckBoxListener(checkbox, i);
			}
			else if (field.valueType == TYPE_UNKNOWN) {
				value = new JLabel("<reference>");
			}
			else {
				final JTextField textField = new JTextField();
				value = textField;
				setupTextFieldListener(textField, i);
			}

			_components.add(value);

			add(label);
			add(value, "width min:50, grow");
		}

		// register listener for component data
		_appContext.eventBus.registerListener(worldListener);*/
	}

	private void setupCheckBoxListener(final JCheckBox checkbox, final int fieldIndex) {
		checkbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean value = checkbox.isSelected();
				_appContext.worldController.setComponentFieldValue(_entityId, _info.index, fieldIndex, value);
			}
		});
	}

	private void setupTextFieldListener(final JTextField textField, final int fieldIndex) {
		textField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// on enter
				if (e.getKeyCode() == 10) {
					String text = textField.getText();

					// TODO convert string to appropriate field's type
					_appContext.worldController.setComponentFieldValue(_entityId, _info.index, fieldIndex, text);
				}
			}
		});
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		_appContext.eventBus.unregisterListener(worldListener);
	}


	private final WorldUpdateListener worldListener = new DummyWorldUpdateListener() {
		@Override
		public void updatedComponentState(int entityId, int componentIndex, Object[] values) {
			if (_info.index != componentIndex || _entityId != entityId) {
				return;
			}

			for (int i = 0, n = values.length; i < n; ++i) {
				Object value = values[i];

				if (value == null) {
					continue;
				}

				Component component = _components.get(i);
				FieldInfo info = _info.fields.get(i);

				if (info.valueType == TYPE_BOOLEAN) {
					((JCheckBox) component).setSelected((Boolean) values[i]);
				}
				else if (info.valueType != TYPE_UNKNOWN) {
					((JTextField) component).setText(values[i].toString());
				}
			}
		}
	};
}
