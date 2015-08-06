package net.namekdev.entity_tracker.ui;

import java.awt.CardLayout;
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
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.namekdev.entity_tracker.connectors.WorldController;
import net.namekdev.entity_tracker.connectors.WorldUpdateListener;
import net.namekdev.entity_tracker.model.ComponentTypeInfo;
import net.namekdev.entity_tracker.ui.model.EntityTableModel;
import net.namekdev.entity_tracker.ui.model.ManagerTableModel;
import net.namekdev.entity_tracker.ui.model.SystemTableModel;
import net.namekdev.entity_tracker.ui.partials.EntityDetailsPanel;
import net.namekdev.entity_tracker.ui.utils.AdjustableJTable;
import net.namekdev.entity_tracker.ui.utils.SelectionListener;
import net.namekdev.entity_tracker.ui.utils.VerticalTableHeaderCellRenderer;

public class EntityTrackerMainWindow implements WorldUpdateListener {
	protected final Context context = new Context();
	protected JFrame frame;
	private JTable entitiesTable;
	private JScrollPane tableScrollPane, filtersScrollPane, detailsPanelContainer;
	private EntityTableModel entitiesTableModel;
	private SystemTableModel systemsTableModel;
	private ManagerTableModel managersTableModel;
	private JSplitPane mainSplitPane, tableFiltersSplitPane, systemsDetailsSplitPane;
	private JPanel filtersPanel, systemsManagersPanel;
	private JTable systemsTable, managersTable;
	private JTabbedPane tabbedPane;
	private EntityDetailsPanel entityDetailsPanel;


	public EntityTrackerMainWindow() {
		this(false);
	}

	public EntityTrackerMainWindow(boolean exitApplicationOnClose) {
		this(true, false);
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


		entitiesTable = new JTable();
		entitiesTable.setAutoCreateRowSorter(true);
		entitiesTable.setShowVerticalLines(false);
		entitiesTable.setFillsViewportHeight(true);
		entitiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JTableHeader tableHeader = entitiesTable.getTableHeader();
		tableHeader.setDefaultRenderer(new VerticalTableHeaderCellRenderer());
		entitiesTableModel = new EntityTableModel();
		entitiesTable.setModel(entitiesTableModel);
		entitiesTable.getColumnModel().getColumn(0).setMaxWidth(10);


		tableScrollPane = new JScrollPane();
		tableScrollPane.setViewportView(entitiesTable);

		filtersPanel = new JPanel();
		filtersPanel.add(new JLabel("TODO filters here"));

		filtersScrollPane = new JScrollPane(filtersPanel);

		systemsManagersPanel = new JPanel();
		systemsManagersPanel.setLayout(new CardLayout(0, 0));
		systemsTableModel = new SystemTableModel();
		managersTableModel = new ManagerTableModel();

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		systemsManagersPanel.add(tabbedPane, "name_959362872326203");

		systemsTable = new AdjustableJTable();
		systemsTable.setAutoCreateRowSorter(true);
		systemsTable.setFillsViewportHeight(true);
		systemsTable.setShowVerticalLines(false);
		systemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		systemsTable.setModel(systemsTableModel);
		JScrollPane systemsTableScrollPane = new JScrollPane();
		systemsTableScrollPane.setViewportView(systemsTable);
		tabbedPane.addTab("Systems", null, systemsTableScrollPane, null);

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
		entityDetailsPanel = new EntityDetailsPanel(context, entitiesTableModel);

		systemsTableModel.addTableModelListener(systemsModelListener);
	}

	public void setVisible(boolean visible) {
		frame.setVisible(visible);
	}

	public boolean isVisible() {
		return frame.isVisible();
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
				systemsTableModel.setSystem(index, name, hasAspect);
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
				systemsTableModel.updateSystem(systemIndex, entitiesCount, maxEntitiesCount);
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

			if (row >= 0) {
				int entityId = (int) entitiesTableModel.getValueAt(row, 0);
				int componentIndex = col-1;

				BitSet entityComponents = entitiesTableModel.getEntityComponents(entityId);
				if (entityComponents.get(componentIndex)) {
					showEntityDetails(entityId, componentIndex);
				}
			}
		}
	};

	private TableModelListener systemsModelListener = new TableModelListener() {
		@Override
		public void tableChanged(TableModelEvent e) {
			if (e.getColumn() != 0) {
				return;
			}

			int rowIndex = e.getFirstRow();
			String systemName = systemsTableModel.getSystemName(rowIndex);
			boolean desiredSystemState = systemsTableModel.getSystemState(rowIndex);

			context.worldController.setSystemState(systemName, desiredSystemState);
		}
	};
}
