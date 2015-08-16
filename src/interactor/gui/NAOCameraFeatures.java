package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import soinnm.Cluster;
import soinnm.SOINNM;
import util.ImagePanel;
import util.PatternPanel;
import util.WrapLayout;
import features.ImageProcessorRGB;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;
import com.jhlabs.image.GaussianFilter;

public class NAOCameraFeatures extends InteractorUDP {

	private final static String NAME = "NAOCameraFeatures";

	private String host = "localhost";
	private int port = 9559;

	private ALVideoDeviceProxy video;
	private final static int RESOLUTION = 0; // 0=160x120, 1=320x240, 2=640x480
	private final static int COLOUR_SPACE = 11; // 0=Yuv, 11=RGB
	private final static int BITS_PER_PIXEL = 3; // Yuv=1, RGB=3
	private final static int FPS = 25;

	private final static float BLUR_RADIUS = 0.0f; // 4.0f
	private final static GaussianFilter GAUSSIAN_FILTER = new GaussianFilter(BLUR_RADIUS);

	private final static boolean CAMERA_CALIBRATION = true; // perform initial camera calibration
	private final static long CAMERA_CALIBRATION_TIME = 2000; // ms
	private final static boolean CAMERA_DISABLE_ADJUSTMENTS = true; // disable automatic camera adjustments 
	private final static long CAMERA_DISABLE_ADJUSTMENTS_TIME = 3000; // ms
	private final static boolean CAMERA_MANUAL_EXPOSURE_GAIN = true; // set exposure and gain manually
	private final static boolean CAMERA_MANUAL_WHITE_BALANCE = false; // set white balance manually

	private final static boolean MOVE_HEAD = true;
	private HeadMovement headMovement;

	private final static int ORIGINAL_WIDTH = 160;
	private final static int ORIGINAL_HEIGHT = 120;
	private final static int IMAGE_WIDTH = 80;
	private final static int IMAGE_HEIGHT = 60;

	private ImageProcessorRGB imageProcessorRGB;
	private final static int NUM_LEVELS = 5;
	private final static int INPUT_DIM = ImageProcessorRGB.getFeatureVectorLength(NUM_LEVELS);

	private double[] patternInput;
	private double[] patternOutput;

	private final static long CYCLE_TIME_DEFAULT = 200;

	private JFrame frame;
	private JPanel panel;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JPanel pnIO;
	private PatternPanel ppInput;
	private PatternPanel ppOutput;
	private JPanel pnClusters;
	private final static BufferedImage IMAGE_BLANK = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	private final static boolean OUTPUT_ALPHA = false;
	private final static boolean CLUSTERS_ALPHA = false;
	private JSpinner spCycleTime;
	private JLabel lbCycleDuration;
	private JLabel lbCycleTime;
	private JLabel lbNumNodes;
	private JLabel lbNumEdges;
	private JLabel lbNumClusters;

	public NAOCameraFeatures() {
		super(NAME, INPUT_DIM);

		// create image processor
		imageProcessorRGB = new ImageProcessorRGB(IMAGE_WIDTH, IMAGE_HEIGHT, NUM_LEVELS);
		System.out.println("Original input has " + (IMAGE_WIDTH * IMAGE_HEIGHT * BITS_PER_PIXEL) + " dimensions");
		System.out.println("Feature vector has " + ImageProcessorRGB.getFeatureVectorLength(NUM_LEVELS) + " dimensions");

		// set interactor parameters
		setCycleTime(CYCLE_TIME_DEFAULT);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.0); // 0.0
		topology.setUseFixedThreshold(true);
		topology.setFixedThreshold(0.1);
		topology.setAgeDead(100);
		topology.setConnectNewNodes(true);
		topology.setLambda(30);
		topology.setEdgeMaxRemoval(true);
		topology.setNodeNumSignalsMinRemoval(true);
		topology.setReduceErrorInsertion(true);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.01);
		topology.setC1Param(0.1);
		topology.setClusterJoining(true);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(true);
		topology.setJoinToleranceAbsolute(0.1);
		topology.setJoiningIterationsMax(10);

		// initialise NAO
		try {
			BufferedReader bReader = new BufferedReader(new FileReader("NAOHostPort.txt"));
			host = bReader.readLine().trim();
			port = Integer.parseInt(bReader.readLine().trim());
			bReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		video = new ALVideoDeviceProxy(host, port);
		video.setParam(18, 0); // camera: 0=front, 1=bottom
		video.subscribe(NAME, RESOLUTION, COLOUR_SPACE, FPS);

		// initial adjustment
		if (CAMERA_CALIBRATION) {
			System.out.println("Performing camera calibration...");
			video.setParam(11, 1); // auto exposition (0-1)
			video.setParam(12, 1); // auto white balance (0-1)
			video.setParam(22, 1); // auto exposure correction algorithm (0-1)
			try {
				video.setParam(13, 1); // auto gain (0-1)
				video.setParam(21, 1); // exposure correction (0-1)
				video.setParam(26, 1); // auto balance (0-1)
				video.setParam(27, 128); // auto balance target (0-255)
				video.setParam(28, 128); // auto balance stable range (0-255)				
			} catch (Exception e) {
				System.out.println("Laser head camera model detected!");
			}
			// wait
			try {
				Thread.sleep(CAMERA_CALIBRATION_TIME);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		// disable automatic adjustments
		if (CAMERA_DISABLE_ADJUSTMENTS) {
			System.out.println("Disabling automatic camera adjustments...");
			video.setParam(11, 0); // auto exposition (0-1)
			video.setParam(12, 0); // auto white balance (0-1)
			video.setParam(22, 0); // auto exposure correction algorithm (0-1)
			try {
				video.setParam(13, 0); // auto gain (0-1)
				video.setParam(21, 0); // exposure correction (0-1)
				video.setParam(26, 0); // auto balance (0-1)
			} catch (Exception e) {
				System.out.println("Laser head camera model detected!");
			}
			// wait
			try {
				Thread.sleep(CAMERA_DISABLE_ADJUSTMENTS_TIME);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// set exposure and gain manually
		if (CAMERA_MANUAL_EXPOSURE_GAIN) {
			System.out.println("Setting exposure and gain parameters...");
			try {
				video.getParam(13); // cause exception for laser head model
				video.setParam(17, 512); // set exposure (0-4096), 512
				video.setParam(6, 32); // set gain (0-255), 32
			} catch (Exception e) {
				System.out.println("Laser head camera model detected!");
				video.setParam(17, 96); // set exposure (0-512), 96
				video.setParam(6, 48); // set gain (0-255), 48
			}
		}

		// set white balance manually
		if (CAMERA_MANUAL_WHITE_BALANCE) {
			System.out.println("Setting white balance parameters...");
			try {
				video.setParam(4, 80); // red chroma (0-255), 80
				video.setParam(5, 160); // blue chroma (0-255), 160
				video.setParam(25, 64); // auto white balance green gain (0-255), 64		
				video.setParam(29, 96); // balance blue (0-255), 96
				video.setParam(30, 96); // balance red (0-255), 96
				video.setParam(31, 96); // balance gain blue (0-255), 96
				video.setParam(32, 96); // balance gain red (0-255), 96
			} catch (Exception e) {
				System.out.println("Laser head camera model detected!");
			}
		}

		// start head movement
		if (MOVE_HEAD) {
			System.out.println("Starting head movement...");
			headMovement = new HeadMovement();
			headMovement.start();
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
		frame.setLocation(0, 0);
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

		// pause interactor
		pause();

		// stop head movement
		if (headMovement != null) {
			System.out.print("Stopping head movement...");
			headMovement.finish();
			while (!headMovement.isFinished()) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.print(".");
			}
			System.out.println("finished");
		}

		// unsubscribe
		synchronized (video) {
			video.unsubscribe(NAME);
		}

		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// read image
		byte[] imageBinary = null;
		synchronized (video) {
			Variant varImage = video.getImageRemote(NAME);
			Variant varImageElement = varImage.getElement(6);
			imageBinary = varImageElement.toBinary();
		}

		// create pattern
		double[] pattern = new double[ORIGINAL_WIDTH * ORIGINAL_HEIGHT * BITS_PER_PIXEL];
		for (int i = 0; i < pattern.length; i++) {
			int unsignedByte = imageBinary[i] & 0xFF;
			pattern[i] = unsignedByte / 255.0;
		}

		// create image
		BufferedImage image = getBufferedImage(pattern, ORIGINAL_WIDTH, ORIGINAL_HEIGHT, 255);

		// resize image
		BufferedImage imageResized = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, image.getType());
		Graphics g = imageResized.getGraphics();
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
		image = imageResized;

		// blur image
		BufferedImage imageBlurred = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, image.getType());
		GAUSSIAN_FILTER.filter(image, imageBlurred);
		image = imageBlurred;

		// process image
		pattern = imageProcessorRGB.processImageRGB(image);

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
			BufferedImage image = imageProcessorRGB.reconstructImageRGB(patternInput);
			ppInput.getImagePanel().setImage(image);
		} else {
			ppInput.getImagePanel().setImage(IMAGE_BLANK);
		}

		// output
		if (patternOutput != null) {
			BufferedImage image = imageProcessorRGB.reconstructImageRGB(patternOutput);
			int alpha = 255;
			if (OUTPUT_ALPHA) {
				alpha = (int) (getActivationOutput() * 200) + 55;
				for (int y = 0; y < IMAGE_HEIGHT; y++) {
					for (int x = 0; x < IMAGE_HEIGHT; x++) {
						int rgb = image.getRGB(x, y);
						Color color = new Color(rgb);
						Color colorAlpha = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
						image.setRGB(x, y, colorAlpha.getRGB());
					}
				}
			}
			ppOutput.getImagePanel().setImage(image);
			ppOutput.getLbBottom().setText("Activation " + String.format("%.2f", getActivationOutput()));
		} else {
			ppOutput.getImagePanel().setImage(IMAGE_BLANK);
			ppOutput.getLbBottom().setText("");
		}

		// clusters
		pnClusters.removeAll();
		for (Cluster cluster : getTopology().getClusterSet()) {
			if (cluster.getNodes().size() >= getActivateClusterThreshold()) {
				BufferedImage image = imageProcessorRGB.reconstructImageRGB(cluster.getMean());
				int alpha = 255;
				if (CLUSTERS_ALPHA) {
					Double activation = getActivationMap().get(cluster);
					if (activation != null) {
						alpha = (int) Math.round(activation.doubleValue() * 200) + 55;
						for (int y = 0; y < IMAGE_HEIGHT; y++) {
							for (int x = 0; x < IMAGE_HEIGHT; x++) {
								int rgb = image.getRGB(x, y);
								Color color = new Color(rgb);
								Color colorAlpha = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
								image.setRGB(x, y, colorAlpha.getRGB());
							}
						}
					}
				}
				ImagePanel ipCluster = new ImagePanel(image);
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
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		int rgba = 0;
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				switch (COLOUR_SPACE) {

				// Yuv			
				case 0:

					// grey
					int grey = (int) Math.round(pattern[i] * 255);
					if (grey < 0)
						grey = 0;
					else if (grey > 255)
						grey = 255;

					// calculate RGB value
					rgba = new Color(grey, grey, grey, alpha).getRGB();

					break;

				// RGB
				case 11:

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

					break;

				}

				// set pixel
				image.setRGB(x, y, rgba);

				i += BITS_PER_PIXEL;
			}
		}

		return image;
	}

	private class HeadMovement extends Thread {

		private boolean running;

		private boolean finished;

		private Random random;

		private ALMotionProxy motion;

		private String[] names = { "HeadYaw", "HeadPitch" };

		private float[] angles = { 0.0f, 0.0f };

		private float[] anglesMin = { -0.1f, -0.1f }; // radians, in degrees: -5.7, +5.7

		private float[] anglesMax = { +0.1f, +0.1f }; // radians, in degrees: -5.7, +5.7

		private float time = 1.0f;

		private float[] times = { time, time };

		@Override
		public void run() {

			// initialise
			random = new Random();
			motion = new ALMotionProxy(host, port);
			running = true;
			finished = false;

			while (running) {

				// set stiffnesses
				for (int i = 0; i < angles.length; i++) {
					motion.setStiffnesses(new Variant(names[i]), new Variant(1.0f));
				}

				// set angles
				for (int i = 0; i < angles.length; i++) {
					angles[i] = (float) random.nextGaussian() * anglesMax[i];
					if (angles[i] < anglesMin[i]) {
						angles[i] = anglesMin[i];
					} else if (angles[i] > anglesMax[i]) {
						angles[i] = anglesMax[i];
					}
				}

				// move head
				motion.angleInterpolation(new Variant(names), new Variant(angles), new Variant(times), true);

				// wait
				try {
					Thread.sleep(Math.round(500 + time * 100));
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			// reset angles
			for (int i = 0; i < angles.length; i++) {
				angles[i] = 0.0f;
			}

			// move head
			motion.angleInterpolation(new Variant(names), new Variant(angles), new Variant(times), true);

			// wait
			try {
				Thread.sleep(Math.round(time * 1000));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// release stiffnesses
			for (int i = 0; i < angles.length; i++) {
				motion.setStiffnesses(new Variant(names[i]), new Variant(0.0f));
			}

			// update status
			finished = true;
		}

		public void finish() {
			running = false;
		}

		public boolean isFinished() {
			return finished;
		}

	}

	public static void main(String[] args) {
		NAOCameraFeatures interactor = new NAOCameraFeatures();
		interactor.run();
	}

}
