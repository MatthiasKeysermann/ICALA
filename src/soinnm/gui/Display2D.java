package soinnm.gui;

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
import soinnm.gui.Display2D;

public class Display2D {

	private SOINNM soinnm;
	private JFrame frame;
	private JPanel panel;

	public Display2D(SOINNM soinnm) {

		// check dimension of input
		if (soinnm.getInputDim() != 2) {
			System.err.println("SOINN-M has wrong dimensionality");
			return;
		}

		// set SOINN-R
		this.soinnm = soinnm;

		// create frame
		frame = new JFrame("SOINN-M");
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		// create panel
		panel = new JPanel();
		frame.add(panel);

	}

	public void update() {
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
			for (Node node : soinnm.getNodeSet()) {
				pattern = node.getPattern();
				xPos = (int) Math.round(pattern[0] * width);
				yPos = (int) Math.round(pattern[1] * height);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw edges
			g.setColor(Color.BLUE);
			int xPosA, yPosA, xPosB, yPosB;
			for (Edge edge : soinnm.getEdgeSet()) {
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
			for (Cluster cluster : soinnm.getClusterSet()) {
				// only clusters with more than one node
				if (cluster.getNodes().size() > 1) {
					// highlight activated cluster
					if (cluster == soinnm.getActivatedCluster()) {
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
		}
	}

	public static void main(String[] args) {

		// initialise
		int inputDim = 2;
		Random random = new Random();
		double pNoisy;
		long numEpochs = 0;

		// create SOINN-M
		SOINNM soinnm = new SOINNM(inputDim);
		soinnm.setNoiseLevel(0.01);
		soinnm.setUseFixedThreshold(true);
		soinnm.setFixedThreshold(0.1);
		soinnm.setAgeDead(100);
		soinnm.setConnectNewNodes(true);
		soinnm.setLambda(50);
		soinnm.setEdgeMaxRemoval(true);
		soinnm.setNodeNumSignalsMinRemoval(true);
		soinnm.setReduceErrorInsertion(true);
		soinnm.setSmallClusterRemoval(true);
		soinnm.setC2Param(0.01);
		soinnm.setC1Param(0.1);
		soinnm.setClusterJoining(true);
		soinnm.setJoinTolerance(1.0);
		soinnm.setUseAbsoluteJoinTolerance(false);
		soinnm.setJoinToleranceAbsolute(0.1);
		soinnm.setJoiningIterationsMax(10);
		soinnm.setPrintDebugInfo(true);

		// create patterns
		LinkedList<double[]> patterns = new LinkedList<double[]>();
		double[] pattern1 = { 0.2, 0.2 };
		patterns.add(pattern1);
		double[] pattern2 = { 0.8, 0.3 };
		patterns.add(pattern2);
		double[] pattern3 = { 0.4, 0.8 };
		patterns.add(pattern3);

		// create display
		Display2D display = new Display2D(soinnm);

		// input patterns
		while (true) {

			for (double[] pattern : patterns) {

				// create noisy pattern
				double[] patternNoisy = new double[inputDim];
				for (int i = 0; i < inputDim; i++) {
					pNoisy = pattern[i] + random.nextGaussian() * 0.08;
					if (pNoisy < 0.0) {
						pNoisy = 0.0;
					} else if (pNoisy > 1.0) {
						pNoisy = 1.0;
					}
					patternNoisy[i] = pNoisy;
				}

				// input noisy pattern
				soinnm.input(patternNoisy);

				// DEBUG
				System.out.println("Nodes: " + soinnm.getNodeSet().size() + "   Edges: " + soinnm.getEdgeSet().size() + "   Clusters: " + soinnm.getClusterSet().size());

			}

			// increase number of epochs
			numEpochs += 1;

			// DEBUG
			System.out.println("Epochs: " + numEpochs);

			// update display
			display.update();

			// wait				
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

		}

	}

}
