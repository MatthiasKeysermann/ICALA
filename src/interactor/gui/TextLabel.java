package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import soinnm.Cluster;
import soinnm.SOINNM;
import util.WrapLayout;
import interactor.InteractorUDP;

public class TextLabel extends InteractorUDP {

	private static final String NAME = "TextLabel";

	private static final int INPUT_DIM = 1;

	private double[] patternInput;
	private double[] patternOutput;
	private boolean ignoreEmptyInput = false;

	private static final long CYCLE_TIME_DEFAULT = 200;

	private JFrame frame;
	private JPanel panel;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JTextField tfInput;
	private JTextField tfOutput;
	private JLabel lbActivationOutput;
	private JPanel pnClusters;
	private HashMap<String, Double> hmInputs;
	private HashMap<Double, String> hmOutputs;

	private double nextIndex;

	private JSpinner spCycleTime;
	private JLabel lbCycleDuration;
	private JLabel lbCycleTime;
	private JLabel lbNumNodes;
	private JLabel lbNumEdges;
	private JLabel lbNumClusters;

	public TextLabel() {
		super(NAME, INPUT_DIM);

		// initialise
		hmInputs = new HashMap<String, Double>();
		hmOutputs = new HashMap<Double, String>();
		nextIndex = 1;

		// set interactor parameters
		setCycleTime(CYCLE_TIME_DEFAULT);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.0);
		topology.setUseFixedThreshold(false);
		topology.setFixedThreshold(0.1);
		topology.setAgeDead(1000);
		topology.setConnectNewNodes(false);
		topology.setLambda(100);
		topology.setEdgeMaxRemoval(false);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(false);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.0);
		topology.setC1Param(0.0);
		topology.setClusterJoining(false);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(true);
		topology.setJoinToleranceAbsolute(0.1);
		topology.setJoiningIterationsMax(10);

		// create frame
		frame = new JFrame(NAME);
		frame.setSize(600, 600);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				shutdown();
			}
		});
		frame.setLocation(600, 0);
		frame.setVisible(true);

		// create panel
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		frame.add(panel);

		// create top panel
		JPanel pnTop = new JPanel();
		pnTop.setLayout(new BorderLayout());
		panel.add(pnTop, BorderLayout.PAGE_START);

		// create panel for input and output
		JPanel pnIO = new JPanel();
		pnIO.setBorder(BorderFactory.createTitledBorder("Input/Output"));
		pnTop.add(pnIO, BorderLayout.LINE_START);

		JPanel pnIOGrid = new JPanel();
		pnIOGrid.setLayout(new GridLayout(3, 2));
		pnIO.add(pnIOGrid);

		JLabel lbInput = new JLabel("Input:");
		pnIOGrid.add(lbInput);

		tfInput = new JTextField("");
		tfInput.setMinimumSize(new Dimension(100, 25));
		tfInput.setMaximumSize(new Dimension(100, 25));
		tfInput.setPreferredSize(new Dimension(100, 25));
		tfInput.setSize(new Dimension(100, 25));
		tfInput.setEditable(true);
		pnIOGrid.add(tfInput);

		JLabel lbOutput = new JLabel("Output:");
		pnIOGrid.add(lbOutput);

		tfOutput = new JTextField("");
		tfOutput.setMinimumSize(new Dimension(100, 25));
		tfOutput.setMaximumSize(new Dimension(100, 25));
		tfOutput.setPreferredSize(new Dimension(100, 25));
		tfOutput.setSize(new Dimension(100, 25));
		tfOutput.setEditable(false);
		pnIOGrid.add(tfOutput);

		JLabel lbActivation = new JLabel("Activation:");
		pnIOGrid.add(lbActivation);

		lbActivationOutput = new JLabel("");
		pnIOGrid.add(lbActivationOutput);

		// create panel for controls
		JPanel pnControls = new JPanel();
		pnControls.setLayout(new BoxLayout(pnControls, BoxLayout.Y_AXIS));
		pnControls.setBorder(BorderFactory.createTitledBorder("Controls"));
		pnTop.add(pnControls);

		// create panel for interactor
		JPanel pnInteractor = new JPanel();
		pnInteractor.setLayout(new BoxLayout(pnInteractor, BoxLayout.X_AXIS));
		pnControls.add(pnInteractor);

		spCycleTime = new JSpinner(new SpinnerNumberModel((int) CYCLE_TIME_DEFAULT, 100, 1000, 100));
		spCycleTime.setMinimumSize(new Dimension(75, 25));
		spCycleTime.setMaximumSize(new Dimension(75, 25));
		pnInteractor.add(spCycleTime);
		JButton btCycleTime = new JButton("Set Cycle Time");
		btCycleTime.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCycleTime(Long.parseLong(spCycleTime.getValue().toString()));
			}
		});
		pnInteractor.add(btCycleTime);

		JPanel pnCycleStatus = new JPanel();
		pnCycleStatus.setLayout(new BoxLayout(pnCycleStatus, BoxLayout.Y_AXIS));
		pnInteractor.add(pnCycleStatus);

		lbCycleTime = new JLabel("Cycle Time: " + getCycleTime() + " ms");
		pnCycleStatus.add(lbCycleTime);

		lbCycleDuration = new JLabel("Cycle Duration: - ms");
		pnCycleStatus.add(lbCycleDuration);

		JPanel pnLearnRecall = new JPanel();
		pnLearnRecall.setLayout(new BoxLayout(pnLearnRecall, BoxLayout.X_AXIS));
		pnControls.add(pnLearnRecall);

		cbLearn = new JCheckBox("Learn");
		cbLearn.setSelected(false);
		pnLearnRecall.add(cbLearn);

		cbRecall = new JCheckBox("Recall");
		cbRecall.setSelected(true);
		pnLearnRecall.add(cbRecall);

		// create panel for topology
		JPanel pnTopology = new JPanel();
		pnTopology.setLayout(new GridLayout(1, 2));
		pnControls.add(pnTopology);

		JPanel pnTopologyStatus = new JPanel();
		pnTopologyStatus.setLayout(new BoxLayout(pnTopologyStatus, BoxLayout.Y_AXIS));
		pnTopology.add(pnTopologyStatus);

		lbNumNodes = new JLabel("Number of Nodes: " + getTopology().getNodeSet().size());
		pnTopologyStatus.add(lbNumNodes);

		lbNumEdges = new JLabel("Number of Edges: " + getTopology().getEdgeSet().size());
		pnTopologyStatus.add(lbNumEdges);

		lbNumClusters = new JLabel("Number of Clusters: " + getTopology().getClusterSet().size());
		pnTopologyStatus.add(lbNumClusters);

		JPanel pnTopologyControls = new JPanel();
		pnTopologyControls.setLayout(new BoxLayout(pnTopologyControls, BoxLayout.Y_AXIS));
		pnTopology.add(pnTopologyControls);

		fcSaveLoad = new JFileChooser();
		fcSaveLoad.setSelectedFile(new File(System.getProperty("user.dir") + "/" + NAME + ".xml"));

		JButton btClear = new JButton("Clear topology");
		btClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getTopology().clear();
				// TODO: clear HashMaps?
				// TODO: reset index?
			}
		});
		pnTopologyControls.add(btClear);

		JButton btSave = new JButton("Save topology");
		btSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileSave = fcSaveLoad.getSelectedFile();
				}
			}
		});
		btSave.setEnabled(false); // Problem: HashMaps are not saved!
		pnTopologyControls.add(btSave);

		JButton btLoad = new JButton("Load topology");
		btLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileLoad = fcSaveLoad.getSelectedFile();
				}
			}
		});
		btLoad.setEnabled(false); // Problem: HashMaps are not loaded!
		pnTopologyControls.add(btLoad);

		JButton btInsert = new JButton("Insert topology");
		btInsert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileInsert = fcSaveLoad.getSelectedFile();
				}
			}
		});
		btInsert.setEnabled(false); // Problem: HashMaps are not inserted!
		pnTopologyControls.add(btInsert);

		// create panel for clusters
		pnClusters = new JPanel();
		pnClusters.setBorder(BorderFactory.createTitledBorder("Clusters"));
		pnClusters.setLayout(new WrapLayout(WrapLayout.LEFT));
		JScrollPane spClusters = new JScrollPane(pnClusters);
		panel.add(spClusters);

		// update panel
		panel.updateUI();
	}

	@Override
	protected void shutdown() {
		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// insert input
		String input = tfInput.getText();
		if (hmInputs.get(input) == null) {
			hmInputs.put(input, nextIndex);
			hmOutputs.put(nextIndex, input);
			nextIndex++;
		}

		// create pattern
		double[] pattern = new double[INPUT_DIM];
		pattern[0] = hmInputs.get(input);

		// set input pattern
		patternInput = pattern;

		// ignore empty inputs
		if (ignoreEmptyInput) {
			if (input.equals("")) {
				return null;
			}
		}

		if (!cbLearn.isSelected()) {
			return null;
		}
		return pattern;
	}

	@Override
	protected void writeOutput(double[] pattern) {

		// set output pattern
		patternOutput = pattern;

		if (!cbRecall.isSelected()) {

			// reset text
			tfOutput.setText("");
			lbActivationOutput.setText("");

		} else {

			if (pattern != null) {

				// approximate output
				//double index = Math.round(pattern[0]);
				// Possible averaging error with many activated clusters!

				// use cluster with maximum activation 				
				double activationMax = Double.MIN_VALUE;
				Cluster clusterMax = null;
				for (Cluster cluster : getActivationMap().keySet()) {
					double activation = getActivationMap().get(cluster);
					if (activation > activationMax) {
						activationMax = activation;
						clusterMax = cluster;
					}
				}
				double index = clusterMax.getMean()[0];

				// fetch string
				String output = hmOutputs.get(index);

				if (output == null) {

					System.err.println("Output is null!");

				} else {

					// set text
					tfOutput.setText(output);
					lbActivationOutput.setText(String.format("%.2f", getActivationOutput()));

				}

			}

		}

	}

	@Override
	protected void updateUI() {

		/*
		// topology status
		System.out.println("Topology has");
		System.out.println("  " + getTopology().getNodeSet().size() + " nodes");
		System.out.println("  " + getTopology().getEdgeSet().size() + " edges");
		System.out.println("  " + getTopology().getClusterSet().size() + " clusters");
		System.out.println();
		*/

		// interactor
		lbCycleDuration.setText("Cycle Duration: " + getCycleDuration() + " ms");
		lbCycleTime.setText("Cycle Time: " + getCycleTime() + " ms");

		// topology
		lbNumNodes.setText("Number of Nodes: " + getTopology().getNodeSet().size());
		lbNumEdges.setText("Number of Edges: " + getTopology().getEdgeSet().size());
		lbNumClusters.setText("Number of Clusters: " + getTopology().getClusterSet().size());

		// input
		if (patternInput != null) {
			// do nothing
		}

		// output
		if (patternOutput != null) {
			// do nothing
		}

		// clusters
		pnClusters.removeAll();
		for (Cluster cluster : getTopology().getClusterSet()) {

			// retrieve pattern
			double[] pattern = cluster.getPrototype();

			// look up string
			String strCluster = hmOutputs.get(pattern[0]);

			// create label
			JLabel lbCluster = new JLabel(cluster.getId() + ": " + strCluster);
			lbCluster.setBorder(BorderFactory.createEtchedBorder());
			pnClusters.add(lbCluster);

		}

		// update panel
		panel.updateUI();

	}

	public void setTextInput(String textInput) {
		tfInput.setText(textInput);
	}

	public void setLearning(boolean learning) {
		cbLearn.setSelected(learning);
	}

	public void setRecalling(boolean recalling) {
		cbRecall.setSelected(recalling);
	}

	public String getOutput() {
		return tfOutput.getText();
	}

	public BufferedImage getClusterPanelImage() {
		BufferedImage image = new BufferedImage(pnClusters.getWidth(), pnClusters.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		pnClusters.paint(g);
		return image;
	}

	public boolean isIgnoreEmptyInput() {
		return ignoreEmptyInput;
	}

	public void setIgnoreEmptyInput(boolean ignoreEmptyInput) {
		this.ignoreEmptyInput = ignoreEmptyInput;
	}

	public static void main(String[] args) {
		TextLabel interactor = new TextLabel();
		interactor.run();
	}

}
