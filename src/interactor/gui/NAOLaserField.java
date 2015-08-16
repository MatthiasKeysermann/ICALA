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

public class NAOLaserField extends InteractorUDP {

	private static final String NAME = "NAOLaserField";

	private String host = "localhost";
	private int port = 9559;

	private ALLaserProxy laser;
	private static final int LENGTH_MIN = 20;
	private static final int LENGTH_MAX = 2600;
	private static final int DISTANCE_MIN = 0; // required for display
	private static final int DISTANCE_MAX = 3000; // required for display	
	private static final int LASER_NUM_DEGREES = 251; // opening angle in number of degrees
	private static final float LASER_STEP_SIZE = 0.0063f;
	private static final float LASER_ANGLE_MAX = (LASER_NUM_DEGREES - 1) / 2 * LASER_STEP_SIZE;
	private ALMemoryProxy memory;

	private static final int INPUT_DIM = LASER_NUM_DEGREES;
	private double[] patternInput;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 500;

	private JFrame frame;
	private JPanel panel;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JPanel pnIO;
	private JPanel pnClusters;
	private PatternPanel ppInput;
	private PatternPanel ppOutput;
	private final static int IMAGE_RADIUS = 50;
	private final static BufferedImage IMAGE_BLANK = new BufferedImage(IMAGE_RADIUS * 2, IMAGE_RADIUS * 2, BufferedImage.TYPE_INT_ARGB);

	public NAOLaserField() {
		super(NAME, INPUT_DIM);

		// initialise NAO
		try {
			BufferedReader bReader = new BufferedReader(new FileReader("NAOHostPort.txt"));
			host = bReader.readLine().trim();
			port = Integer.parseInt(bReader.readLine().trim());
			bReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		laser = new ALLaserProxy(host, port);
		laser.laserON();
		laser.setDetectingLength(new Variant(LENGTH_MIN), new Variant(LENGTH_MAX));
		laser.setOpeningAngle(new Variant(-LASER_ANGLE_MAX), new Variant(+LASER_ANGLE_MAX));
		memory = new ALMemoryProxy(host, port);

		// set interactor parameters
		setCycleTime(CYCLE_TIME);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.0);
		topology.setUseFixedThreshold(false);
		topology.setFixedThreshold(10.0);
		topology.setAgeDead(1000);
		topology.setConnectNewNodes(true);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(false);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(true);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.01);
		topology.setC1Param(0.1);
		topology.setClusterJoining(true);
		topology.setJoinTolerance(2.0);
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
		cbRecall.setEnabled(false);
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
	protected void shutdown() {

		// switch laser off
		laser.laserOFF();

		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// read data		
		Variant varReadings = memory.getData("Device/Laser/Value");

		// create pattern
		double[] pattern = new double[INPUT_DIM];
		for (int i = 0; i < LASER_NUM_DEGREES; i++) {
			Variant varReading = varReadings.getElement(i).getElement(0);
			float reading = varReading.toFloat();
			// only consider valid readings
			if (reading >= LENGTH_MIN) {
				pattern[i] = reading;
			} else {
				pattern[i] = LENGTH_MAX;
			}
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

	private double[] normalisePattern(double[] pattern) {
		double[] patternNorm = new double[pattern.length];
		for (int i = 0; i < patternNorm.length; i++) {
			patternNorm[i] = (pattern[i] - DISTANCE_MIN) / (DISTANCE_MAX - DISTANCE_MIN);
		}
		return patternNorm;
	}

	private BufferedImage getBufferedImage(double[] pattern) {
		double[] patternNorm = normalisePattern(pattern);
		int imageWidth = IMAGE_RADIUS * 2;
		int imageHeight = IMAGE_RADIUS * 2;
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, imageWidth, imageHeight);
		g.setColor(Color.WHITE);
		for (int i = 0; i < patternNorm.length; i++) {
			double angleRad = -LASER_ANGLE_MAX + i * LASER_STEP_SIZE;
			double radius = patternNorm[i] * IMAGE_RADIUS;
			int x = (int) Math.round(Math.cos(angleRad) * radius);
			int y = (int) Math.round(Math.sin(angleRad) * radius);
			g.fillOval(imageWidth / 2 - y, imageHeight / 2 - x, 2, 2);
		}
		return image;
	}

	public static void main(String[] args) {
		NAOLaserField interactor = new NAOLaserField();
		interactor.run();
	}

}
