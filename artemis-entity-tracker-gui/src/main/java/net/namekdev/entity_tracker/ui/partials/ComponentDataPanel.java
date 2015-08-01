package net.namekdev.entity_tracker.ui.partials;

import static net.namekdev.entity_tracker.utils.serialization.NetworkSerialization.*;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.namekdev.entity_tracker.connectors.DummyWorldUpdateListener;
import net.namekdev.entity_tracker.connectors.WorldUpdateListener;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.model.FieldInfo;
import net.namekdev.entity_tracker.ui.Context;


public class ComponentDataPanel extends JPanel {
	private Context _appContext;
	private ComponentTypeInfo _info;
	private int _entityId;
	private Vector<JComponent> _components;


	public ComponentDataPanel(Context appContext, ComponentTypeInfo info, int entityId) {
		_appContext = appContext;
		_info = info;
		_entityId = entityId;

		initialize();
	}

	protected void initialize() {
		GridLayout layout = new GridLayout(0, 2);
		setLayout(layout);

		int size = _info.fields.size();
		_components = new Vector<JComponent>(size);

		for (int i = 0; i < size; ++i) {
			FieldInfo field = _info.fields.get(i);

			JLabel label = new JLabel(field.fieldName);
			JComponent value = null;

			if (field.valueType == TYPE_BOOLEAN) {
				value = new JCheckBox();
			}
			else if (field.valueType == TYPE_UNKNOWN) {
				value = new JLabel("<reference>");
			}
			else {
				value = new JTextField();
			}

			_components.add(value);

			add(label);
			add(value);
		}

		// register listener for component data
		_appContext.eventBus.registerListener(worldListener);

		revalidate();
		repaint();
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
