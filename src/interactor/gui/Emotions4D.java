package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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

public class Emotions4D extends InteractorUDP {

	private static final String NAME = "Emotions4D";

	private static final int INPUT_DIM = 4;
	private double[] patternInput;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 200;

	private JFrame frame;
	private JPanel panel;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JPanel pnIO;
	private double[] patternInputForce;
	private PatternPanel ppInputForce;
	private PatternPanel ppInput;
	private PatternPanel ppOutput;
	private JPanel pnClusters;

	private final static int BIN_HEIGHT = 10;
	private final static int BIN_WIDTH_MAX = 40;
	private final static int BIN_SPACING_X = 10;
	private final static int BIN_SPACING_Y = 10;
	private final static int IMAGE_WIDTH = BIN_SPACING_X + BIN_WIDTH_MAX + BIN_WIDTH_MAX + BIN_SPACING_X;
	private final static int IMAGE_HEIGHT = BIN_SPACING_Y + INPUT_DIM * (BIN_HEIGHT + BIN_SPACING_Y);
	private final static BufferedImage IMAGE_BLANK = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	private final static String[] DIM_NAMES = { "Valence", "Arousal", "Potency", "Unpredictability" };
	private final static Color[] DIM_COLORS = { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW };

	public Emotions4D() {
		super(NAME, INPUT_DIM);

		// initialise
		patternInputForce = new double[INPUT_DIM];

		// set interactor parameters
		setCycleTime(CYCLE_TIME);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.1);
		topology.setUseFixedThreshold(false);
		topology.setFixedThreshold(0.01);
		topology.setAgeDead(100);
		topology.setConnectNewNodes(false);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(true);
		topology.setNodeNumSignalsMinRemoval(true);
		topology.setReduceErrorInsertion(true);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.1);
		topology.setC1Param(0.2);
		topology.setClusterJoining(true);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(false);
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
		pnIO = new JPanel();
		pnIO.setLayout(new FlowLayout(FlowLayout.LEFT));
		pnIO.setBorder(BorderFactory.createTitledBorder("Input/Output"));

		// create panel for presets
		JPanel pnPresets = new JPanel();
		pnPresets.setLayout(new BoxLayout(pnPresets, BoxLayout.Y_AXIS));

		// create buttons for presets
		JButton btHappiness = new JButton("Happiness");
		btHappiness.setAlignmentX(JButton.CENTER_ALIGNMENT);
		btHappiness.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				patternInputForce[0] = -0.8; // valence
				patternInputForce[1] = +0.1; // arousal
				patternInputForce[2] = -0.2; // potency
				patternInputForce[3] = -0.0; // unpredictability
			}
		});
		pnPresets.add(btHappiness);
		JButton btSadness = new JButton("Sadness");
		btSadness.setAlignmentX(JButton.CENTER_ALIGNMENT);
		btSadness.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				patternInputForce[0] = +0.1; // valence
				patternInputForce[1] = -0.5; // arousal
				patternInputForce[2] = +0.7; // potency
				patternInputForce[3] = -0.2; // unpredictability
			}
		});
		pnPresets.add(btSadness);
		JButton btJoy = new JButton("Joy");
		btJoy.setAlignmentX(JButton.CENTER_ALIGNMENT);
		btJoy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				patternInputForce[0] = -0.8; // valence
				patternInputForce[1] = +0.2; // arousal
				patternInputForce[2] = -0.4; // potency
				patternInputForce[3] = +0.2; // unpredictability
			}
		});
		pnPresets.add(btJoy);
		JButton btAnger = new JButton("Anger");
		btAnger.setAlignmentX(JButton.CENTER_ALIGNMENT);
		btAnger.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				patternInputForce[0] = +0.5; // valence
				patternInputForce[1] = +0.7; // arousal
				patternInputForce[2] = -0.7; // potency
				patternInputForce[3] = +0.0; // unpredictability
			}
		});
		pnPresets.add(btAnger);

		pnIO.add(pnPresets);

		// TODO: use JSlider

		// forced input pattern 
		ImagePanel ipInputForce = new ImagePanel(IMAGE_BLANK);
		ppInputForce = new PatternPanel(ipInputForce, "Forced Input", "");
		pnIO.add(ppInputForce);

		// mouse listener
		ipInputForce.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				int mouseX = e.getX();
				int mouseY = e.getY();
				int dim = mouseY / (BIN_SPACING_Y + BIN_HEIGHT);
				int mod = mouseY % (BIN_SPACING_Y + BIN_HEIGHT);
				if (dim < INPUT_DIM && mod >= BIN_SPACING_Y) {
					double value = (double) (mouseX - (BIN_SPACING_X + BIN_WIDTH_MAX)) / BIN_WIDTH_MAX;
					if (value > 1.0)
						value = 1.0;
					else if (value < -1.0)
						value = -1.0;
					patternInputForce[dim] = value;
				}

			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

		});

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
	protected double[] readInput() {

		// create pattern
		double[] pattern = new double[INPUT_DIM];
		for (int i = 0; i < INPUT_DIM; i++) {
			pattern[i] = patternInputForce[i];
		}

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

		if (cbRecall.isSelected()) {
			if (pattern != null) {
				// TODO: use as input for TOSAM (somehow)
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

		// forced input
		if (patternInputForce != null) {
			ppInputForce.getImagePanel().setImage(getBufferedImage(patternInputForce));
		} else {
			ppInputForce.getImagePanel().setImage(IMAGE_BLANK);
		}

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
				ImagePanel ipCluster = new ImagePanel(getBufferedImage(cluster.getMean(), cluster.getMin(), cluster.getMax()));
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

	@Override
	protected void shutdown() {
		System.exit(0);
	}

	private BufferedImage getBufferedImage(double[] pattern) {
		double[] patternNorm = normalisePattern(pattern);

		// create image
		BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();

		// background
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

		for (int i = 0; i < INPUT_DIM; i++) {
			int yPos = BIN_SPACING_Y + i * (BIN_HEIGHT + BIN_SPACING_Y);

			// draw bin background
			g.setColor(Color.DARK_GRAY);
			g.fillRect(BIN_SPACING_X, yPos, BIN_WIDTH_MAX + BIN_WIDTH_MAX, BIN_HEIGHT);

			// draw bin
			double value = patternNorm[i];
			g.setColor(DIM_COLORS[i]);
			int binWidth = (int) Math.round(Math.abs(value) * BIN_WIDTH_MAX);
			int xPos = BIN_SPACING_X + BIN_WIDTH_MAX;
			if (value < 0) {
				xPos -= binWidth;
			}
			g.fillRect(xPos, yPos, binWidth, BIN_HEIGHT);

			// draw text
			g.setColor(Color.WHITE);
			g.drawString(DIM_NAMES[i], BIN_SPACING_X, yPos + BIN_SPACING_Y);

		}

		return image;
	}

	private BufferedImage getBufferedImage(double[] pattern, double[] patternMin, double[] patternMax) {
		double[] patternNorm = normalisePattern(pattern);
		double[] patternNormMin = normalisePattern(patternMin);
		double[] patternNormMax = normalisePattern(patternMax);

		// create image
		BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();

		// background
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

		for (int i = 0; i < INPUT_DIM; i++) {
			int yPos = BIN_SPACING_Y + i * (BIN_HEIGHT + BIN_SPACING_Y);

			// draw bin background
			g.setColor(Color.DARK_GRAY);
			g.fillRect(BIN_SPACING_X, yPos, BIN_WIDTH_MAX + BIN_WIDTH_MAX, BIN_HEIGHT);

			// limit values
			double value = patternNorm[i];
			if (value < -1.0)
				value = -1.0;
			else if (value > 1.0)
				value = 1.0;
			double valueMin = patternNormMin[i];
			if (valueMin < -1.0)
				valueMin = -1.0;
			else if (valueMin > 1.0)
				valueMin = 1.0;
			double valueMax = patternNormMax[i];
			if (valueMax < -1.0)
				valueMax = -1.0;
			else if (valueMax > 1.0)
				valueMax = 1.0;

			// initialise
			int xStart = 0;
			int xFinish = 0;
			int red = DIM_COLORS[i].getRed();
			int green = DIM_COLORS[i].getGreen();
			int blue = DIM_COLORS[i].getBlue();
			int alpha = 255;

			// draw gradient from valueMin to value			
			xStart = BIN_SPACING_X + BIN_WIDTH_MAX + (int) Math.round(valueMin * BIN_WIDTH_MAX);
			xFinish = BIN_SPACING_X + BIN_WIDTH_MAX + (int) Math.round(value * BIN_WIDTH_MAX);
			for (int xPos = xStart; xPos <= xFinish; xPos++) {
				alpha = Math.round((float) (xPos - xStart) / (float) (xFinish - xStart) * 255);
				g.setColor(new Color(red, green, blue, alpha));
				g.drawLine(xPos, yPos, xPos, yPos + BIN_HEIGHT);
			}

			// draw gradient from value to valueMax
			xStart = BIN_SPACING_X + BIN_WIDTH_MAX + (int) Math.round(value * BIN_WIDTH_MAX);
			xFinish = BIN_SPACING_X + BIN_WIDTH_MAX + (int) Math.round(valueMax * BIN_WIDTH_MAX);
			for (int xPos = xStart; xPos <= xFinish; xPos++) {
				alpha = 255 - Math.round((float) (xPos - xStart) / (float) (xFinish - xStart) * 255);
				g.setColor(new Color(red, green, blue, alpha));
				g.drawLine(xPos, yPos, xPos, yPos + BIN_HEIGHT);
			}

			// draw text
			g.setColor(Color.WHITE);
			g.drawString(DIM_NAMES[i], BIN_SPACING_X, yPos + BIN_SPACING_Y);

		}

		return image;
	}

	private double[] normalisePattern(double[] pattern) {
		return pattern;
	}

	public static void main(String[] args) {
		Emotions4D interactor = new Emotions4D();
		interactor.run();
	}

}
