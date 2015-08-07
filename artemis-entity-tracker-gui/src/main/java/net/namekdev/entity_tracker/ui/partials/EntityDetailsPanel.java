package net.namekdev.entity_tracker.ui.partials;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.BitSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;

import net.namekdev.entity_tracker.connectors.WorldController;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.ui.Context;
import net.namekdev.entity_tracker.ui.model.EntityTableModel;
import net.namekdev.entity_tracker.ui.utils.SelectionListener;
import net.namekdev.entity_tracker.utils.Array;
import net.namekdev.entity_tracker.utils.IndexBiMap;

public class EntityDetailsPanel extends JPanel {
	private Context _appContext;

	private EntityTableModel _entityTableModel;
	private int _currentEntityId = -1;
	private int _currentComponentIndex = -1;
	private final IndexBiMap _componentIndices = new IndexBiMap();

	private JSplitPane _splitPane;
	private JPanel _entityPanel, _componentsPanelContainer;

	private TitledBorder _entityTitledBorder, _componentTitledBorder;
	private JList<String> _componentList;
	private DefaultListModel<String> _componentListModel;


	public EntityDetailsPanel(Context appContext, EntityTableModel entityTableModel) {
		_appContext = appContext;
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
		_componentsPanelContainer = new JPanel();
		_componentsPanelContainer.setBorder(_componentTitledBorder);
		_componentsPanelContainer.setLayout(new BorderLayout());
		_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _entityPanel, _componentsPanelContainer);
		_splitPane.setOpaque(false);
	}

	private void setup(int entityId) {
		setup(entityId, -1);
	}

	private void setup(int entityId, int componentTypeIndex) {
		if (componentTypeIndex >= 0) {
			// show component details
			removeAll();
			add(_splitPane);
			setBorder(null);
			_entityPanel.setBorder(_entityTitledBorder);
			_entityPanel.add(_componentList);
		}
		else if (componentTypeIndex < 0 && _currentComponentIndex >= 0) {
			// show only entity info
			removeAll();
			add(_componentList);
			setBorder(_entityTitledBorder);
		}

		if (entityId != _currentEntityId) {
			BitSet entityComponents = _entityTableModel.getEntityComponents(entityId);

			_entityTitledBorder.setTitle("Entity #" + entityId);

			_componentIndices.ensureSize(_entityTableModel.getColumnCount());
			_componentListModel.clear();
			for (int i = entityComponents.nextSetBit(0), j = 0; i >= 0; i = entityComponents.nextSetBit(i+1), ++j) {
				ComponentTypeInfo info = _entityTableModel.getComponentTypeInfo(i);

				_componentListModel.addElement(info.name);
				_componentIndices.set(j, i);
			}

			_currentEntityId = entityId;
		}

		if (componentTypeIndex >= 0) {
			ComponentTypeInfo info = _entityTableModel.getComponentTypeInfo(componentTypeIndex);

			_componentTitledBorder.setTitle(info.name);
			_componentsPanelContainer.removeAll();
			_componentsPanelContainer.add(new ComponentDataPanel(_appContext, info, entityId), BorderLayout.PAGE_START);

			_appContext.worldController.requestComponentState(_currentEntityId, componentTypeIndex);
		}
		_currentComponentIndex = componentTypeIndex;

		revalidate();
		repaint(50);

	}

	public int getEntityId() {
		return _currentEntityId;
	}

	public void selectComponent(int entityId, int componentIndex) {
		setup(entityId, componentIndex);
		int rowIndex = _componentIndices.getLocalIndex(componentIndex);
		_componentList.setSelectedIndex(rowIndex);
	}

	private ListSelectionListener _componentSelectionListener = new SelectionListener() {
		@Override
		public void rowSelected(int rowIndex) {
			if (rowIndex >= 0) {
				int componentIndex = _componentIndices.getGlobalIndex(rowIndex);

				setup(_currentEntityId, componentIndex);
			}
		}
	};
}
