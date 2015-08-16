package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;

public class NAOJointsArms extends InteractorUDP {

	private final static String NAME = "NAOJointsArms";

	private String host = "localhost";
	private int port = 9559;

	private ALMotionProxy motion;
	private String[] names = { "LShoulderRoll", "RShoulderRoll", "LShoulderPitch", "RShoulderPitch", "LElbowRoll", "RElbowRoll", "LElbowYaw", "RElbowYaw" };
	private float[] stiffnesses = { 0.8f, 0.8f, 0.8f, 0.8f, 0.4f, 0.4f, 0.4f, 0.4f };
	private float fractionMaxSpeed = 0.1f;
	//private final static float[] ANGLES_MIN = { -0.3142f, -1.3265f, -2.0857f, -2.0857f, -1.5446f, +0.0349f, -2.0857f, -2.0857f }; // required for display
	//private final static float[] ANGLES_MAX = { +1.3265f, +0.3142f, +2.0857f, +2.0857f, -0.0349f, +1.5446f, +2.0857f, +2.0857f }; // required for display
	private final static float[] ANGLES_MIN = { -0.4f, -1.4f, -2.1f, -2.1f, -1.6f, +0.0f, -2.1f, -2.1f }; // required for display
	private final static float[] ANGLES_MAX = { +1.4f, +0.4f, +2.1f, +2.1f, -0.0f, +1.6f, +2.1f, +2.1f }; // required for display
	private boolean interleaving = false; // interleave between weak and strong stiffnesses
	private float[] stiffnessesWeak = { 0.2f, 0.2f, 0.2f, 0.2f, 0.1f, 0.1f, 0.1f, 0.1f };
	private float[] stiffnessesStrong = { 0.8f, 0.8f, 0.8f, 0.8f, 0.4f, 0.4f, 0.4f, 0.4f };
	private float fractionMaxSpeedStrong = 0.1f;
	private final static long FORCE_TIME = 200; // milliseconds
	private boolean stiffnessesByActivation = false; // output activation influences stiffnesses

	private final static int INPUT_DIM = 8;
	private double[] patternInput;
	private double[] patternOutput;
	private final static long CYCLE_TIME_DEFAULT = 400; // ms

	private JFrame frame;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JPanel pnLShoulder;
	private JPanel pnRShoulder;
	private JPanel pnLElbow;
	private JPanel pnRElbow;
	private JSpinner spCycleTime;
	private JLabel lbCycleDuration;
	private JLabel lbCycleTime;
	private JLabel lbOutputActivation;
	private JLabel lbNumNodes;
	private JLabel lbNumEdges;
	private JLabel lbNumClusters;
	private JCheckBox cbInterleaving;
	private JCheckBox cbStiffnessesByActivation;

	public NAOJointsArms() {
		super(NAME, INPUT_DIM);

		// set interactor parameters
		setCycleTime(CYCLE_TIME_DEFAULT);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.02); // 0.02
		topology.setUseFixedThreshold(true); // true
		topology.setFixedThreshold(0.02); // 0.02
		topology.setMinimumThreshold(0.0); // 0.0
		topology.setAgeDead(1000); // 1000
		topology.setConnectNewNodes(false); // false
		topology.setLambda(30); // 30
		topology.setEdgeMaxRemoval(true); // true
		topology.setNodeNumSignalsMinRemoval(false); // false
		topology.setReduceErrorInsertion(true); // true
		topology.setSmallClusterRemoval(true); // true
		topology.setC2Param(0.001); // 0.001
		topology.setC1Param(0.1); // 0.1
		topology.setClusterJoining(true); // true
		topology.setJoinTolerance(1.0); // 1.0
		topology.setUseAbsoluteJoinTolerance(true); // true
		topology.setJoinToleranceAbsolute(0.02); // 0.02
		topology.setJoiningIterationsMax(10); // 10

		// initialise NAO
		try {
			BufferedReader bReader = new BufferedReader(new FileReader("NAOHostPort.txt"));
			host = bReader.readLine().trim();
			port = Integer.parseInt(bReader.readLine().trim());
			bReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		motion = new ALMotionProxy(host, port);
		//motion.setSmartStiffnessEnabled(true);

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
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		frame.add(panel);

		// create top panel
		JPanel pnTop = new JPanel();
		pnTop.setLayout(new BorderLayout());
		panel.add(pnTop, BorderLayout.PAGE_START);

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

		JPanel pnParameters = new JPanel();
		pnParameters.setLayout(new BoxLayout(pnParameters, BoxLayout.Y_AXIS));
		pnInteractor.add(pnParameters);

		cbInterleaving = new JCheckBox("Interleaving");
		cbInterleaving.setSelected(interleaving);
		cbInterleaving.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				interleaving = cbInterleaving.isSelected();
			}
		});
		pnParameters.add(cbInterleaving);

		cbStiffnessesByActivation = new JCheckBox("Stiffnesses by Activation");
		cbStiffnessesByActivation.setSelected(stiffnessesByActivation);
		cbStiffnessesByActivation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stiffnessesByActivation = cbStiffnessesByActivation.isSelected();
			}
		});
		pnParameters.add(cbStiffnessesByActivation);

		lbOutputActivation = new JLabel(" Output Activation: -");
		pnParameters.add(lbOutputActivation);

		// create panel for topology
		JPanel pnTopology = new JPanel();
		//pnTopology.setLayout(new BoxLayout(pnTopology, BoxLayout.X_AXIS));
		pnControls.add(pnTopology);

		JPanel pnLearnRecall = new JPanel();
		pnLearnRecall.setLayout(new BoxLayout(pnLearnRecall, BoxLayout.Y_AXIS));
		pnTopology.add(pnLearnRecall);

		cbLearn = new JCheckBox("Learn");
		cbLearn.setSelected(true);
		pnLearnRecall.add(cbLearn);

		cbRecall = new JCheckBox("Recall");
		cbRecall.setSelected(false);
		pnLearnRecall.add(cbRecall);

		JPanel pnTopologyControls = new JPanel();
		pnTopologyControls.setLayout(new GridLayout(2, 2));
		pnTopology.add(pnTopologyControls);

		fcSaveLoad = new JFileChooser();
		fcSaveLoad.setSelectedFile(new File(System.getProperty("user.dir") + "/" + NAME + ".xml"));

		JButton btClear = new JButton("Clear topology");
		btClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getTopology().clear();
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
		pnTopologyControls.add(btSave);

		JButton btLoad = new JButton("Load topology");
		btLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileLoad = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnTopologyControls.add(btLoad);

		JButton btInsert = new JButton("Insert topology");
		btInsert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileInsert = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnTopologyControls.add(btInsert);

		JPanel pnTopologyStatus = new JPanel();
		pnTopologyStatus.setLayout(new BoxLayout(pnTopologyStatus, BoxLayout.Y_AXIS));
		pnTopology.add(pnTopologyStatus);

		lbNumNodes = new JLabel("Number of Nodes: " + getTopology().getNodeSet().size());
		pnTopologyStatus.add(lbNumNodes);

		lbNumEdges = new JLabel("Number of Edges: " + getTopology().getEdgeSet().size());
		pnTopologyStatus.add(lbNumEdges);

		lbNumClusters = new JLabel("Number of Clusters: " + getTopology().getClusterSet().size());
		pnTopologyStatus.add(lbNumClusters);

		// create clusters panel
		JPanel pnClusters = new JPanel();
		pnClusters.setLayout(new GridLayout(2, 2));
		pnClusters.setBorder(BorderFactory.createTitledBorder("Clusters"));
		panel.add(pnClusters, BorderLayout.CENTER);

		// create panel RShoulder
		pnRShoulder = new JPanel();
		pnRShoulder.setBorder(BorderFactory.createTitledBorder("RShoulder"));
		pnClusters.add(pnRShoulder);

		// create panel LShoulder
		pnLShoulder = new JPanel();
		pnLShoulder.setBorder(BorderFactory.createTitledBorder("LShoulder"));
		pnClusters.add(pnLShoulder);

		// create panel RElbow
		pnRElbow = new JPanel();
		pnRElbow.setBorder(BorderFactory.createTitledBorder("RElbow"));
		pnClusters.add(pnRElbow);

		// create panel LElbow
		pnLElbow = new JPanel();
		pnLElbow.setBorder(BorderFactory.createTitledBorder("LElbow"));
		pnClusters.add(pnLElbow);

		// update panel
		panel.updateUI();

	}

	@Override
	protected void shutdown() {

		// pause interactor
		pause();

		// release stiffnesses
		motion.setStiffnesses(new Variant(names), new Variant(new float[stiffnesses.length]));

		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// read angles
		float[] angles = motion.getAngles(new Variant(names), true);
		double[] pattern = { angles[0], angles[1], angles[2], angles[3], angles[4], angles[5], angles[6], angles[7] };

		// set input pattern
		patternInput = pattern;

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

			// release stiffnesses
			motion.setStiffnesses(new Variant(names), new Variant(new float[stiffnesses.length]));

		} else {

			if (pattern != null) {

				if (interleaving) {

					// set stiffnesses to strong
					motion.setStiffnesses(new Variant(names), new Variant(stiffnessesStrong));

					// set angles
					float[] angles = { (float) pattern[0], (float) pattern[1], (float) pattern[2], (float) pattern[3], (float) pattern[4], (float) pattern[5], (float) pattern[6], (float) pattern[7] };
					motion.setAngles(new Variant(names), new Variant(angles), fractionMaxSpeedStrong);

					// wait
					try {
						Thread.sleep(FORCE_TIME);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// set stiffnesses to weak
					motion.setStiffnesses(new Variant(names), new Variant(stiffnessesWeak));

				} else {

					// set stiffnesses
					if (stiffnessesByActivation) {

						// set stiffness relative to output activation
						float activationOutput = (float) getActivationOutput();
						float[] stiffnessesActivation = new float[names.length];
						for (int i = 0; i < stiffnessesActivation.length; i++) {
							stiffnessesActivation[i] = stiffnesses[i] * activationOutput;
							if (stiffnessesActivation[i] < 0.0f) {
								stiffnessesActivation[i] = 0.0f;
							}
							if (stiffnessesActivation[i] > 1.0f) {
								stiffnessesActivation[i] = 1.0f;
							}
						}
						motion.setStiffnesses(new Variant(names), new Variant(stiffnessesActivation));

					} else {

						// set fixed stiffnesses
						motion.setStiffnesses(new Variant(names), new Variant(stiffnesses));

					}

					// set angles
					float[] angles = { (float) pattern[0], (float) pattern[1], (float) pattern[2], (float) pattern[3], (float) pattern[4], (float) pattern[5], (float) pattern[6], (float) pattern[7] };

					// calculate error
					float[] anglesCurrent = motion.getAngles(new Variant(names), true);
					float anglesError = 0.0f;
					for (int i = 0; i < angles.length; i++) {
						anglesError += Math.abs(angles[i] - anglesCurrent[i]);
					}

					// smoothen movement
					float anglesErrorTolerance = 0.05f * angles.length;
					if (anglesError > anglesErrorTolerance) {
						motion.setAngles(new Variant(names), new Variant(angles), fractionMaxSpeed);
					}

				}

			}

		}
	}

	@Override
	protected void updateUI() {

		/*
		if (STIFFNESSES_BY_ACTIVATION) {
			System.out.println("activationOutput = " + String.format("%.1f", getActivationOutput()));
			System.out.println();
		}
		*/

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

		// output activation
		lbOutputActivation.setText("Output Activation: " + String.format("%.1f", getActivationOutput()));

		draw2DPanel(pnRShoulder, 1, 3);
		draw2DPanel(pnLShoulder, 0, 2);
		draw2DPanel(pnRElbow, 5, 7);
		draw2DPanel(pnLElbow, 4, 6);

	}

	private double[] normalisePattern(double[] pattern) {
		double[] patternNorm = new double[pattern.length];
		for (int i = 0; i < patternNorm.length; i++) {
			patternNorm[i] = (pattern[i] - ANGLES_MIN[i]) / (ANGLES_MAX[i] - ANGLES_MIN[i]);
		}
		return patternNorm;
	}

	private void draw2DPanel(JPanel panel, int index0, int index1) {
		synchronized (panel) {

			// initialise
			Insets insets = panel.getBorder().getBorderInsets(panel);
			int width = panel.getWidth() - insets.left - insets.right;
			int height = panel.getHeight() - insets.top - insets.bottom;
			Graphics g = panel.getGraphics().create(insets.left, insets.top, width, height);
			int xPos, yPos;
			double[] pattern;
			double[] patternNorm;

			// clear pnLShoulder
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);

			// draw grid
			g.setColor(Color.DARK_GRAY);
			g.drawLine(width / 2, 0, width / 2, height - 1);
			g.drawLine(0, height / 2, width - 1, height / 2);

			// draw nodes
			g.setColor(Color.RED);
			int diameterNode = 8;
			for (Node node : getTopology().getNodeSet()) {
				pattern = node.getPattern();
				patternNorm = normalisePattern(pattern);
				xPos = (int) Math.round(patternNorm[index0] * width);
				yPos = (int) Math.round(patternNorm[index1] * height);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw edges
			g.setColor(Color.BLUE);
			int xPosA, yPosA, xPosB, yPosB;
			for (Edge edge : getTopology().getEdgeSet()) {
				pattern = edge.getNodeA().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosA = (int) Math.round(patternNorm[index0] * width);
				yPosA = (int) Math.round(patternNorm[index1] * height);
				pattern = edge.getNodeB().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosB = (int) Math.round(patternNorm[index0] * width);
				yPosB = (int) Math.round(patternNorm[index1] * height);
				g.drawLine(xPosA, yPosA, xPosB, yPosB);
			}

			// draw clusters
			g.setColor(Color.GREEN);
			int diameterCluster = 22;
			FontMetrics metrics = g.getFontMetrics();
			int textHeight = metrics.getHeight();
			int textWidth;
			for (Cluster cluster : getTopology().getClusterSet()) {
				// only clusters with more than one node
				if (cluster.getNodes().size() >= getActivateClusterThreshold()) {
					// highlight activated cluster
					if (cluster == getTopology().getActivatedCluster()) {
						diameterCluster = 36;
						g.setColor(Color.WHITE);
					} else {
						diameterCluster = 22;
						g.setColor(Color.GRAY);
					}
					pattern = cluster.getMean();
					patternNorm = normalisePattern(pattern);
					xPos = (int) Math.round(patternNorm[index0] * width);
					yPos = (int) Math.round(patternNorm[index1] * height);
					g.drawOval(xPos - diameterCluster / 2, yPos - diameterCluster / 2, diameterCluster, diameterCluster);
					String strId = String.valueOf(cluster.getId());
					textWidth = metrics.stringWidth(strId);
					g.drawString(strId, xPos - textWidth / 2, yPos + textHeight / 2);
				}
			}

			// draw input
			if (patternInput != null) {
				patternNorm = normalisePattern(patternInput);
				xPos = (int) Math.round(patternNorm[index0] * width);
				yPos = (int) Math.round(patternNorm[index1] * height);
				g.setColor(Color.WHITE);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw output
			if (patternOutput != null) {
				patternNorm = normalisePattern(patternOutput);
				xPos = (int) Math.round(patternNorm[index0] * width);
				yPos = (int) Math.round(patternNorm[index1] * height);
				g.setColor(Color.GREEN);
				g.fillRect(xPos, yPos, diameterNode, diameterNode);
			}

		}
	}

	public static void main(String[] args) {
		NAOJointsArms interactor = new NAOJointsArms();
		interactor.run();
	}

}
