package interactor.gui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;
import interactor.InteractorUDP;

public class Distribution2DRandom extends InteractorUDP {

	private static final String NAME = "Distribution2DRandom";

	private static final int INPUT_DIM = 2;

	private LinkedList<double[]> patterns;

	private double[] patternInput;

	private double[] patternOutput;

	private Random random;

	private JFrame frame;

	private JPanel panel;

	public Distribution2DRandom() {
		super(NAME, INPUT_DIM);

		// initialise
		random = new Random();

		// set interactor parameters
		setCycleTime(200);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.05);
		topology.setUseFixedThreshold(false);
		topology.setFixedThreshold(0.1);
		topology.setAgeDead(100);
		topology.setConnectNewNodes(true);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(true);
		topology.setNodeNumSignalsMinRemoval(true);
		topology.setReduceErrorInsertion(true);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.01);
		topology.setC1Param(0.1);
		topology.setClusterJoining(true);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(false);
		topology.setJoinToleranceAbsolute(0.1);
		topology.setJoiningIterationsMax(10);

		// create patterns
		patterns = new LinkedList<double[]>();
		double[] pattern1 = { 0.2, 0.2 };
		patterns.add(pattern1);
		double[] pattern2 = { 0.8, 0.3 };
		patterns.add(pattern2);
		double[] pattern3 = { 0.4, 0.8 };
		patterns.add(pattern3);

		// create frame
		frame = new JFrame(NAME);
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		// create panel
		panel = new JPanel();
		frame.add(panel);

	}

	@Override
	protected double[] readInput() {

		// determine pattern		
		int patternIndex = random.nextInt(patterns.size());
		double[] pattern = patterns.get(patternIndex);

		// duplicate pattern
		double[] patternNoisy = pattern.clone();

		// update input pattern
		patternInput = patternNoisy;

		return patternNoisy;
	}

	@Override
	protected void writeOutput(double[] pattern) {

		// update output pattern
		patternOutput = pattern;

		/*
		System.out.print("Output is");
		for (int i = 0; i < pattern.length; i++) {
			System.out.print(String.format("  %.3f", pattern[i]));
		}
		System.out.println();
		*/

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

		synchronized (panel) {

			// initialise
			Graphics g = panel.getGraphics();
			int width = panel.getWidth() - 1;
			int height = panel.getHeight() - 1;
			int xPos, yPos;
			double[] pattern;

			// clear panel
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);

			// draw nodes
			g.setColor(Color.RED);
			int diameterNode = 8;
			for (Node node : getTopology().getNodeSet()) {
				pattern = node.getPattern();
				xPos = (int) Math.round(pattern[0] * width);
				yPos = (int) Math.round(pattern[1] * height);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw edges
			g.setColor(Color.BLUE);
			int xPosA, yPosA, xPosB, yPosB;
			for (Edge edge : getTopology().getEdgeSet()) {
				pattern = edge.getNodeA().getPattern();
				xPosA = (int) Math.round(pattern[0] * width);
				yPosA = (int) Math.round(pattern[1] * height);
				pattern = edge.getNodeB().getPattern();
				xPosB = (int) Math.round(pattern[0] * width);
				yPosB = (int) Math.round(pattern[1] * height);
				g.drawLine(xPosA, yPosA, xPosB, yPosB);
			}

			// draw clusters
			g.setColor(Color.GREEN);
			int diameterCluster = 22;
			FontMetrics metrics = g.getFontMetrics();
			int textHeight = metrics.getHeight();
			int textWidth;
			for (Cluster cluster : getTopology().getClusterSet()) {
				// only clusters with more than one node
				if (cluster.getNodes().size() >= getActivateClusterThreshold()) {
					// highlight activated cluster
					if (cluster == getTopology().getActivatedCluster()) {
						diameterCluster = 36;
						g.setColor(Color.WHITE);
					} else {
						diameterCluster = 22;
						g.setColor(Color.GREEN);
					}
					pattern = cluster.getMean();
					xPos = (int) Math.round(pattern[0] * width);
					yPos = (int) Math.round(pattern[1] * height);
					g.drawOval(xPos - diameterCluster / 2, yPos - diameterCluster / 2, diameterCluster, diameterCluster);
					String strId = String.valueOf(cluster.getId());
					textWidth = metrics.stringWidth(strId);
					g.drawString(strId, xPos - textWidth / 2, yPos + textHeight / 2);
				}
			}

			// draw input
			if (patternInput != null) {
				xPos = (int) Math.round(patternInput[0] * width);
				yPos = (int) Math.round(patternInput[1] * height);
				g.setColor(Color.WHITE);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw output
			if (patternOutput != null) {
				xPos = (int) Math.round(patternOutput[0] * width);
				yPos = (int) Math.round(patternOutput[1] * height);
				g.setColor(Color.GREEN);
				g.fillRect(xPos, yPos, diameterNode, diameterNode);
			}

		}

	}

	@Override
	protected void shutdown() {
		System.exit(0);
	}

	public static void main(String[] args) {
		Distribution2DRandom distribution2DRandom = new Distribution2DRandom();
		distribution2DRandom.run();
	}

}
