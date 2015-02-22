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

public class EntityTrackerMainWindow implements UpdateListener {
	private JFrame frame;
	private JTable table;
	private JScrollPane tableScrollPane, filtersScrollPane;
	private EntityTableModel tableModel;
	private JSplitPane mainSplitPane, tableFiltersSplitPane, systemsDetailsSplitPane;
	private JPanel filtersPanel, systemsPanel, detailsPanel;


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
		frame.setBounds(100, 100, 742, 671);
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));


		table = new JTable();
		table.setShowVerticalLines(false);
		table.setFillsViewportHeight(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JTableHeader tableHeader = table.getTableHeader();
		tableHeader.setDefaultRenderer(new VerticalTableHeaderCellRenderer());
		tableModel = new EntityTableModel();
		table.setModel(tableModel);
		table.getColumnModel().getColumn(0).setMaxWidth(40);


		tableScrollPane = new JScrollPane();
		tableScrollPane.add(table);
		tableScrollPane.setViewportView(table);

		filtersPanel = new JPanel();
		filtersPanel.add(new JLabel("TODO filters here"));

		filtersScrollPane = new JScrollPane(filtersPanel);

		systemsPanel = new JPanel();
		systemsPanel.add(new JLabel("TODO systems, managers here"));

		detailsPanel = new JPanel();
		detailsPanel.add(new JLabel("TODO details here"));

		systemsDetailsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, systemsPanel, detailsPanel);

		tableFiltersSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, filtersScrollPane);
		mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableFiltersSplitPane, systemsDetailsSplitPane);
		frame.getContentPane().add(mainSplitPane);

		frame.setVisible(true);
	}

	@Override
	public int getListeningBitset() {
		return UpdateListener.ADDED | UpdateListener.DELETED;
	}

	@Override
	public void added(int entityId, BitSet components) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				tableModel.addEntity(entityId, components);
			}
		});
	}

	@Override
	public void deleted(int entityId) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				tableModel.removeEntity(entityId);
			}
		});
	}

	@Override
	public void addedComponentType(String name) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				TableColumnModel columns = table.getColumnModel();
				TableColumn col = new TableColumn(columns.getColumnCount());
				columns.addColumn(col);

				tableModel.addComponentType(name);
				setupAllColumnHeadersVerticalRenderer();
			}
		});
	}

	private void setupAllColumnHeadersVerticalRenderer() {
		TableCellRenderer headerRenderer = new VerticalTableHeaderCellRenderer();
		TableColumnModel columns = table.getColumnModel();
		Enumeration<TableColumn> columnIter = columns.getColumns();
		while (columnIter.hasMoreElements()) {
			TableColumn column = columnIter.nextElement();
			column.setHeaderRenderer(headerRenderer);
		}
	}

	private void setupColumnLook(int columnIndex) {
		TableColumnModel columns = table.getColumnModel();
		TableColumn column = columns.getColumn(columnIndex);
		column.setHeaderRenderer(new VerticalTableHeaderCellRenderer());
	}
}
