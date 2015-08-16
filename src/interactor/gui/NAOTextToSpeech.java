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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import soinnm.Cluster;
import soinnm.SOINNM;
import util.WrapLayout;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;

public class NAOTextToSpeech extends InteractorUDP {

	private static final String NAME = "NAOTextToSpeech";

	private String host = "localhost";
	private int port = 9559;

	private ALTextToSpeechProxy tts;
	private float volumeMax = 1.0f;
	private float volumeForced = -1.0f; // -1.0f to deactivate
	private boolean usePhrases = false; // use with volumeForced = 1;

	private static final String[] PHRASES_NO_IDEA = { "I cannot recall clearly", "I don't remember exactly", "I don't know certainly" };
	private static final String[] PHRASES_NOT_SURE = { "This could be ", "Perhaps it is ", "Possibly it is " };
	private static final String[] PHRASES_CERTAIN = { "I am sure it is ", "That must be ", "I am convinced it is " };

	private static final double ACTIVATION_NO_IDEA = 0.1;
	private static final double ACTIVATION_NOT_SURE = 0.5;
	private static final double ACTIVATION_CERTAIN = 1.0;

	private static final boolean USE_ARM_GESTURES = false;
	private ArmGestures armGestures;

	private Random random;

	private static final int INPUT_DIM = 1;

	private double[] patternInput;
	private double[] patternOutput;

	private static final long CYCLE_TIME_DEFAULT = 1000;

	private JFrame frame;
	private JPanel panel;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JTextField tfInput;
	private JTextField tfOutput;
	private JLabel lbActivationOutput;
	private JSlider slVolumeMax;
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

	public NAOTextToSpeech() {
		super(NAME, INPUT_DIM);

		// initialise
		random = new Random();
		hmInputs = new HashMap<String, Double>();
		hmOutputs = new HashMap<Double, String>();
		nextIndex = 1;

		// initialise NAO
		try {
			BufferedReader bReader = new BufferedReader(new FileReader("NAOHostPort.txt"));
			host = bReader.readLine().trim();
			port = Integer.parseInt(bReader.readLine().trim());
			bReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		tts = new ALTextToSpeechProxy(host, port);

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

		// start arm gestures
		if (USE_ARM_GESTURES) {
			System.out.println("Starting arm gestures...");
			armGestures = new ArmGestures();
			armGestures.start();
		}

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
		pnIOGrid.setLayout(new GridLayout(4, 2));
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

		JLabel lbVolumeMax = new JLabel("Max. Volume:");
		pnIOGrid.add(lbVolumeMax);

		slVolumeMax = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
		slVolumeMax.setMinimumSize(new Dimension(100, 25));
		slVolumeMax.setMaximumSize(new Dimension(100, 25));
		slVolumeMax.setPreferredSize(new Dimension(100, 25));
		slVolumeMax.setSize(new Dimension(100, 25));
		slVolumeMax.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				volumeMax = (float) slVolumeMax.getValue() / 100;
			}
		});
		pnIOGrid.add(slVolumeMax);

		// create panel for controls
		JPanel pnControls = new JPanel();
		pnControls.setLayout(new BoxLayout(pnControls, BoxLayout.Y_AXIS));
		pnControls.setBorder(BorderFactory.createTitledBorder("Controls"));
		pnTop.add(pnControls);

		// create panel for interactor
		JPanel pnInteractor = new JPanel();
		pnInteractor.setLayout(new BoxLayout(pnInteractor, BoxLayout.X_AXIS));
		pnControls.add(pnInteractor);

		spCycleTime = new JSpinner(new SpinnerNumberModel((int) CYCLE_TIME_DEFAULT, 100, 10000, 100));
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
		cbLearn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (cbLearn.isSelected()) {
					tfInput.setEditable(false);
				} else {
					tfInput.setEditable(true);
				}
			}
		});
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

		// pause interactor
		pause();

		// stop all speech
		tts.stopAll();

		// stop arm gestures
		if (armGestures != null) {
			System.out.print("Stopping arm gestures...");
			armGestures.finish();
			while (!armGestures.isFinished()) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.print(".");
			}
			System.out.println("finished");
		}

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

			// stop all speech
			tts.stopAll();

			// move arms
			if (armGestures != null) {
				armGestures.setActivationOutput(-1.0);
			}

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

					// speak
					tts.stopAll();
					if (volumeForced >= 0) {
						tts.setVolume(volumeForced * volumeMax);
					} else {
						tts.setVolume((float) getActivationOutput() * volumeMax);
					}
					if (usePhrases) {
						String phrase = "";
						if (getActivationOutput() <= ACTIVATION_NO_IDEA) {
							phrase = PHRASES_NO_IDEA[random.nextInt(PHRASES_NO_IDEA.length)];
						} else if (getActivationOutput() <= ACTIVATION_NOT_SURE) {
							phrase = PHRASES_NOT_SURE[random.nextInt(PHRASES_NOT_SURE.length)] + output;
						} else if (getActivationOutput() <= ACTIVATION_CERTAIN) {
							phrase = PHRASES_CERTAIN[random.nextInt(PHRASES_CERTAIN.length)] + output;
						}
						tts.say(phrase);
					} else {
						tts.say(output);
					}

					// move arms
					if (armGestures != null) {
						armGestures.setActivationOutput(getActivationOutput());
					}

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

	public float getVolumeMax() {
		return volumeMax;
	}

	public void setVolumeMax(float volumeMax) {
		this.volumeMax = volumeMax;
	}

	public float getVolumeForced() {
		return volumeForced;
	}

	public void setVolumeForced(float volumeForced) {
		this.volumeForced = volumeForced;
	}

	private class ArmGestures extends Thread {

		private boolean running;

		private boolean finished;

		private ALMotionProxy motion;

		private String[] names = { "LShoulderPitch", "LShoulderRoll", "LElbowYaw", "LElbowRoll", "RShoulderPitch", "RShoulderRoll", "RElbowYaw", "RElbowRoll" };

		private float[] anglesIdle = { 1.52f, -0.06f, -0.73f, -1.26f, 1.52f, -0.06f, 0.73f, 1.26f };

		private double activationOutput;

		private float[] anglesNoIdea = { 0.83f, 1.02f, -1.89f, -0.84f, 0.83f, -1.02f, 1.89f, 0.84f };

		private float[] anglesNotSure = { 0.49f, 0.31f, 0.53f, -1.34f, 0.48f, 0.05f, 0.45f, 1.56f };

		private float[] anglesCertain = { 0.53f, 0.64f, -0.33f, -0.95f, -1.18f, -0.75f, 0.13f, 0.79f };

		private float time = 1.5f; // seconds

		private float[] times = { time, time, time, time, time, time, time, time };

		private float timeWait = 0.5f; // seconds

		@Override
		public void run() {

			// initialise
			motion = new ALMotionProxy(host, port);
			running = true;
			finished = false;
			activationOutput = -1.0;

			while (running) {

				// store output activation before movement (main thread can change output activation anytime)
				double activationOutputBefore = activationOutput;

				// set stiffnesses
				if (activationOutput != -1.0 && activationOutputBefore != -1.0) {
					for (int i = 0; i < names.length; i++) {
						motion.setStiffnesses(new Variant(names[i]), new Variant(1.0f));
					}
				}

				// move arms
				if (activationOutput == -1.0) {
					motion.angleInterpolation(new Variant(names), new Variant(anglesIdle), new Variant(times), true);
				} else if (activationOutput <= ACTIVATION_NO_IDEA) {
					motion.angleInterpolation(new Variant(names), new Variant(anglesNoIdea), new Variant(times), true);
				} else if (activationOutput <= ACTIVATION_NOT_SURE) {
					motion.angleInterpolation(new Variant(names), new Variant(anglesNotSure), new Variant(times), true);
				} else if (activationOutput <= ACTIVATION_CERTAIN) {
					motion.angleInterpolation(new Variant(names), new Variant(anglesCertain), new Variant(times), true);
				}

				// wait
				try {
					Thread.sleep(Math.round(timeWait * 1000));
				} catch (Exception e) {
					e.printStackTrace();
				}

				// release stiffnesses
				if (activationOutput == -1.0 && activationOutputBefore == -1.0) {
					for (int i = 0; i < names.length; i++) {
						motion.setStiffnesses(new Variant(names[i]), new Variant(0.0f));
					}
				}

			}

			// set stiffnesses
			for (int i = 0; i < names.length; i++) {
				motion.setStiffnesses(new Variant(names[i]), new Variant(1.0f));
			}

			// move arms
			motion.angleInterpolation(new Variant(names), new Variant(anglesIdle), new Variant(times), true);

			// wait
			try {
				Thread.sleep(Math.round(timeWait * 1000));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// release stiffnesses
			for (int i = 0; i < names.length; i++) {
				motion.setStiffnesses(new Variant(names[i]), new Variant(0.0f));
			}

			// update status
			finished = true;
		}

		public void setActivationOutput(double activationOutput) {
			this.activationOutput = activationOutput;
		}

		public void finish() {
			running = false;
		}

		public boolean isFinished() {
			return finished;
		}

	}

	public static void main(String[] args) {
		NAOTextToSpeech interactor = new NAOTextToSpeech();
		interactor.run();
	}

}
