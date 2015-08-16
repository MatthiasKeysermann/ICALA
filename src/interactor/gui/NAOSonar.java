package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;

public class NAOSonar extends InteractorUDP {

	private static final String NAME = "NAOSonar";

	private String host = "localhost";
	private int port = 9559;

	private ALSonarProxy sonar;
	//private static final double DISTANCE_MIN = 0.25; // required for display
	//private static final double DISTANCE_MAX = 2.55; // required for display	
	private static final double DISTANCE_MIN = 0.2; // required for display
	private static final double DISTANCE_MAX = 2.6; // required for display	
	private static final int sonarNumSamples = 1; // number of samples taken from sonar
	private static final long sonarInterval = 25; // sampling interval (ms)
	private ALMemoryProxy memory;

	private static final int INPUT_DIM = 1;
	private double[] patternInput;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 400;

	private JFrame frame;
	private JPanel panel;
	private JPanel pnControls;
	private JFileChooser fcSaveLoad;
	private JPanel pnTopology;

	public NAOSonar() {
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
		sonar = new ALSonarProxy(host, port);
		sonar.subscribe(NAME);
		memory = new ALMemoryProxy(host, port);

		// set interactor parameters
		setCycleTime(CYCLE_TIME);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.0);
		topology.setUseFixedThreshold(true);
		topology.setFixedThreshold(0.05);
		topology.setAgeDead(1000);
		topology.setConnectNewNodes(false);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(true);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(false);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.001);
		topology.setC1Param(0.1);
		topology.setClusterJoining(true);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(false);
		topology.setJoinToleranceAbsolute(0.1);
		topology.setJoiningIterationsMax(5);

		// create frame
		frame = new JFrame(NAME);
		frame.setSize(450, 500);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				shutdown();
			}
		});
		frame.setVisible(true);

		// create panel
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		frame.add(panel);

		// create panel for controls
		pnControls = new JPanel();
		pnControls.setBorder(BorderFactory.createTitledBorder("Controls"));

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

		panel.add(pnControls, BorderLayout.PAGE_START);

		// create panel for topology
		pnTopology = new JPanel();
		pnTopology.setBorder(BorderFactory.createTitledBorder("Topology"));
		panel.add(pnTopology, BorderLayout.CENTER);

		// update panel		
		panel.updateUI();
	}

	@Override
	protected void shutdown() {

		// unsubscribe
		sonar.unsubscribe(NAME);

		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// supersampling
		double distanceLeft = 0;
		double distanceRight = 0;
		for (int i = 0; i < sonarNumSamples; i++) {

			// read distances		
			Variant varDistanceLeft = memory.getData("Device/SubDeviceList/US/Left/Sensor/Value");
			Variant varDistanceRight = memory.getData("Device/SubDeviceList/US/Right/Sensor/Value");
			distanceLeft += (double) varDistanceLeft.toFloat();
			distanceRight += (double) varDistanceRight.toFloat();

			// DEBUG
			//System.out.println(String.format("%.3f     %.3f", varDistanceLeft.toFloat(), varDistanceRight.toFloat()));

			try {
				Thread.sleep(sonarInterval);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		distanceLeft /= sonarNumSamples;
		distanceRight /= sonarNumSamples;

		// take minimum from left and right
		double distance = Math.min(distanceLeft, distanceRight);

		// create pattern
		double[] pattern = new double[INPUT_DIM];
		pattern[0] = distance;

		// set input pattern
		patternInput = pattern;

		return pattern;
	}

	@Override
	protected void writeOutput(double[] pattern) {

		// set output pattern
		patternOutput = pattern;

	}

	@Override
	protected void updateUI() {

		// current distance reading
		System.out.println("distance is " + String.format("%3dcm", Math.round(patternInput[0] * 100)));

		synchronized (pnTopology) {

			// initialise
			Insets insets = pnTopology.getBorder().getBorderInsets(pnTopology);
			int width = pnTopology.getWidth() - insets.left - insets.right;
			int height = pnTopology.getHeight() - insets.top - insets.bottom;
			Graphics g = pnTopology.getGraphics().create(insets.left, insets.top, width, height);
			FontMetrics metrics = g.getFontMetrics();
			int textHeight = metrics.getHeight();
			int textWidth;
			int xPos, yPos;
			double[] pattern;
			double[] patternNorm;

			// clear panel
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);

			// draw grid and labels
			g.setColor(Color.DARK_GRAY);
			g.drawLine(width / 2, 0, width / 2, height - 1);
			int numLabels = 24;
			for (int i = 0; i <= numLabels; i++) {
				long labelDistance = Math.round(100 * (DISTANCE_MIN + i * (DISTANCE_MAX - DISTANCE_MIN) / numLabels));
				String strDistance = String.valueOf(labelDistance);
				textWidth = metrics.stringWidth(strDistance);
				yPos = (int) Math.round((double) i / numLabels * height);
				int xPos1 = (int) Math.round(0.4 * width);
				int xPos2 = (int) Math.round(0.6 * width);
				g.drawLine(xPos1, yPos, xPos2, yPos);
				xPos = (int) Math.round(0.7 * width);
				g.drawString(strDistance, xPos - textWidth / 2, yPos + textHeight / 2);
			}

			// draw nodes
			g.setColor(Color.RED);
			int diameterNode = 8;
			for (Node node : getTopology().getNodeSet()) {
				pattern = node.getPattern();
				patternNorm = normalisePattern(pattern);
				xPos = (int) Math.round(0.5 * width);
				yPos = (int) Math.round(patternNorm[0] * height);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw edges
			g.setColor(Color.BLUE);
			int xPosA, yPosA, xPosB, yPosB;
			for (Edge edge : getTopology().getEdgeSet()) {
				pattern = edge.getNodeA().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosA = (int) Math.round(0.5 * width);
				yPosA = (int) Math.round(patternNorm[0] * height);
				pattern = edge.getNodeB().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosB = (int) Math.round(0.5 * width);
				yPosB = (int) Math.round(patternNorm[0] * height);
				g.drawLine(xPosA, yPosA, xPosB, yPosB);
			}

			// draw clusters
			g.setColor(Color.GREEN);
			int diameterCluster = 22;
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
					xPos = (int) Math.round(0.5 * width);
					yPos = (int) Math.round(patternNorm[0] * height);
					g.drawOval(xPos - diameterCluster / 2, yPos - diameterCluster / 2, diameterCluster, diameterCluster);
					String strId = String.valueOf(cluster.getId());
					textWidth = metrics.stringWidth(strId);
					g.drawString(strId, xPos - textWidth / 2, yPos + textHeight / 2);
				}
			}

			// draw input
			if (patternInput != null) {
				patternNorm = normalisePattern(patternInput);
				xPos = (int) Math.round(0.5 * width);
				yPos = (int) Math.round(patternNorm[0] * height);
				g.setColor(Color.WHITE);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw output
			if (patternOutput != null) {
				patternNorm = normalisePattern(patternOutput);
				xPos = (int) Math.round(0.5 * width);
				yPos = (int) Math.round(patternNorm[0] * height);
				g.setColor(Color.GREEN);
				g.fillRect(xPos, yPos, diameterNode, diameterNode);
			}

		}
	}

	private double[] normalisePattern(double[] pattern) {
		double[] patternNorm = new double[pattern.length];
		for (int i = 0; i < patternNorm.length; i++) {
			patternNorm[i] = (pattern[i] - DISTANCE_MIN) / (DISTANCE_MAX - DISTANCE_MIN);
		}
		return patternNorm;
	}

	public static void main(String[] args) {
		NAOSonar interactor = new NAOSonar();
		interactor.run();
	}

}
