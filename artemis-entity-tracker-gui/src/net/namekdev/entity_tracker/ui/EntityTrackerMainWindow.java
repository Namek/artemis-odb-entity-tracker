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
import net.namekdev.entity_tracker.ui.model.EntityTableModel;
import net.namekdev.entity_tracker.ui.model.ManagerTableModel;
import net.namekdev.entity_tracker.ui.model.SystemTableModel;
import net.namekdev.entity_tracker.ui.partials.EntityDetailsPanel;
import net.namekdev.entity_tracker.ui.utils.AdjustableJTable;
import net.namekdev.entity_tracker.ui.utils.SelectionListener;
import net.namekdev.entity_tracker.ui.utils.VerticalTableHeaderCellRenderer;

public class EntityTrackerMainWindow implements WorldUpdateListener {
	private WorldController worldController;
	private JFrame frame;
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
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception exc) { }

		initialize(exitApplicationOnClose);
	}

	protected void initialize(boolean exitApplicationOnClose) {
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
		detailsPanelContainer.setViewportView(new JLabel("TODO details here"));

		systemsDetailsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, systemsManagersPanel, detailsPanelContainer);

		tableFiltersSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, filtersScrollPane);
		tableFiltersSplitPane.setResizeWeight(1.0);

		mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableFiltersSplitPane, systemsDetailsSplitPane);
		mainSplitPane.setResizeWeight(0.5);
		frame.getContentPane().add(mainSplitPane);

		frame.setVisible(true);

		entitiesTable.getSelectionModel().addListSelectionListener(entitySelectionListener);
		entitiesTable.addMouseListener(rightBtnCellSelectionListener);
		entityDetailsPanel = new EntityDetailsPanel(entitiesTableModel);

		systemsTableModel.addTableModelListener(systemsModelListener);
	}

	public void injectWorldController(WorldController worldController) {
		this.worldController = worldController;
	}

	@Override
	public int getListeningBitset() {
		return ENTITY_ADDED | ENTITY_DELETED | ENTITY_SYSTEM_STATS;
	}

	@Override
	public void addedSystem(int index, String name, BitSet allTypes, BitSet oneTypes, BitSet notTypes) {
		boolean hasAspect = allTypes != null || oneTypes != null || notTypes != null;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				systemsTableModel.setSystem(index, name, hasAspect);
			}
		});
	}

	@Override
	public void addedManager(String name) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				managersTableModel.addManager(name);
			}
		});
	}

	@Override
	public void addedComponentType(int index, String name) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				TableColumnModel columns = entitiesTable.getColumnModel();
				TableColumn col = new TableColumn(columns.getColumnCount());
				columns.addColumn(col);

				entitiesTableModel.setComponentType(index, name);
				setupAllColumnHeadersVerticalRenderer();
			}
		});
	}

	@Override
	public void updatedEntitySystem(int systemIndex, int entitiesCount, int maxEntitiesCount) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				systemsTableModel.updateSystem(systemIndex, entitiesCount, maxEntitiesCount);
			}
		});
	}

	@Override
	public void addedEntity(int entityId, BitSet components) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entitiesTableModel.addEntity(entityId, components);
			}
		});
	}

	@Override
	public void deletedEntity(int entityId) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entitiesTableModel.removeEntity(entityId);
			}
		});
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

	protected void showEntityDetails(int entityId, int componentIndex) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entityDetailsPanel.setup(entityId, componentIndex);
				detailsPanelContainer.setViewportView(entityDetailsPanel);
				detailsPanelContainer.revalidate();
				detailsPanelContainer.repaint();
			}
		});
	}

	private ListSelectionListener entitySelectionListener = new SelectionListener() {
		@Override
		public void rowSelected(int index) {
			int entityId = (int) entitiesTableModel.getValueAt(index, 0);
			showEntityDetails(entityId, -1);
		}
	};

	private MouseListener rightBtnCellSelectionListener = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent evt) {
			if (!SwingUtilities.isRightMouseButton(evt)) {
				return;
			}

			int row = entitiesTable.rowAtPoint(evt.getPoint());
			int col = entitiesTable.columnAtPoint(evt.getPoint());

			if (row >= 0) {
				int entityId = (int) entitiesTableModel.getValueAt(row, 0);
				int componentIndex = col-1;

				showEntityDetails(entityId, componentIndex);
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

			worldController.setSystemState(systemName, desiredSystemState);
		}
	};
}
