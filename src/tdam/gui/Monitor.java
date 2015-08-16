package tdam.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import tdam.Association;
import tdam.Simulation;
import tdam.Unit;

public class Monitor extends Simulation {

	private static final String TITLE = "Monitor (TDAM)";

	private JFrame frame;
	private JLabel lbCycleDuration;
	private JLabel lbCycleTime;
	private JSpinner spCycleTime;
	private JLabel lbNumUnits;
	private JLabel lbNumAssociations;
	private JFileChooser fcSaveLoad;
	private JCheckBox cbUnitsUpdate;
	private TableModelUnits tmUnits;
	private JCheckBox cbAssociationsUpdate;
	private TableModelAssociations tmAssociations;

	public Monitor() {
		super();

		// frame

		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle(TITLE);
		frame.setSize(720, 600);
		frame.setLocation(1200, 0);

		JPanel pnMain = new JPanel();
		pnMain.setLayout(new BoxLayout(pnMain, BoxLayout.Y_AXIS));
		frame.getContentPane().add(pnMain);

		// simulation

		JPanel pnSimulation = new JPanel();
		pnSimulation.setBorder(BorderFactory.createTitledBorder("Simulation"));
		pnSimulation.setLayout(new BoxLayout(pnSimulation, BoxLayout.X_AXIS));
		pnMain.add(pnSimulation);

		JPanel pnActions = new JPanel();
		pnActions.setLayout(new BoxLayout(pnActions, BoxLayout.X_AXIS));
		pnSimulation.add(pnActions);

		JButton btPause = new JButton("Pause");
		btPause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pause();
			}
		});
		pnActions.add(btPause);

		JButton btResume = new JButton("Resume");
		btResume.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resume();
			}
		});
		pnActions.add(btResume);

		JPanel pnCycleTime = new JPanel();
		pnCycleTime.setLayout(new BoxLayout(pnCycleTime, BoxLayout.X_AXIS));
		pnSimulation.add(pnCycleTime);

		spCycleTime = new JSpinner();
		spCycleTime.setModel(new SpinnerNumberModel(100, 0, 10000, 100));
		spCycleTime.setMinimumSize(new Dimension(75, 25));
		spCycleTime.setMaximumSize(new Dimension(75, 25));
		pnCycleTime.add(spCycleTime);

		JButton btCycleTime = new JButton("Set Cycle Time");
		btCycleTime.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCycleTime(Long.parseLong(String.valueOf(spCycleTime.getValue())));
			}
		});
		pnCycleTime.add(btCycleTime);

		JPanel pnCycle = new JPanel();
		pnCycle.setLayout(new BoxLayout(pnCycle, BoxLayout.Y_AXIS));
		pnSimulation.add(pnCycle);

		lbCycleTime = new JLabel("Cycle Time: " + getCycleTime() + " ms");
		pnCycle.add(lbCycleTime);

		lbCycleDuration = new JLabel("Cycle Duration: - ms");
		pnCycle.add(lbCycleDuration);

		// network

		JPanel pnNetwork = new JPanel();
		pnNetwork.setBorder(BorderFactory.createTitledBorder("Network"));
		pnNetwork.setLayout(new BoxLayout(pnNetwork, BoxLayout.X_AXIS));
		pnMain.add(pnNetwork);

		JPanel pnComplexity = new JPanel();
		pnComplexity.setLayout(new BoxLayout(pnComplexity, BoxLayout.Y_AXIS));
		pnNetwork.add(pnComplexity);

		lbNumUnits = new JLabel("Number of Units: " + getNetwork().getUnits().size());
		lbNumUnits.setMinimumSize(new Dimension(200, 20));
		lbNumUnits.setMaximumSize(new Dimension(200, 20));
		pnComplexity.add(lbNumUnits);

		lbNumAssociations = new JLabel("Number of Associations: " + getNetwork().getAssociations().size());
		pnComplexity.add(lbNumAssociations);

		JButton btClearNetwork = new JButton("Clear Network");
		btClearNetwork.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clearNetwork();
			}
		});
		pnNetwork.add(btClearNetwork);

		fcSaveLoad = new JFileChooser();
		fcSaveLoad.setSelectedFile(new File(System.getProperty("user.dir") + "/TDAM.xml"));

		JButton btSave = new JButton("Save Network");
		btSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileSave = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnNetwork.add(btSave);

		JButton btLoad = new JButton("Load Network");
		btLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileLoad = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnNetwork.add(btLoad);

		// units

		JPanel pnUnits = new JPanel();
		pnUnits.setBorder(BorderFactory.createTitledBorder("Units"));
		pnUnits.setLayout(new BoxLayout(pnUnits, BoxLayout.Y_AXIS));
		pnMain.add(pnUnits);

		cbUnitsUpdate = new JCheckBox();
		cbUnitsUpdate.setText("Update");
		cbUnitsUpdate.setSelected(true);
		pnUnits.add(cbUnitsUpdate);

		tmUnits = new TableModelUnits();
		TableUnits tUnits = new TableUnits(tmUnits);
		JScrollPane spUnits = new JScrollPane(tUnits);
		pnUnits.add(spUnits);

		// associations

		JPanel pnAssociations = new JPanel();
		pnAssociations.setBorder(BorderFactory.createTitledBorder("Associations"));
		pnAssociations.setLayout(new BoxLayout(pnAssociations, BoxLayout.Y_AXIS));
		pnMain.add(pnAssociations);

		cbAssociationsUpdate = new JCheckBox();
		cbAssociationsUpdate.setText("Update");
		cbAssociationsUpdate.setSelected(true);
		pnAssociations.add(cbAssociationsUpdate);

		tmAssociations = new TableModelAssociations();
		TableAssociations tAssociations = new TableAssociations(tmAssociations);
		JScrollPane spAssociations = new JScrollPane(tAssociations);
		pnAssociations.add(spAssociations);

		// finalise

		for (Component component : pnMain.getComponents()) {
			((JComponent) component).setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		frame.setVisible(true);

	}

	@Override
	protected void updateUI() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				// simulation

				lbCycleDuration.setText("Cycle Duration: " + getCycleDuration() + " ms");
				lbCycleTime.setText("Cycle Time: " + getCycleTime() + " ms");

				// network

				lbNumUnits.setText("Number of Units: " + getNetwork().getUnits().size());
				lbNumAssociations.setText("Number of Associations: " + getNetwork().getAssociations().size());

				// units

				if (cbUnitsUpdate.isSelected()) {

					// clear table model
					while (tmUnits.getRowCount() > 0) {
						tmUnits.removeRow(0);
					}

					LinkedList<Unit> units = getNetwork().getUnits();
					synchronized (units) {
						for (Unit unit : units) {

							// update table model
							Object[] rowData = new Object[tmUnits.getColumnCount()];
							rowData[0] = unit.getId();
							rowData[1] = String.valueOf(unit.getData());
							rowData[2] = unit.getInput();
							rowData[3] = unit.getTrace();
							rowData[4] = unit.getSignalSum();
							rowData[5] = unit.getError();
							tmUnits.addRow(rowData);

						}
					}

				}

				// associations

				if (cbAssociationsUpdate.isSelected()) {

					// clear table model
					while (tmAssociations.getRowCount() > 0) {
						tmAssociations.removeRow(0);
					}

					LinkedList<Association> associations = getNetwork().getAssociations();
					synchronized (associations) {
						for (Association association : associations) {

							// update table model
							Object[] rowData = new Object[tmAssociations.getColumnCount()];
							rowData[0] = association.getId();
							Unit unitSrc = association.getSrc();
							//rowData[1] = String.valueOf(unitSrc.getData()) + " [" + unitSrc.getId() + "]";
							rowData[1] = String.valueOf(unitSrc.getData());
							Unit unitDst = association.getDst();
							//rowData[2] = String.valueOf(unitDst.getData()) + " [" + unitDst.getId() + "]";
							rowData[2] = String.valueOf(unitDst.getData());
							rowData[3] = association.getStrength();
							tmAssociations.addRow(rowData);

						}
					}

				}

			}
		});
	}

	private class TableModelUnits extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		public TableModelUnits() {
			super();
			addColumn("ID");
			addColumn("Data");
			addColumn("Input");
			addColumn("Trace");
			addColumn("Signal Sum");
			addColumn("Error");
		}

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
			case 0:
				return Long.class;
			case 1:
				return Object.class;
			case 2:
				return Double.class;
			case 3:
				return Double.class;
			case 4:
				return Double.class;
			case 5:
				return Double.class;
			default:
				return Object.class;
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

	}

	private class TableUnits extends JTable {

		private static final long serialVersionUID = 1L;

		public TableUnits(TableModelUnits tableModel) {
			super(tableModel);
			setAutoCreateRowSorter(true);
		}

		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			Component component = super.prepareRenderer(renderer, row, column);

			double signalSum = (Double) this.getValueAt(row, 4);
			double signalSumMin = 0;
			double signalSumMax = 1;
			if (signalSum < signalSumMin) {
				signalSum = signalSumMin;
			} else if (signalSum > signalSumMax) {
				signalSum = signalSumMax;
			}
			double range = signalSumMax - signalSumMin;
			int grey = (int) Math.round((signalSum - signalSumMin) / range * 155) + 100;
			if (grey < 0)
				grey = 0;
			if (grey > 255)
				grey = 255;
			component.setBackground(new Color(grey, grey, grey));

			return component;
		}
	}

	private class TableModelAssociations extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		public TableModelAssociations() {
			super();
			addColumn("ID");
			addColumn("Source");
			addColumn("Destination");
			addColumn("Strength");
		}

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
			case 0:
				return Long.class;
			case 1:
				return Object.class;
			case 2:
				return Object.class;
			case 3:
				return Double.class;
			default:
				return Object.class;
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

	}

	private class TableAssociations extends JTable {

		private static final long serialVersionUID = 1L;

		public TableAssociations(TableModelAssociations tableModel) {
			super(tableModel);
			setAutoCreateRowSorter(true);
			if (getRowSorter() != null) {
				getRowSorter().toggleSortOrder(3);
				getRowSorter().toggleSortOrder(3);
			}
		}

		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			Component component = super.prepareRenderer(renderer, row, column);

			double strength = (Double) this.getValueAt(row, 3);
			double strengthMin = -1;
			double strengthMax = +1;
			if (strength < strengthMin) {
				strength = strengthMin;
			} else if (strength > strengthMax) {
				strength = strengthMax;
			}
			double range = strengthMax - strengthMin;
			int grey = (int) Math.round((strength - strengthMin) / range * 155) + 100;
			if (grey < 0)
				grey = 0;
			if (grey > 255)
				grey = 255;
			component.setBackground(new Color(grey, grey, grey));

			return component;
		}
	}

	public static void main(String[] args) {
		Monitor monitor = new Monitor();
		monitor.run();
	}

}
