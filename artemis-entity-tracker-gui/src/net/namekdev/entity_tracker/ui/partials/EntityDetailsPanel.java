package net.namekdev.entity_tracker.ui.partials;

import java.awt.Color;
import java.awt.Component;
import java.util.BitSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import net.namekdev.entity_tracker.ui.model.EntityTableModel;

public class EntityDetailsPanel extends JPanel {
	private EntityTableModel _entityTableModel;

	private TitledBorder _titledBorder;
	private JList<String> _componentList;
	private DefaultListModel<String> _componentListModel;


	public EntityDetailsPanel(EntityTableModel entityTableModel) {
		_entityTableModel = entityTableModel;

		initialize();
	}

	protected void initialize() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		_titledBorder = new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Entity 32", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0));
		setBorder(_titledBorder);

		_componentListModel = new DefaultListModel<String>();
		_componentList = new JList<String>(_componentListModel);
		_componentList.setAlignmentX(Component.LEFT_ALIGNMENT);
		_componentList.setLayoutOrientation(JList.VERTICAL_WRAP);
		_componentList.setBorder(new TitledBorder("Components:"));
		add(_componentList);

		setBackground(Color.WHITE);
	}

	public void setup(int entityId) {
		BitSet entityComponents = _entityTableModel.getEntityComponents(entityId);

		_titledBorder.setTitle("Entity #" + entityId);

		_componentListModel.clear();
		for (int i = entityComponents.nextSetBit(0); i >= 0; i = entityComponents.nextSetBit(i+1)) {
			_componentListModel.addElement(_entityTableModel.getComponentName(i));
		}

		revalidate();
		repaint();
	}
}
