package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import soinnm.Cluster;
import soinnm.SOINNM;
import util.ImagePanel;
import util.PatternPanel;
import util.WrapLayout;
import interactor.InteractorUDP;

import com.jhlabs.image.CropFilter;
import com.jhlabs.image.GaussianFilter;
import com.jhlabs.image.RotateFilter;
import com.jhlabs.image.ScaleFilter;

public class Signs extends InteractorUDP {

	private final static String NAME = "Signs";

	private final static String SIGNS_PATH = "res/Signs/";
	private final static int NUM_SIGNS = 4; // 8	
	BufferedImage[] images;

	private int signIndex;
	private boolean blankIsActive;
	private final static long TIME_BLANK = 1000;

	private static enum Order {
		RANDOM, SEQUENTIAL//, PERMUTATED, PROBABILISTIC
	};

	private Order signOrder;
	private long timePerSign;
	private long timeLastUpdate;

	private Random random;

	private String labelText;

	private final static float SCALE_FACTOR_MAX = 0.01f;
	private final static float ANGLE_MAX = 3.0f; // degrees
	private final static float BLUR_RADIUS = 1.0f; // 4.0f
	private final static GaussianFilter GAUSSIAN_FILTER = new GaussianFilter(BLUR_RADIUS);
	//private final static int IMAGE_TYPE = BufferedImage.TYPE_BYTE_GRAY;
	private final static int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
	private final static int BITS_PER_PIXEL = 3;
	private final static int IMAGE_WIDTH = 75; // 225
	private final static int IMAGE_HEIGHT = 75; // 225

	private final static int INPUT_DIM = IMAGE_WIDTH * IMAGE_HEIGHT * BITS_PER_PIXEL;
	private double[] patternInput;
	private double[] patternOutput;
	private final static long CYCLE_TIME = 200;

	private JFrame frame;
	private JPanel panel;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JPanel pnIO;
	private PatternPanel ppInput;
	private PatternPanel ppOutput;
	private JPanel pnClusters;
	private final static BufferedImage IMAGE_BLANK = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_TYPE);
	private final static boolean OUTPUT_ALPHA = false;
	private final static boolean CLUSTERS_ALPHA = false;
	private JComboBox<String> cbSignOrder;
	private JSpinner spTimePerSign;
	private JSpinner spCycleTime;
	private JLabel lbCycleDuration;
	private JLabel lbCycleTime;
	private JLabel lbNumNodes;
	private JLabel lbNumEdges;
	private JLabel lbNumClusters;

	public Signs() {
		super(NAME, INPUT_DIM);

		// initialise
		random = new Random();
		signOrder = Order.RANDOM;
		timePerSign = 4000; // ms

		// load images
		images = new BufferedImage[NUM_SIGNS];
		for (int s = 0; s < NUM_SIGNS; s++) {
			String path = SIGNS_PATH + "Sign" + (s + 1) + ".png";
			System.out.println("Reading image " + path);
			try {
				// read image
				BufferedImage imageGrey = ImageIO.read(new File(path));
				// convert to RGB					
				BufferedImage imageRGB = new BufferedImage(imageGrey.getWidth(), imageGrey.getHeight(), IMAGE_TYPE);
				imageRGB.createGraphics().drawImage(imageGrey, 0, 0, null);
				// store image
				images[s] = imageRGB;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// set interactor parameters
		setCycleTime(CYCLE_TIME);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.01); // 0.01
		topology.setUseFixedThreshold(true); // true
		topology.setFixedThreshold(0.1); // 0.1
		topology.setAgeDead(50); // 50
		topology.setConnectNewNodes(true); // true
		topology.setLambda(50); // 50
		topology.setEdgeMaxRemoval(false); // false
		topology.setNodeNumSignalsMinRemoval(false); // false
		topology.setReduceErrorInsertion(true); // true
		topology.setSmallClusterRemoval(false); // true
		topology.setC2Param(0.01); // 0.01
		topology.setC1Param(0.02); // 0.02
		topology.setClusterJoining(true); // true
		topology.setJoinTolerance(1.0); // 1.0
		topology.setUseAbsoluteJoinTolerance(true); // true
		topology.setJoinToleranceAbsolute(0.1); // 0.1
		topology.setJoiningIterationsMax(10); // 10

		// create frame
		frame = new JFrame(NAME);
		frame.setSize(600, 600);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				shutdown();
			}
		});
		frame.setLocation(0, 0);
		frame.setVisible(true);

		// create panel for parameters
		JPanel pnParameters = new JPanel();
		pnParameters.setLayout(new GridLayout(2, 2));
		pnParameters.setBorder(BorderFactory.createTitledBorder("Parameters"));
		frame.add(pnParameters, BorderLayout.PAGE_START);

		JLabel lbSignOrder = new JLabel("Sign Order:", JLabel.RIGHT);
		pnParameters.add(lbSignOrder);
		cbSignOrder = new JComboBox<String>();
		for (Order order : Order.values()) {
			cbSignOrder.addItem(order.toString());
			if (signOrder == order) {
				cbSignOrder.setSelectedIndex(cbSignOrder.getItemCount() - 1);
			}
		}
		cbSignOrder.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String item = (String) cbSignOrder.getSelectedItem();
				for (Order order : Order.values()) {
					if (item.equals(order.toString())) {
						signOrder = order;
						break;
					}
				}

			}
		});
		pnParameters.add(cbSignOrder);

		JLabel lbTimePerSign = new JLabel("Time per Sign (ms):", JLabel.RIGHT);
		pnParameters.add(lbTimePerSign);
		spTimePerSign = new JSpinner(new SpinnerNumberModel((int) timePerSign, 100, 60000, 100));
		spTimePerSign.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				timePerSign = Long.parseLong(spTimePerSign.getValue().toString());
			}
		});
		pnParameters.add(spTimePerSign);

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
		pnControls.setLayout(new BoxLayout(pnControls, BoxLayout.Y_AXIS));
		pnControls.setBorder(BorderFactory.createTitledBorder("Controls"));
		pnTop.add(pnControls);

		// create panel for interactor
		JPanel pnInteractor = new JPanel();
		pnInteractor.setLayout(new BoxLayout(pnInteractor, BoxLayout.X_AXIS));
		pnControls.add(pnInteractor);

		spCycleTime = new JSpinner(new SpinnerNumberModel((int) CYCLE_TIME, 100, 1000, 100));
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
		cbLearn.setSelected(true);
		pnLearnRecall.add(cbLearn);

		cbRecall = new JCheckBox("Recall");
		cbRecall.setSelected(true);
		cbRecall.setEnabled(false);
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

		// update
		long time = System.currentTimeMillis();

		// switch between blank and signs
		if (blankIsActive) {
			if (time - timeLastUpdate >= TIME_BLANK) {
				timeLastUpdate = time;
				blankIsActive = false;
			}
		} else {
			if (time - timeLastUpdate >= timePerSign) {
				timeLastUpdate = time;
				blankIsActive = true;

				// update sign index
				switch (signOrder) {
				case RANDOM:
					signIndex = random.nextInt(NUM_SIGNS);
					break;
				case SEQUENTIAL:
					signIndex = (signIndex + 1) % NUM_SIGNS;
					break;
				}
			}
		}

		BufferedImage image = null;

		if (blankIsActive) {

			// fetch image
			image = IMAGE_BLANK;

			// update label text
			labelText = "Blank";

		} else {

			// fetch image
			image = images[signIndex];

			// update label text
			labelText = "Sign" + (signIndex + 1);

		}

		// resize image
		BufferedImage imageResized = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, image.getType());
		Graphics g = imageResized.getGraphics();
		g.drawImage(image, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
		image = imageResized;

		// scale, rotate & crop image
		float scaleFactor = 1.0f + SCALE_FACTOR_MAX * (random.nextFloat() * 2.0f - 1.0f);
		int scaledWidth = Math.round((float) IMAGE_WIDTH * scaleFactor);
		int scaledHeight = Math.round((float) IMAGE_HEIGHT * scaleFactor);
		BufferedImage imageScaled = new BufferedImage(scaledWidth, scaledHeight, image.getType());
		ScaleFilter scaleFilter = new ScaleFilter(scaledWidth, scaledHeight);
		scaleFilter.filter(image, imageScaled);
		float angle = ANGLE_MAX * (random.nextFloat() * 2.0f - 1.0f);
		BufferedImage imageRotated = new BufferedImage(IMAGE_WIDTH * 2, IMAGE_HEIGHT * 2, image.getType());
		RotateFilter rotateFilter = new RotateFilter(angle / 180.0f * 3.14f);
		rotateFilter.filter(imageScaled, imageRotated);
		BufferedImage imageCropped = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, image.getType());
		CropFilter cropFilter = new CropFilter(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
		cropFilter.filter(imageRotated, imageCropped);
		image = imageCropped;

		// blur image
		BufferedImage imageBlurred = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, image.getType());
		GAUSSIAN_FILTER.filter(image, imageBlurred);
		image = imageBlurred;

		// create pattern
		double[] pattern = getPattern(image, IMAGE_WIDTH, IMAGE_HEIGHT);

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
			int alpha = 255;
			ppInput.getImagePanel().setImage(getBufferedImage(patternInput, IMAGE_WIDTH, IMAGE_HEIGHT, alpha));
		} else {
			ppInput.getImagePanel().setImage(IMAGE_BLANK);
		}

		// output
		if (patternOutput != null) {
			int alpha = 255;
			if (OUTPUT_ALPHA) {
				alpha = (int) (getActivationOutput() * 200) + 55;
			}
			ppOutput.getImagePanel().setImage(getBufferedImage(patternOutput, IMAGE_WIDTH, IMAGE_HEIGHT, alpha));
			ppOutput.getLbBottom().setText("Activation " + String.format("%.2f", getActivationOutput()));
		} else {
			ppOutput.getImagePanel().setImage(IMAGE_BLANK);
			ppOutput.getLbBottom().setText("");
		}

		// clusters
		pnClusters.removeAll();
		for (Cluster cluster : getTopology().getClusterSet()) {
			if (cluster.getNodes().size() >= getActivateClusterThreshold()) {
				int alpha = 255;
				if (CLUSTERS_ALPHA) {
					Double activation = getActivationMap().get(cluster);
					if (activation != null) {
						alpha = (int) Math.round(activation.doubleValue() * 200) + 55;
					}
				}
				ImagePanel ipCluster = new ImagePanel(getBufferedImage(cluster.getMean(), IMAGE_WIDTH, IMAGE_HEIGHT, alpha));
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

	private BufferedImage getBufferedImage(double[] pattern, int width, int height, int alpha) {
		BufferedImage image = new BufferedImage(width, height, IMAGE_TYPE);

		int rgba = 0;
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				switch (IMAGE_TYPE) {

				case BufferedImage.TYPE_BYTE_GRAY:

					// grey
					int grey = (int) Math.round(pattern[i] * 255);
					if (grey < 0)
						grey = 0;
					else if (grey > 255)
						grey = 255;

					// calculate RGB value
					rgba = new Color(grey, grey, grey, alpha).getRGB();

					// set pixel
					image.setRGB(x, y, rgba);

					break;

				case BufferedImage.TYPE_INT_ARGB:

					// red
					int r = (int) Math.round(pattern[i + 0] * 255);
					if (r < 0)
						r = 0;
					else if (r > 255)
						r = 255;

					// green
					int g = (int) Math.round(pattern[i + 1] * 255);
					if (g < 0)
						g = 0;
					else if (g > 255)
						g = 255;

					// blue
					int b = (int) Math.round(pattern[i + 2] * 255);
					if (b < 0)
						b = 0;
					else if (b > 255)
						b = 255;

					// calculate RGB value
					rgba = new Color(r, g, b, alpha).getRGB();

					// set pixel
					image.setRGB(x, y, rgba);

					break;

				}

				i += BITS_PER_PIXEL;
			}
		}

		return image;
	}

	private double[] getPattern(BufferedImage image, int width, int height) {
		double[] pattern = new double[width * height * BITS_PER_PIXEL];

		int rgba = 0;
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				rgba = image.getRGB(x, y);

				switch (IMAGE_TYPE) {

				case BufferedImage.TYPE_BYTE_GRAY:

					pattern[i] = (rgba & 255) / 255.0;

					break;

				case BufferedImage.TYPE_INT_ARGB:

					pattern[i + 0] = ((rgba >> 16) & 255) / 255.0;
					pattern[i + 1] = ((rgba >> 8) & 255) / 255.0;
					pattern[i + 2] = ((rgba >> 0) & 255) / 255.0;

					break;
				}

				i += BITS_PER_PIXEL;

			}
		}

		return pattern;
	}

	public String getLabelText() {
		return labelText;
	}

	public static void main(String[] args) {
		Signs interactor = new Signs();
		interactor.run();
	}

}
