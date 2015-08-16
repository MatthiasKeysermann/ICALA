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

public class NAOTactiles extends InteractorUDP {

	private static final String NAME = "NAOTactiles";

	private String host = "localhost";
	private int port = 9559;

	private static final double VALUE_MIN = -0.5; // required for display
	private static final double VALUE_MAX = +1.5; // required for display	
	private ALMemoryProxy memory;

	private static final int INPUT_DIM = 2;
	private double[] patternInput;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 500;

	private JFrame frame;
	private JPanel panel;

	public NAOTactiles() {
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
		topology.setFixedThreshold(0.1);
		topology.setAgeDead(1000);
		topology.setConnectNewNodes(false);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(false);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(false);
		topology.setSmallClusterRemoval(false);
		topology.setC2Param(0.001);
		topology.setC1Param(0.1);
		topology.setClusterJoining(false);
		topology.setJoinTolerance(1.0);
		topology.setJoiningIterationsMax(5);

		// create frame
		frame = new JFrame(NAME);
		frame.setSize(200, 250);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				shutdown();
			}
		});
		frame.setVisible(true);

		// create panel
		panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Tactiles"));
		frame.add(panel);

	}

	@Override
	protected void shutdown() {
		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		Variant varTactileRear = memory.getData("RearTactilTouched");
		double tactileRear = (double) varTactileRear.toFloat();
		Variant varTactileFront = memory.getData("FrontTactilTouched");
		double tactileFront = (double) varTactileFront.toFloat();

		// create pattern
		double[] pattern = new double[INPUT_DIM];
		pattern[0] = tactileRear;
		pattern[1] = tactileFront;

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

		// topology status
		System.out.println("Topology has");
		System.out.println("  " + getTopology().getNodeSet().size() + " nodes");
		System.out.println("  " + getTopology().getEdgeSet().size() + " edges");
		System.out.println("  " + getTopology().getClusterSet().size() + " clusters");
		System.out.println();

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
			g.drawLine(0, height / 2, width - 1, height / 2);
			g.drawLine(width / 2, 0, width / 2, height - 1);
			String strFront = "Front";
			textWidth = metrics.stringWidth(strFront);
			g.drawString(strFront, (int) Math.round(width * 0.1), (int) Math.round(height * 0.9));
			String strRear = "Rear";
			textWidth = metrics.stringWidth(strRear);
			g.drawString(strRear, (int) Math.round(width * 0.9) - textWidth, (int) Math.round(height * 0.1));

			// draw nodes
			g.setColor(Color.RED);
			int diameterNode = 8;
			for (Node node : getTopology().getNodeSet()) {
				pattern = node.getPattern();
				patternNorm = normalisePattern(pattern);
				xPos = (int) Math.round(patternNorm[0] * width);
				yPos = (int) Math.round(patternNorm[1] * height);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw edges
			g.setColor(Color.BLUE);
			int xPosA, yPosA, xPosB, yPosB;
			for (Edge edge : getTopology().getEdgeSet()) {
				pattern = edge.getNodeA().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosA = (int) Math.round(patternNorm[0] * width);
				yPosA = (int) Math.round(patternNorm[1] * height);
				pattern = edge.getNodeB().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosB = (int) Math.round(patternNorm[0] * width);
				yPosB = (int) Math.round(patternNorm[1] * height);
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
					xPos = (int) Math.round(patternNorm[0] * width);
					yPos = (int) Math.round(patternNorm[1] * height);
					g.drawOval(xPos - diameterCluster / 2, yPos - diameterCluster / 2, diameterCluster, diameterCluster);
					String strId = String.valueOf(cluster.getId());
					textWidth = metrics.stringWidth(strId);
					g.drawString(strId, xPos - textWidth / 2, yPos + textHeight / 2);
				}
			}

			// draw input
			if (patternInput != null) {
				patternNorm = normalisePattern(patternInput);
				xPos = (int) Math.round(patternNorm[0] * width);
				yPos = (int) Math.round(patternNorm[1] * height);
				g.setColor(Color.WHITE);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw output
			if (patternOutput != null) {
				patternNorm = normalisePattern(patternOutput);
				xPos = (int) Math.round(patternNorm[0] * width);
				yPos = (int) Math.round(patternNorm[1] * height);
				g.setColor(Color.GREEN);
				g.fillRect(xPos, yPos, diameterNode, diameterNode);
			}

		}
	}

	private double[] normalisePattern(double[] pattern) {
		double[] patternNorm = new double[pattern.length];
		for (int i = 0; i < patternNorm.length; i++) {
			patternNorm[i] = (pattern[i] - VALUE_MIN) / (VALUE_MAX - VALUE_MIN);
		}
		return patternNorm;
	}

	public static void main(String[] args) {
		NAOTactiles interactor = new NAOTactiles();
		interactor.run();
	}

}
