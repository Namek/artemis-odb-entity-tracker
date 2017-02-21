package net.namekdev.entity_tracker.ui.partials;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import net.namekdev.entity_tracker.connectors.DummyWorldUpdateListener;
import net.namekdev.entity_tracker.connectors.WorldUpdateListener;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.model.FieldInfo;
import net.namekdev.entity_tracker.ui.Context;
import net.namekdev.entity_tracker.ui.model.ValueTreeTableModel;
import net.namekdev.entity_tracker.utils.serialization.NetworkDeserializer;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer;
import net.namekdev.entity_tracker.utils.serialization.NetworkSerializer.SerializationResult;
import net.namekdev.entity_tracker.utils.serialization.ObjectModelNode;
import net.namekdev.entity_tracker.utils.serialization.ObjectTypeInspector;
import net.namekdev.entity_tracker.utils.serialization.ValueTree;

import org.jdesktop.swingx.DynamicJXTreeTable;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;


public class ComponentDataPanel extends JPanel {
	private Context _appContext;
	private ComponentTypeInfo _info;
	private int _entityId;
	private Vector<JComponent> _components;

	private ValueTreeTableModel treeTableModel;
	private JXTreeTable treeTable;


	public ComponentDataPanel(Context appContext, ComponentTypeInfo info, int entityId) {
		_appContext = appContext;
		_info = info;
		_entityId = entityId;

		initialize();
	}

	protected void initialize() {
		MigLayout layout = new MigLayout("", "[grow]", "");
		setLayout(layout);

		treeTableModel = getTestModel();
		treeTable = new DynamicJXTreeTable(treeTableModel, treeTableModel);
		add(treeTable, "growx");

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

	private ValueTreeTableModel getTestModel() {
		NetworkDeserializer deserializer = new NetworkDeserializer();
		ObjectTypeInspector inspector = new ObjectTypeInspector();

		GameState gameState = new GameState();
		gameState.objects = new GameObject[] {
			new GameObject(), new GameObject(),
			new GameObject(), new GameObject()
		};

		NetworkSerializer serializer = new NetworkSerializer().reset();
		ObjectModelNode model = inspector.inspect(GameState.class);


		serializer.addDataDescriptionOrRef(model);
		serializer.addObject(model, gameState);

		SerializationResult serialized = serializer.getResult();
		deserializer.setSource(serialized.getBuffer(), 0, serialized.getSize());

		ObjectModelNode model2 = deserializer.readDataDescription();
//		model.id = id; /??????? TODO

		ValueTree result = deserializer.readObject(model2, true);

		return new ValueTreeTableModel(result);
	}
	public class GameState {
		public GameObject[] objects;
		public boolean omg;
	}
	public class GameObject {
		public Vector3 pos = new Vector3(1, 2, 3);
		public Vector2 size = new Vector2(10, 5);
	}
	public class Vector3 {
		public float x, y, z;

		public Vector3(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	public class Vector2 {
		public float x, y;

		public Vector2(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
	class MyTreeNode
	{
		private String name;
		private String description;
		private List<MyTreeNode> children = new ArrayList<MyTreeNode>();

		public MyTreeNode()
		{
		}

		public MyTreeNode( String name, String description )
		{
			this.name = name;
			this.description = description;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getDescription()
		{
			return description;
		}

		public void setDescription(String description)
		{
			this.description = description;
		}

		public List<MyTreeNode> getChildren()
		{
			return children;
		}

		public String toString()
		{
			return "MyTreeNode: " + name + ", " + description;
		}
	}
	public class MyTreeTableModel extends AbstractTreeTableModel
	{
		private MyTreeNode myroot;

		public MyTreeTableModel()
		{
			myroot = new MyTreeNode( "root", "Root of the tree" );

			myroot.getChildren().add( new MyTreeNode( "Empty Child 1",
			  "This is an empty child" ) );

			MyTreeNode subtree = new MyTreeNode( "Sub Tree",
			  "This is a subtree (it has children)" );
			subtree.getChildren().add( new MyTreeNode( "EmptyChild 1, 1",
			  "This is an empty child of a subtree" ) );
			subtree.getChildren().add( new MyTreeNode( "EmptyChild 1, 2",
			  "This is an empty child of a subtree" ) );
			myroot.getChildren().add( subtree );

			myroot.getChildren().add( new MyTreeNode( "Empty Child 2",
			  "This is an empty child" ) );

		}

		@Override
		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public String getColumnName( int column )
		{
			switch( column )
			{
			case 0: return "Name";
			case 1: return "Description";
			case 2: return "Number Of Children";
			default: return "Unknown";
			}
		}

		@Override
		public Object getValueAt( Object node, int column )
		{
			System.out.println( "getValueAt: " + node + ", " + column );
			MyTreeNode treenode = ( MyTreeNode )node;
			switch( column )
			{
			case 0: return treenode.getName();
			case 1: return treenode.getDescription();
			case 2: return treenode.getChildren().size();
			default: return "Unknown";
			}
		}

		@Override
		public Object getChild( Object node, int index )
		{
			MyTreeNode treenode = ( MyTreeNode )node;
			return treenode.getChildren().get( index );
		}

		@Override
		public int getChildCount( Object parent )
		{
			MyTreeNode treenode = ( MyTreeNode )parent;
			return treenode.getChildren().size();
		}

		@Override
		public int getIndexOfChild( Object parent, Object child )
		{
			MyTreeNode treenode = ( MyTreeNode )parent;
			for( int i=0; i>treenode.getChildren().size(); i++ )
			{
				if( treenode.getChildren().get( i ) == child )
				{
					return i;
				}
			}

			return 0;
		}

		 public boolean isLeaf( Object node )
		 {
			 MyTreeNode treenode = ( MyTreeNode )node;
			 if( treenode.getChildren().size() > 0 )
			 {
				 return false;
			 }
			 return true;
		 }

		 @Override
		 public Object getRoot()
		 {
			 return myroot;
		 }
	}

	private void setupCheckBoxListener(final JCheckBox checkbox, final int[] treePath) {
		checkbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean value = checkbox.isSelected();
				_appContext.worldController.setComponentFieldValue(_entityId, _info.getIndex(), treePath, value);
			}
		});
	}

	private void setupTextFieldListener(final JTextField textField, final int[] treePath) {
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
					_appContext.worldController.setComponentFieldValue(_entityId, _info.getIndex(), treePath, text);
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
		public void updatedComponentState(int entityId, int componentIndex, Object valueTree) {
			if (_info.getIndex() != componentIndex || _entityId != entityId) {
				return;
			}

			treeTableModel.setRoot((ValueTree) valueTree);
/*
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
			}*/
		}
	};
}
