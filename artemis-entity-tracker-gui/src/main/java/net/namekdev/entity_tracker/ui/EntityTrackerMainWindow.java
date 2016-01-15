package net.namekdev.entity_tracker.ui;

import java.awt.CardLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.BitSet;
import java.util.Enumeration;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.namekdev.entity_tracker.connectors.WorldController;
import net.namekdev.entity_tracker.connectors.WorldUpdateInterfaceListener;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.ui.listener.ChangingSystemEnabledStateListener;
import net.namekdev.entity_tracker.ui.model.BaseSystemTableModel;
import net.namekdev.entity_tracker.ui.model.EntitySystemTableModel;
import net.namekdev.entity_tracker.ui.model.EntityTableModel;
import net.namekdev.entity_tracker.ui.model.ManagerTableModel;
import net.namekdev.entity_tracker.ui.partials.EntityDetailsPanel;
import net.namekdev.entity_tracker.ui.partials.EntityTable;
import net.namekdev.entity_tracker.ui.utils.AdjustableJTable;
import net.namekdev.entity_tracker.ui.utils.VerticalTableHeaderCellRenderer;

public class EntityTrackerMainWindow implements WorldUpdateInterfaceListener {
	protected final Context context = new Context();
	protected JFrame frame;
	private JTable entitiesTable;
	private JScrollPane tableScrollPane, filtersScrollPane, detailsPanelContainer;
	private EntityTableModel entitiesTableModel;
	private EntitySystemTableModel entitySystemsTableModel;
	private BaseSystemTableModel baseSystemsTableModel;
	private ManagerTableModel managersTableModel;
	private JSplitPane mainSplitPane, tableFiltersSplitPane, systemsDetailsSplitPane;
	private JPanel filtersPanel, systemsManagersPanel;
	private JTable entitySystemsTable, baseSystemsTable, managersTable;
	private JTabbedPane tabbedPane;
	private EntityDetailsPanel entityDetailsPanel;

	private int _lastSelectedCol;


	public EntityTrackerMainWindow() {
		this(false);
	}

	public EntityTrackerMainWindow(boolean exitApplicationOnClose) {
		this(true, exitApplicationOnClose);
	}

	public EntityTrackerMainWindow(boolean showWindowOnStart, boolean exitApplicationOnClose) {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception exc) { }

		initialize(showWindowOnStart, exitApplicationOnClose);
	}

	protected void initialize(boolean showWindowOnStart, boolean exitApplicationOnClose) {
		frame = new JFrame("Artemis Entity Tracker");
		frame.setDefaultCloseOperation(exitApplicationOnClose ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
		frame.setBounds(100, 100, 959, 823);
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));

		entitiesTableModel = new EntityTableModel();
		entitiesTable = new EntityTable(entitiesTableModel);

		tableScrollPane = new JScrollPane();
		tableScrollPane.setViewportView(entitiesTable);


		filtersPanel = new JPanel();
//		filtersPanel.add(new JLabel("TODO filters here"));

		filtersScrollPane = new JScrollPane(filtersPanel);

		systemsManagersPanel = new JPanel();
		systemsManagersPanel.setLayout(new CardLayout(0, 0));
		entitySystemsTableModel = new EntitySystemTableModel();
		baseSystemsTableModel = new BaseSystemTableModel();
		managersTableModel = new ManagerTableModel();

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		systemsManagersPanel.add(tabbedPane, "name_959362872326203");

		entitySystemsTable = new AdjustableJTable();
		entitySystemsTable.setAutoCreateRowSorter(true);
		entitySystemsTable.setFillsViewportHeight(true);
		entitySystemsTable.setShowVerticalLines(false);
		entitySystemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		entitySystemsTable.setModel(entitySystemsTableModel);
		JScrollPane entitySystemsTableScrollPane = new JScrollPane();
		entitySystemsTableScrollPane.setViewportView(entitySystemsTable);
		tabbedPane.addTab("Entity Systems", null, entitySystemsTableScrollPane, null);

		baseSystemsTable = new AdjustableJTable();
		baseSystemsTable.setAutoCreateRowSorter(true);
		baseSystemsTable.setFillsViewportHeight(true);
		baseSystemsTable.setShowVerticalLines(false);
		baseSystemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		baseSystemsTable.setModel(baseSystemsTableModel);
		JScrollPane baseSystemsTableScrollPane = new JScrollPane();
		baseSystemsTableScrollPane.setViewportView(baseSystemsTable);
		tabbedPane.addTab("Base Systems", null, baseSystemsTableScrollPane, null);

		managersTable = new JTable();
		managersTable.setAutoCreateRowSorter(true);
		managersTable.setFillsViewportHeight(true);
		managersTable.setShowVerticalLines(false);
		managersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		managersTable = new AdjustableJTable();
		managersTable.setModel(managersTableModel);
		JScrollPane managersTableScrollPane = new JScrollPane();
		managersTableScrollPane.setViewportView(managersTable);
		tabbedPane.addTab("Managers", null, managersTableScrollPane, null);

		detailsPanelContainer = new JScrollPane();
		detailsPanelContainer.setViewportView(new JLabel("Select entity from the table to inspect entity components."));

		systemsDetailsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, systemsManagersPanel, detailsPanelContainer);

		tableFiltersSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, filtersScrollPane);
		tableFiltersSplitPane.setResizeWeight(1.0);

		mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableFiltersSplitPane, systemsDetailsSplitPane);
		mainSplitPane.setResizeWeight(0.5);
		frame.getContentPane().add(mainSplitPane);

		frame.setVisible(showWindowOnStart);

		entitiesTable.addMouseListener(entityRowCellSelectionListener);
		entitiesTable.addKeyListener(entityTableKeyListener);
		entityDetailsPanel = new EntityDetailsPanel(context, entitiesTableModel);



		entitySystemsTableModel.addChangingSystemEnabledStateListener(systemEnableChangingListener);
		baseSystemsTableModel.addChangingSystemEnabledStateListener(systemEnableChangingListener);

		managersTableModel.addChangingSystemEnabledStateListener(new ChangingSystemEnabledStateListener() {
			@Override
			public void onChangingSystemEnabledState(BaseSystemTableModel model, int systemIndex, String managerName, boolean enabled) {
				context.worldController.setManagerState(managerName, enabled);
			}
		});
	}

	public void setVisible(boolean visible) {
		frame.setVisible(visible);
	}

	public boolean isVisible() {
		return frame.isVisible();
	}

	private void selectEntity(int viewRow, int viewCol) {
		if (viewRow < 0) {
			return;
		}
		int modelRow = entitiesTable.convertRowIndexToModel(viewRow);
		int modelCol = entitiesTable.convertColumnIndexToModel(viewCol);

		int entityId = (int) entitiesTableModel.getValueAt(modelRow, 0);
		int componentIndex = modelCol-1;

		BitSet entityComponents = entitiesTableModel.getEntityComponents(entityId);

		if (componentIndex >= 0 && !entityComponents.get(componentIndex)) {
			componentIndex = -1;
		}

		showEntityDetails(entityId, componentIndex);
		_lastSelectedCol = modelCol;
	}

	public void injectWorldController(WorldController worldController) {
		context.worldController = worldController;
	}

	@Override
	public int getListeningBitset() {
		return ENTITY_ADDED | ENTITY_DELETED | ENTITY_SYSTEM_STATS;
	}

	@Override
	public void addedSystem(final int index, final String name, final BitSet allTypes, final BitSet oneTypes, final BitSet notTypes) {
		final boolean hasAspect = allTypes != null || oneTypes != null || notTypes != null;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				baseSystemsTableModel.setSystem(index, name);

				if (hasAspect) {
					entitySystemsTableModel.setSystem(index, name);
				}
			}
		});
	}

	@Override
	public void addedManager(final String name) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				managersTableModel.addManager(name);
			}
		});
	}

	@Override
	public void addedComponentType(final int index, final ComponentTypeInfo info) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				TableColumnModel columns = entitiesTable.getColumnModel();
				TableColumn col = new TableColumn(columns.getColumnCount());
				col.setHeaderValue(info.name);
				col.setModelIndex(info.index);
				columns.addColumn(col);

				entitiesTableModel.setComponentType(index, info);
				setupAllColumnHeadersVerticalRenderer();
			}
		});
	}

	@Override
	public void updatedEntitySystem(final int systemIndex, final int entitiesCount, final int maxEntitiesCount) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entitySystemsTableModel.updateSystem(systemIndex, entitiesCount, maxEntitiesCount);
			}
		});
	}

	@Override
	public void addedEntity(final int entityId, final BitSet components) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entitiesTableModel.addEntity(entityId, components);
			}
		});
	}

	@Override
	public void deletedEntity(final int entityId) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entitiesTableModel.removeEntity(entityId);
			}
		});
	}

	@Override
	public void updatedComponentState(int entityId, int componentIndex, Object[] values) {
		context.eventBus.updatedComponentState(entityId, componentIndex, values);
	}

	@Override
	public void disconnected() {
		entitiesTableModel.clear();
		entitySystemsTableModel.clear();
		managersTableModel.clear();
		detailsPanelContainer.setViewportView(null);
	}

	private void setupAllColumnHeadersVerticalRenderer() {
		TableCellRenderer headerRenderer = new VerticalTableHeaderCellRenderer();
		TableColumnModel columns = entitiesTable.getColumnModel();
		Enumeration<TableColumn> columnIter = columns.getColumns();

		while (columnIter.hasMoreElements()) {
			TableColumn column = columnIter.nextElement();
			column.setHeaderRenderer(headerRenderer);
		}
	}

	protected void showEntityDetails(final int entityId, final int componentIndex) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entityDetailsPanel.selectComponent(entityId, componentIndex);

				if (detailsPanelContainer.getViewport().getView() != entityDetailsPanel) {
					detailsPanelContainer.setViewportView(entityDetailsPanel);
					detailsPanelContainer.revalidate();
					detailsPanelContainer.repaint();
				}
			}
		});
	}

	private MouseListener entityRowCellSelectionListener = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent evt) {
			int row = entitiesTable.rowAtPoint(evt.getPoint());
			int col = entitiesTable.columnAtPoint(evt.getPoint());
			selectEntity(row, col);
		}
	};

	private KeyListener entityTableKeyListener = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int key = e.getKeyCode();

			switch (key) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
					ListSelectionModel selection = entitiesTable.getSelectionModel();
					int currentIndex = selection.getMinSelectionIndex();

					if (key == KeyEvent.VK_UP && currentIndex > 0) {
						currentIndex -= 1;
					}
					else if (key == KeyEvent.VK_DOWN && currentIndex < entitiesTable.getRowCount() - 1) {
						currentIndex += 1;
					}

					selection.setSelectionInterval(currentIndex, currentIndex);
					selectEntity(currentIndex, _lastSelectedCol);

					break;
			}

			e.consume();
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}
	};

	private ChangingSystemEnabledStateListener systemEnableChangingListener = new ChangingSystemEnabledStateListener() {
		@Override
		public void onChangingSystemEnabledState(BaseSystemTableModel model, int systemIndex, String systemName, boolean enabled) {
			entitySystemsTableModel.updateSystemState(systemIndex, enabled);
			baseSystemsTableModel.updateSystemState(systemIndex, enabled);

			context.worldController.setSystemState(systemName, enabled);
		}
	};
}
