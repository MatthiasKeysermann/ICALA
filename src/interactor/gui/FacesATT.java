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

public class FacesATT extends InteractorUDP {

	private final static String NAME = "FacesATT";

	private final static String FACES_PATH = "res/ATTFaces/";
	private final static int NUM_PERSONS_MAX = 40; // 40
	private final static int NUM_FACES_MAX = 10; // 10
	BufferedImage[][] images;

	private int personIndex;
	private int faceIndex;
	private boolean updatePersonIndex;
	private boolean updateFaceIndex;

	public static enum Order {
		RANDOM, SEQUENTIAL
		//, PERMUTATED, PROBABILISTIC
	};

	private int numPersons;
	private int numFaces;
	private Order personOrder;
	private Order faceOrder;
	private long timePerPerson;
	private long timePerFace;
	private long timeLastUpdatePerson;
	private long timeLastUpdateFace;

	private Random random;

	private String labelText;
	public final static String PREFIX_PERSON = "Person";

	private final static float BLUR_RADIUS = 0.0f;
	private final static GaussianFilter GAUSSIAN_FILTER = new GaussianFilter(BLUR_RADIUS);

	private final static int IMAGE_TYPE = BufferedImage.TYPE_BYTE_GRAY;
	//private final static int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
	private final static int BITS_PER_PIXEL = 1;
	private final static int IMAGE_WIDTH = 92;
	private final static int IMAGE_HEIGHT = 112;
	private final static int INPUT_DIM = IMAGE_WIDTH * IMAGE_HEIGHT * BITS_PER_PIXEL;

	private boolean transformImage = false;
	private float scaleFactorMax = 0.01f; // 0.01f
	private int rotateAngleMax = 1; // 1
	private int translateMax = 10; // 10

	private double[] patternInput;
	private double[] patternOutput;
	private boolean ignoreEmptyInput = false;

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
	private final static BufferedImage IMAGE_BLANK = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_TYPE);
	private final static boolean OUTPUT_ALPHA = false;
	private final static boolean CLUSTERS_ALPHA = false;
	private JComboBox<String> cbPersonOrder;
	private JComboBox<String> cbFaceOrder;
	private JSpinner spTimePerPerson;
	private JSpinner spTimePerFace;
	private JSpinner spCycleTime;
	private JLabel lbCycleDuration;
	private JLabel lbCycleTime;
	private JLabel lbNumNodes;
	private JLabel lbNumEdges;
	private JLabel lbNumClusters;

	public FacesATT() {
		super(NAME, INPUT_DIM);

		// initialise
		random = new Random();
		numPersons = 40;
		numFaces = 10;
		personOrder = Order.SEQUENTIAL;
		faceOrder = Order.SEQUENTIAL;
		timePerPerson = 2000; // ms
		timePerFace = 200; // ms
		timeLastUpdatePerson = 0;
		timeLastUpdateFace = 0;
		personIndex = -1;
		faceIndex = -1;
		updatePersonIndex = true;
		updateFaceIndex = true;

		// load images
		images = new BufferedImage[NUM_PERSONS_MAX][NUM_FACES_MAX];
		for (int p = 0; p < NUM_PERSONS_MAX; p++) {
			for (int f = 0; f < NUM_FACES_MAX; f++) {
				String path = FACES_PATH + "s" + (p + 1) + "/" + (f + 1) + ".png";
				System.out.println("Reading image " + path);
				try {
					// read image
					BufferedImage imageOriginal = ImageIO.read(new File(path));
					// convert to image type					
					BufferedImage image = new BufferedImage(imageOriginal.getWidth(), imageOriginal.getHeight(), IMAGE_TYPE);
					image.createGraphics().drawImage(imageOriginal, 0, 0, null);
					// store image
					images[p][f] = image;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// set interactor parameters
		setCycleTime(CYCLE_TIME_DEFAULT);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.01); // 0.0
		topology.setUseFixedThreshold(true); // true
		topology.setFixedThreshold(0.1); // 0.1
		topology.setAgeDead(1000); // 1000
		topology.setConnectNewNodes(true); // true
		topology.setLambda(30); // 30
		topology.setEdgeMaxRemoval(false); // false
		topology.setNodeNumSignalsMinRemoval(false); // false
		topology.setReduceErrorInsertion(true); // true
		topology.setSmallClusterRemoval(true); // true
		topology.setC2Param(0.02); // 0.02
		topology.setC1Param(0.01); // 0.01
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
		pnParameters.setLayout(new GridLayout(2, 4));
		pnParameters.setBorder(BorderFactory.createTitledBorder("Parameters"));
		frame.add(pnParameters, BorderLayout.PAGE_START);

		JLabel lbPersonOrder = new JLabel("Person Order:", JLabel.RIGHT);
		pnParameters.add(lbPersonOrder);
		cbPersonOrder = new JComboBox<String>();
		for (Order order : Order.values()) {
			cbPersonOrder.addItem(order.toString());
			if (personOrder == order) {
				cbPersonOrder.setSelectedIndex(cbPersonOrder.getItemCount() - 1);
			}
		}
		cbPersonOrder.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String item = (String) cbPersonOrder.getSelectedItem();
				for (Order order : Order.values()) {
					if (item.equals(order.toString())) {
						personOrder = order;
						break;
					}
				}

			}
		});
		pnParameters.add(cbPersonOrder);

		JLabel lbFaceOrder = new JLabel("Face Order:", JLabel.RIGHT);
		pnParameters.add(lbFaceOrder);
		cbFaceOrder = new JComboBox<String>();
		for (Order order : Order.values()) {
			cbFaceOrder.addItem(order.toString());
			if (faceOrder == order) {
				cbFaceOrder.setSelectedIndex(cbFaceOrder.getItemCount() - 1);
			}
		}
		cbFaceOrder.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String item = (String) cbFaceOrder.getSelectedItem();
				for (Order order : Order.values()) {
					if (item.equals(order.toString())) {
						faceOrder = order;
						break;
					}
				}
			}
		});
		pnParameters.add(cbFaceOrder);

		JLabel lbTimePerPerson = new JLabel("Time per Person (ms):", JLabel.RIGHT);
		pnParameters.add(lbTimePerPerson);
		spTimePerPerson = new JSpinner(new SpinnerNumberModel((int) timePerPerson, 100, 60000, 100));
		spTimePerPerson.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				timePerPerson = Long.parseLong(spTimePerPerson.getValue().toString());
			}
		});
		pnParameters.add(spTimePerPerson);

		JLabel lbTimePerFace = new JLabel("Time per Face (ms):", JLabel.RIGHT);
		pnParameters.add(lbTimePerFace);
		spTimePerFace = new JSpinner(new SpinnerNumberModel((int) timePerFace, 100, 60000, 100));
		spTimePerFace.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				timePerFace = Long.parseLong(spTimePerFace.getValue().toString());
			}
		});
		pnParameters.add(spTimePerFace);

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
		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// update indeces
		long time = System.currentTimeMillis();

		// update person index
		if (updatePersonIndex) {
			if (time - timeLastUpdatePerson >= timePerPerson) {
				timeLastUpdatePerson = time;
				switch (personOrder) {
				case RANDOM:
					personIndex = random.nextInt(numPersons);
					break;
				case SEQUENTIAL:
					personIndex = (personIndex + 1) % numPersons;
					break;
				}
			}
		}

		// update face index
		if (updateFaceIndex) {
			if (time - timeLastUpdateFace >= timePerFace) {
				timeLastUpdateFace = time;
				switch (faceOrder) {
				case RANDOM:
					faceIndex = random.nextInt(numFaces);
					break;
				case SEQUENTIAL:
					faceIndex = (faceIndex + 1) % numFaces;
					break;
				}
			}
		}

		// update label text
		labelText = "";
		if (personIndex > -1 && faceIndex > -1) {
			labelText = PREFIX_PERSON + (personIndex + 1);
		}

		// fetch image
		BufferedImage image = IMAGE_BLANK;
		if (personIndex > -1 && faceIndex > -1) {
			image = images[personIndex][faceIndex];
		}

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

		// transform image
		if (transformImage) {

			// scale image
			float scaleFactor = random.nextFloat() * scaleFactorMax * 2 - scaleFactorMax;
			int scaledWidth = image.getWidth() + (int) ((float) image.getWidth() * scaleFactor);
			int scaledHeight = image.getHeight() + (int) ((float) image.getHeight() * scaleFactor);
			BufferedImage imageScaled = new BufferedImage(scaledWidth, scaledHeight, image.getType());
			Graphics gScaled = imageScaled.getGraphics();
			gScaled.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
			image = imageScaled;

			// rotate image
			float angleDegrees = (float) (random.nextInt(rotateAngleMax * 2) - rotateAngleMax);
			float angleRadians = angleDegrees * (float) Math.PI / 180.0f;
			float angleCos = (float) Math.cos(Math.abs(angleRadians));
			float angleSin = (float) Math.sin(Math.abs(angleRadians));
			int imageWidth = image.getWidth();
			int imageHeight = image.getHeight();
			int rotatedWidth = (int) Math.ceil(angleCos * imageWidth + angleSin * imageHeight);
			int rotatedHeight = (int) Math.ceil(angleSin * imageWidth + angleCos * imageHeight);
			BufferedImage imageRotated = new BufferedImage(rotatedWidth, rotatedHeight, image.getType());
			RotateFilter rotateFilter = new RotateFilter(angleRadians);
			rotateFilter.filter(image, imageRotated);
			image = imageRotated;

			// translate image
			int translateX = 0;
			int translateY = 0;
			if (translateMax > 0) {
				translateX = random.nextInt(translateMax * 2) - translateMax;
				translateY = random.nextInt(translateMax * 2) - translateMax;
			}
			BufferedImage imageTranslated = new BufferedImage(image.getWidth() + translateMax, image.getHeight() + translateMax, image.getType());
			Graphics gTranslate = imageTranslated.getGraphics();
			gTranslate.drawImage(image, translateMax / 2 + translateX, translateMax / 2 + translateY, image.getWidth(), image.getHeight(), null);
			image = imageTranslated;

			// crop image
			BufferedImage imageCropped = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, image.getType());
			int cropX = (image.getWidth() - IMAGE_WIDTH) / 2;
			int cropY = (image.getHeight() - IMAGE_HEIGHT) / 2;
			CropFilter cropFilter = new CropFilter(cropX, cropY, IMAGE_WIDTH, IMAGE_HEIGHT);
			cropFilter.filter(image, imageCropped);
			image = imageCropped;

		}

		// create pattern
		double[] pattern = getPattern(image, IMAGE_WIDTH, IMAGE_HEIGHT);

		// set input pattern
		patternInput = pattern;

		// ignore empty inputs
		if (ignoreEmptyInput) {
			if (personIndex == -1 || faceIndex == -1) {
				return null;
			}
		}

		if (!cbLearn.isSelected()) {
			return null;
		}
		return pattern;
	}

	@Override
	protected String readLabel() {
		return labelText;
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

	public int getNumPersons() {
		return numPersons;
	}

	public void setNumPersons(int numPersons) {
		this.numPersons = numPersons;
	}

	public int getNumFaces() {
		return numFaces;
	}

	public void setNumFaces(int numFaces) {
		this.numFaces = numFaces;
	}

	public Order getPersonOrder() {
		return personOrder;
	}

	public void setPersonOrder(Order personOrder) {
		this.personOrder = personOrder;
		cbPersonOrder.setSelectedItem(personOrder);
	}

	public Order getFaceOrder() {
		return faceOrder;
	}

	public void setFaceOrder(Order faceOrder) {
		this.faceOrder = faceOrder;
		cbFaceOrder.setSelectedItem(faceOrder.toString());
	}

	public long getTimePerPerson() {
		return timePerPerson;
	}

	public void setTimePerPerson(long timePerPerson) {
		this.timePerPerson = timePerPerson;
		spTimePerPerson.setValue(timePerPerson);
	}

	public long getTimePerFace() {
		return timePerFace;
	}

	public void setTimePerFace(long timePerFace) {
		this.timePerFace = timePerFace;
		spTimePerFace.setValue(timePerFace);
	}

	public int getPersonIndex() {
		return personIndex;
	}

	public void setPersonIndex(int personIndex) {
		this.personIndex = personIndex;
	}

	public int getFaceIndex() {
		return faceIndex;
	}

	public void setFaceIndex(int faceIndex) {
		this.faceIndex = faceIndex;
	}

	public boolean isUpdatePersonIndex() {
		return updatePersonIndex;
	}

	public void setUpdatePersonIndex(boolean updatePersonIndex) {
		this.updatePersonIndex = updatePersonIndex;
	}

	public boolean isUpdateFaceIndex() {
		return updateFaceIndex;
	}

	public void setUpdateFaceIndex(boolean updateFaceIndex) {
		this.updateFaceIndex = updateFaceIndex;
	}

	public void setLearning(boolean learning) {
		cbLearn.setSelected(learning);
	}

	public void setRecalling(boolean recalling) {
		cbRecall.setSelected(recalling);
	}

	public static float getBlurRadius() {
		return BLUR_RADIUS;
	}

	public static int getImageWidth() {
		return IMAGE_WIDTH;
	}

	public static int getImageHeight() {
		return IMAGE_HEIGHT;
	}

	public BufferedImage getClusterPanelImage() {
		BufferedImage image = new BufferedImage(pnClusters.getWidth(), pnClusters.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		pnClusters.paint(g);
		return image;
	}

	public boolean isTransformImage() {
		return transformImage;
	}

	public void setTransformImage(boolean transformImage) {
		this.transformImage = transformImage;
	}

	public float getScaleFactorMax() {
		return scaleFactorMax;
	}

	public void setScaleFactorMax(float scaleFactorMax) {
		this.scaleFactorMax = scaleFactorMax;
	}

	public int getRotateAngleMax() {
		return rotateAngleMax;
	}

	public void setRotateAngleMax(int rotateAngleMax) {
		this.rotateAngleMax = rotateAngleMax;
	}

	public int getTranslateMax() {
		return translateMax;
	}

	public void setTranslateMax(int translateMax) {
		this.translateMax = translateMax;
	}

	public boolean isIgnoreEmptyInput() {
		return ignoreEmptyInput;
	}

	public void setIgnoreEmptyInput(boolean ignoreEmptyInput) {
		this.ignoreEmptyInput = ignoreEmptyInput;
	}

	public static void main(String[] args) {
		FacesATT interactor = new FacesATT();
		interactor.run();
	}

}
