package net.namekdev.entity_tracker.ui;

import java.util.BitSet;
import java.util.Enumeration;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.namekdev.entity_tracker.connectors.UpdateListener;
import net.namekdev.entity_tracker.ui.utils.VerticalTableHeaderCellRenderer;
import java.awt.Component;
import java.awt.BorderLayout;

public class EntityTrackerMainWindow implements UpdateListener {
	private JFrame frame;
	private JTable entitiesTable;
	private JScrollPane tableScrollPane, filtersScrollPane;
	private EntityTableModel entitiesTableModel;
	private EntityObserverTableModel systemsTableModel;
	private JSplitPane mainSplitPane, tableFiltersSplitPane, systemsDetailsSplitPane;
	private JPanel filtersPanel, systemsManagersPanel, detailsPanel;
	private JTable systemsTable;


	public EntityTrackerMainWindow() {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception exc) { }

		initialize();
	}

	public void initialize() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setBounds(100, 100, 959, 823);
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));


		entitiesTable = new JTable();
		entitiesTable.setShowVerticalLines(false);
		entitiesTable.setFillsViewportHeight(true);
		entitiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JTableHeader tableHeader = entitiesTable.getTableHeader();
		tableHeader.setDefaultRenderer(new VerticalTableHeaderCellRenderer());
		entitiesTableModel = new EntityTableModel();
		entitiesTable.setModel(entitiesTableModel);
		entitiesTable.getColumnModel().getColumn(0).setMinWidth(50);


		tableScrollPane = new JScrollPane();
		tableScrollPane.setViewportView(entitiesTable);

		filtersPanel = new JPanel();
		filtersPanel.add(new JLabel("TODO filters here"));

		filtersScrollPane = new JScrollPane(filtersPanel);

		systemsManagersPanel = new JPanel();

		systemsTable = new AdjustableJTable();
		systemsTable.setFillsViewportHeight(true);
		systemsTable.setShowVerticalLines(false);
		systemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		systemsTableModel = new EntityObserverTableModel("system");
		systemsTable.setModel(systemsTableModel);
		entitiesTable.getColumnModel().getColumn(0).setMaxWidth(10);
		systemsManagersPanel.setLayout(new BorderLayout(0, 0));
		JScrollPane systemsTableScrollPane = new JScrollPane();
		systemsTableScrollPane.setViewportView(systemsTable);

		systemsManagersPanel.add(systemsTableScrollPane);

		detailsPanel = new JPanel();
		detailsPanel.add(new JLabel("TODO details here"));

		systemsDetailsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, systemsManagersPanel, detailsPanel);

		tableFiltersSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, filtersScrollPane);
		tableFiltersSplitPane.setResizeWeight(1.0);
		mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableFiltersSplitPane, systemsDetailsSplitPane);
		mainSplitPane.setResizeWeight(0.5);
		frame.getContentPane().add(mainSplitPane);

		frame.setVisible(true);
	}

	@Override
	public int getListeningBitset() {
		return UpdateListener.ADDED | UpdateListener.DELETED;
	}

	@Override
	public void addedEntitySystem(String name, BitSet allTypes, BitSet oneTypes, BitSet notTypes) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				systemsTableModel.addObserver(name);
			}
		});
	}

	@Override
	public void added(int entityId, BitSet components) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entitiesTableModel.addEntity(entityId, components);
			}
		});
	}

	@Override
	public void deleted(int entityId) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				entitiesTableModel.removeEntity(entityId);
			}
		});
	}

	@Override
	public void addedComponentType(String name) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				TableColumnModel columns = entitiesTable.getColumnModel();
				TableColumn col = new TableColumn(columns.getColumnCount());
				columns.addColumn(col);

				entitiesTableModel.addComponentType(name);
				setupAllColumnHeadersVerticalRenderer();
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

	private void setupColumnLook(int columnIndex) {
		TableColumnModel columns = entitiesTable.getColumnModel();
		TableColumn column = columns.getColumn(columnIndex);
		column.setHeaderRenderer(new VerticalTableHeaderCellRenderer());
	}
}
