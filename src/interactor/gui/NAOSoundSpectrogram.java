package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import soinnm.Cluster;
import soinnm.SOINNM;
import util.ImagePanel;
import util.PatternPanel;
import util.WrapLayout;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;

/*
 * "soundspectrum" needs to run on NAO in order to process sound from the microphone and write data to ALMemory.
 * "soundgeneration" needs to run on NAO in order to generate sound from the data written to ALMemory (optional).
 */
public class NAOSoundSpectrogram extends InteractorUDP {

	private static final String NAME = "NAOSoundSpectrogram";

	private String host = "localhost";
	private int port = 9559;

	private ALMemoryProxy memory;
	private boolean simulateSine = false;

	private static final int NUM_BINS = 16;
	private static final int NUM_SPECTRA = 10;
	private static final int INPUT_DIM = NUM_BINS * NUM_SPECTRA;
	private int inputSpectrumIndex = 0;
	private double[] patternInputTemp;
	private double[] patternInput;
	private int outputSpectrumIndex = 0;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 200; // each spectrogram spans CYCLE_TIME * NUM_SPECTRA ms

	private JFrame frame;
	private JPanel panel;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JPanel pnIO;
	private JPanel pnClusters;
	private PatternPanel ppInput;
	private PatternPanel ppOutput;

	private final static int BIN_WIDTH = 9;
	private final static int BIN_HEIGHT = 4;
	private final static int BIN_SPACING_X = 1;
	private final static int BIN_SPACING_Y = 1;
	private final static int IMAGE_WIDTH = BIN_SPACING_X + (BIN_WIDTH + BIN_SPACING_X) * NUM_SPECTRA;
	private final static int IMAGE_HEIGHT = BIN_SPACING_Y + (BIN_HEIGHT + BIN_SPACING_Y) * NUM_BINS;

	private final static BufferedImage IMAGE_BLANK = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

	public NAOSoundSpectrogram() {
		super(NAME, INPUT_DIM);

		// initialise
		patternInputTemp = new double[INPUT_DIM];
		patternInput = new double[INPUT_DIM];
		patternOutput = new double[INPUT_DIM];

		// initialise NAO
		try {
			BufferedReader bReader = new BufferedReader(new FileReader("NAOHostPort.txt"));
			host = bReader.readLine().trim();
			port = Integer.parseInt(bReader.readLine().trim());
			bReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		memory = new ALMemoryProxy(host, port);

		// set interactor parameters
		setCycleTime(CYCLE_TIME);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.01);
		topology.setUseFixedThreshold(true);
		topology.setFixedThreshold(0.04);
		topology.setAgeDead(50);
		topology.setConnectNewNodes(false);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(false);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(true);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.01);
		topology.setC1Param(0.1);
		topology.setClusterJoining(true);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(true);
		topology.setJoinToleranceAbsolute(0.04);
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
		pnIO = new JPanel();
		pnIO.setLayout(new FlowLayout(FlowLayout.LEFT));
		pnIO.setBorder(BorderFactory.createTitledBorder("Input/Output"));

		ImagePanel ipInput = new ImagePanel(IMAGE_BLANK);
		ppInput = new PatternPanel(ipInput, "Input", "");
		pnIO.add(ppInput);

		ImagePanel ipOutput = new ImagePanel(IMAGE_BLANK);
		ppOutput = new PatternPanel(ipOutput, "Output", "");
		pnIO.add(ppOutput);

		pnTop.add(pnIO, BorderLayout.LINE_START);

		// create panel for controls
		JPanel pnControls = new JPanel();
		pnControls.setLayout(new WrapLayout(WrapLayout.LEFT));
		pnControls.setBorder(BorderFactory.createTitledBorder("Controls"));

		cbLearn = new JCheckBox("Learn");
		cbLearn.setSelected(true);
		pnControls.add(cbLearn);

		cbRecall = new JCheckBox("Recall");
		cbRecall.setSelected(true);
		pnControls.add(cbRecall);

		fcSaveLoad = new JFileChooser();
		fcSaveLoad.setSelectedFile(new File(System.getProperty("user.dir") + "/" + NAME + ".xml"));

		JButton btSave = new JButton("Save topology");
		btSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileSave = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnControls.add(btSave);

		JButton btLoad = new JButton("Load topology");
		btLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileLoad = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnControls.add(btLoad);

		JButton btInsert = new JButton("Insert topology");
		btInsert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileInsert = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnControls.add(btInsert);

		JButton btClear = new JButton("Clear topology");
		btClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getTopology().clear();
			}
		});
		pnControls.add(btClear);

		JCheckBox cbSimulateSine = new JCheckBox("Simulate Sine Waves");
		cbSimulateSine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				simulateSine = cb.isSelected();
			}
		});
		pnControls.add(cbSimulateSine);

		pnTop.add(pnControls);

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

		// reset data in ALMemory
		Variant varList = new Variant();
		for (int i = 0; i < NUM_BINS; i++) {
			Variant varName = new Variant("ALSoundGeneration/mono" + i);
			Variant varValue = new Variant(0.0f);
			Variant varData = new Variant();
			varData.push_back(varName);
			varData.push_back(varValue);
			varList.push_back(varData);
		}
		memory.insertListData(varList);

		// reset output activation
		memory.insertData("ALSoundGeneration/activation", 0.0f);

		// reset simulate sine
		memory.insertData("ALSoundSpectrum/simulateSine", 0.0f);

		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// simulate sine waves
		if (simulateSine) {
			memory.insertData("ALSoundSpectrum/simulateSine", 1.0f);
		} else {
			memory.insertData("ALSoundSpectrum/simulateSine", 0.0f);
		}

		// fetch data from memory
		Variant varList = new Variant();
		for (int i = 0; i < NUM_BINS; i++) {
			Variant varKey = new Variant("ALSoundSpectrum/front" + i);
			varList.push_back(varKey);
		}
		Variant varListData = null;
		try {
			varListData = memory.getListData(varList);
		} catch (Exception e) {
			System.err.println("ALSoundSpectrum value could not be read!");
		}

		// append to temporary pattern
		if (varListData != null) {
			for (int i = 0; i < NUM_BINS; i++) {
				patternInputTemp[inputSpectrumIndex * NUM_BINS + i] = (double) varListData.getElement(i).toFloat();
			}
		} else {
			for (int i = 0; i < NUM_BINS; i++) {
				patternInputTemp[inputSpectrumIndex * NUM_BINS + i] = 0.0;
			}
		}

		// increase input spectrum index
		inputSpectrumIndex++;
		if (inputSpectrumIndex == NUM_SPECTRA) {

			// reset input spectrum index
			inputSpectrumIndex = 0;

			// set input pattern
			for (int i = 0; i < INPUT_DIM; i++) {
				patternInput[i] = patternInputTemp[i];
			}

		}

		if (!cbLearn.isSelected()) {
			return null;
		}
		return patternInput;
	}

	@Override
	protected void writeOutput(double[] pattern) {

		if (!cbRecall.isSelected()) {

			// reset data in ALMemory
			Variant varList = new Variant();
			for (int i = 0; i < NUM_BINS; i++) {
				Variant varName = new Variant("ALSoundGeneration/mono" + i);
				Variant varValue = new Variant(0.0f);
				Variant varData = new Variant();
				varData.push_back(varName);
				varData.push_back(varValue);
				varList.push_back(varData);
			}
			memory.insertListData(varList);

			// insert output activation
			memory.insertData("ALSoundGeneration/activation", 0.0f);

		} else {

			if (pattern != null) {

				// write data to ALMemory
				Variant varList = new Variant();
				for (int i = 0; i < NUM_BINS; i++) {
					Variant varName = new Variant("ALSoundGeneration/mono" + i);
					Variant varValue = new Variant((float) patternOutput[outputSpectrumIndex * NUM_BINS + i]);
					Variant varData = new Variant();
					varData.push_back(varName);
					varData.push_back(varValue);
					varList.push_back(varData);
				}
				memory.insertListData(varList);

				// insert output activation
				memory.insertData("ALSoundGeneration/activation", (float) getActivationOutput());

			}

		}

		// increase output spectrum index
		outputSpectrumIndex++;
		if (outputSpectrumIndex == NUM_SPECTRA) {

			// reset input spectrum index
			outputSpectrumIndex = 0;

			// set output pattern
			if (pattern != null) {
				for (int i = 0; i < INPUT_DIM; i++) {
					patternOutput[i] = pattern[i];
				}
			}

		}

	}

	@Override
	protected void updateUI() {

		// topology status
		System.out.println("Topology has");
		System.out.println("  " + getTopology().getNodeSet().size() + " nodes");
		System.out.println("  " + getTopology().getEdgeSet().size() + " edges");
		System.out.println("  " + getTopology().getClusterSet().size() + " clusters");
		System.out.println();

		// input
		if (patternInput != null) {
			ppInput.getImagePanel().setImage(getBufferedImage(patternInput));
		} else {
			ppInput.getImagePanel().setImage(IMAGE_BLANK);
		}

		// output
		if (patternOutput != null) {
			ppOutput.getImagePanel().setImage(getBufferedImage(patternOutput));
			ppOutput.getLbBottom().setText("Activation " + String.format("%.2f", getActivationOutput()));
		} else {
			ppOutput.getImagePanel().setImage(IMAGE_BLANK);
			ppOutput.getLbBottom().setText("");
		}

		// clusters
		pnClusters.removeAll();
		for (Cluster cluster : getTopology().getClusterSet()) {
			if (cluster.getNodes().size() >= getActivateClusterThreshold()) {
				ImagePanel ipCluster = new ImagePanel(getBufferedImage(cluster.getMean()));
				String strClusterId = "Cluster " + cluster.getId();
				String strNodes = cluster.getNodes().size() + " Nodes";
				PatternPanel ppCluster = new PatternPanel(ipCluster, strClusterId, strNodes);
				if (cluster == getTopology().getActivatedCluster()) {
					ppCluster.setBackground(Color.RED);
				}
				pnClusters.add(ppCluster);
			}
		}

		// update panel
		panel.updateUI();

	}

	private BufferedImage getBufferedImage(double[] pattern) {
		double[] patternNorm = normalisePattern(pattern);

		// create image
		BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();

		// background
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

		for (int j = 0; j < NUM_SPECTRA; j++) {
			for (int i = 0; i < NUM_BINS; i++) {

				// limit value
				double value = patternNorm[j * NUM_BINS + i];
				if (value < 0.0)
					value = 0.0;
				else if (value > 1.0)
					value = 1.0;

				// draw bin
				int red = (int) Math.round(value * 255);
				int green = 0;
				int blue = (int) Math.round((1.0 - value) * 255);
				g.setColor(new Color(red, green, blue));
				int xPos = BIN_SPACING_X + (BIN_WIDTH + BIN_SPACING_X) * j;
				int yPos = BIN_SPACING_Y + (BIN_HEIGHT + BIN_SPACING_Y) * (NUM_BINS - i - 1);
				g.fillRect(xPos, yPos, BIN_WIDTH, BIN_HEIGHT);

			}
		}

		return image;
	}

	private double[] normalisePattern(double[] pattern) {
		return pattern;
	}

	public static void main(String[] args) {
		NAOSoundSpectrogram interactor = new NAOSoundSpectrogram();
		interactor.run();
	}

}
