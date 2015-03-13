package net.namekdev.entity_tracker.ui.partials;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.model.FieldInfo;


public class ComponentDataPanel extends JPanel {
	private ComponentTypeInfo _info;


	public ComponentDataPanel(ComponentTypeInfo info) {
		_info = info;
		initialize();
	}

	protected void initialize() {
		GridLayout layout = new GridLayout(0, 2);
		setLayout(layout);

		for (int i = 0, n = _info.fields.size(); i < n; ++i) {
			FieldInfo field = _info.fields.get(i);

			JLabel label = new JLabel(field.fieldName);
			JComponent value = new JTextField("");

			add(label);
			add(value);

		}

		revalidate();
		repaint();
	}
}
