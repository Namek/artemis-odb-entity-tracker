package net.namekdev.entity_tracker.ui.partials;

import java.awt.Color;
import java.awt.Component;
import java.util.BitSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;

import net.namekdev.entity_tracker.ui.model.EntityTableModel;
import net.namekdev.entity_tracker.ui.utils.SelectionListener;

public class EntityDetailsPanel extends JPanel {
	private EntityTableModel _entityTableModel;
	private int _currentEntityId = -1;
	private int _currentComponentIndex = -1;

	private JSplitPane _splitPane;
	private JPanel _entityPanel, _componentsPanel;

	private TitledBorder _entityTitledBorder, _componentTitledBorder;
	private JList<String> _componentList;
	private DefaultListModel<String> _componentListModel;


	public EntityDetailsPanel(EntityTableModel entityTableModel) {
		_entityTableModel = entityTableModel;

		initialize();
	}

	protected void initialize() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		_entityTitledBorder = new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Entity 32", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0));
		_componentTitledBorder = new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Renderable", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0));

		_componentListModel = new DefaultListModel<String>();
		_componentList = new JList<String>(_componentListModel);
		_componentList.setAlignmentX(Component.LEFT_ALIGNMENT);
		_componentList.setLayoutOrientation(JList.VERTICAL);
		_componentList.setBorder(new TitledBorder("Components:"));
		add(_componentList);

		_componentList.addListSelectionListener(_componentSelectionListener);

		// Things used to show component details
		_entityPanel = new JPanel();
		_entityPanel.setLayout(new BoxLayout(_entityPanel, BoxLayout.Y_AXIS));
		_entityPanel.setBorder(_entityTitledBorder);
		_componentsPanel = new JPanel();
		_componentsPanel.setBorder(_componentTitledBorder);
		_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _entityPanel, _componentsPanel);
		_splitPane.setOpaque(false);
	}

	public void setup(int entityId) {
		setup(entityId, -1);
	}

	public void setup(int entityId, int componentIndex) {
		if (componentIndex >= 0 && _currentComponentIndex < 0) {
			// show component details
			removeAll();
			add(_splitPane);
			setBorder(null);
			_entityPanel.setBorder(_entityTitledBorder);
			_entityPanel.add(_componentList);
		}
		else if (componentIndex < 0 && _currentComponentIndex >= 0) {
			// show only entity info
			removeAll();
			add(_componentList);
			setBorder(_entityTitledBorder);
		}

		if (entityId != _currentEntityId) {
			BitSet entityComponents = _entityTableModel.getEntityComponents(entityId);

			_entityTitledBorder.setTitle("Entity #" + entityId);

			_componentListModel.clear();
			for (int i = entityComponents.nextSetBit(0); i >= 0; i = entityComponents.nextSetBit(i+1)) {
				String componentName = _entityTableModel.getComponentName(i);

				_componentListModel.addElement(componentName);
			}

			_currentEntityId = entityId;
		}

		if (componentIndex >= 0) {
			String componentName = _entityTableModel.getComponentName(componentIndex);

			_componentTitledBorder.setTitle(componentName);
			_componentsPanel.removeAll();
			_componentsPanel.add(new JLabel("TODO component details"));
		}
		_currentComponentIndex = componentIndex;

		revalidate();
		repaint();
	}

	private ListSelectionListener _componentSelectionListener = new SelectionListener() {
		@Override
		public void rowSelected(int index) {
			if (index >= 0) {
				setup(_currentEntityId, index);
			}
		}
	};
}
