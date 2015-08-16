package interactor.gui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;

public class NAOLaserDistance extends InteractorUDP {

	private static final String NAME = "NAOLaserDistance";

	private String host = "localhost";
	private int port = 9559;

	private ALLaserProxy laser;
	private static final int LENGTH_MIN = 20;
	private static final int LENGTH_MAX = 2600;
	private static final int DISTANCE_MIN = 0; // required for display
	private static final int DISTANCE_MAX = 3000; // required for display	
	private static final int LASER_NUM_DEGREES = 11; // opening angle in number of degrees	
	private ALMemoryProxy memory;

	private static final int INPUT_DIM = 1;
	private double[] patternInput;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 500;

	private JFrame frame;
	private JPanel panel;

	public NAOLaserDistance() {
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
		float stepSize = 0.0063f;
		float angleMax = (LASER_NUM_DEGREES - 1) / 2 * stepSize;
		laser.setOpeningAngle(new Variant(-angleMax), new Variant(+angleMax));
		memory = new ALMemoryProxy(host, port);

		// set interactor parameters
		setCycleTime(CYCLE_TIME);
		setInnerFeedback(false);
		setActivateClusterThreshold(3);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(15.0);
		topology.setUseFixedThreshold(true);
		topology.setFixedThreshold(15.0);
		topology.setAgeDead(1000);
		topology.setConnectNewNodes(false);
		topology.setLambda(25);
		topology.setEdgeMaxRemoval(false);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(false);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.001);
		topology.setC1Param(0.1);
		topology.setClusterJoining(false);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(true);
		topology.setJoinToleranceAbsolute(15.0);
		topology.setJoiningIterationsMax(5);

		// create frame
		frame = new JFrame(NAME);
		frame.setSize(250, 600);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				shutdown();
			}
		});
		frame.setVisible(true);

		// create panel
		panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Distance"));
		frame.add(panel);

	}

	@Override
	protected void shutdown() {

		// switch laser off
		laser.laserOFF();

		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// average readings		
		double distance = 0;
		int numReadings = 0;
		Variant varReadings = memory.getData("Device/Laser/Value");
		for (int i = 0; i < LASER_NUM_DEGREES; i++) {
			Variant varReading = varReadings.getElement(i).getElement(0);
			float reading = varReading.toFloat();
			// only consider valid readings
			if (reading >= LENGTH_MIN) {
				distance += (double) reading;
				numReadings += 1;
			}
		}
		if (numReadings > 0) {
			distance /= numReadings;
		} else {
			// assume maximum length
			distance = LENGTH_MAX;
		}

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
		System.out.println("distance is " + String.format("%3dcm", Math.round(patternInput[0] * 0.1)));

		synchronized (panel) {

			// initialise
			Insets insets = panel.getBorder().getBorderInsets(panel);
			int width = panel.getWidth() - insets.left - insets.right;
			int height = panel.getHeight() - insets.top - insets.bottom;
			Graphics g = panel.getGraphics().create(insets.left, insets.top, width, height);
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
			int numLabels = 30;
			for (int i = 0; i <= numLabels; i++) {
				long labelDistance = Math.round(0.1 * (DISTANCE_MIN + i * (DISTANCE_MAX - DISTANCE_MIN)) / numLabels);
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
		NAOLaserDistance interactor = new NAOLaserDistance();
		interactor.run();
	}

}
