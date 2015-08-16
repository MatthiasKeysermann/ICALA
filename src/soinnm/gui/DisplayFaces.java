package soinnm.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import soinnm.Cluster;
import soinnm.SOINNM;
import soinnm.gui.DisplayFaces;

public class DisplayFaces {

	private SOINNM soinnm;
	private JFrame frame;
	private JPanel panelClusters;

	public DisplayFaces(SOINNM soinnm) {

		// set SOINN-M
		this.soinnm = soinnm;

		// create frame
		frame = new JFrame("SOINN-M");
		frame.setSize(1050, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(10, 10);
		frame.setVisible(true);

		// create panel for clusters
		panelClusters = new JPanel();
		panelClusters.setBorder(BorderFactory.createTitledBorder("Clusters"));
		frame.add(panelClusters);

	}

	public void update() {
		synchronized (panelClusters) {

			// initialise
			Insets insets = panelClusters.getBorder().getBorderInsets(panelClusters);
			int offsetX = panelClusters.getX() + insets.left;
			int offsetY = panelClusters.getY() + insets.top;
			int width = panelClusters.getWidth() - insets.left - insets.right;
			int height = panelClusters.getHeight() - insets.top - insets.bottom;
			Graphics g = panelClusters.getGraphics().create(offsetX, offsetY, width, height);

			// clear panel			
			g.setColor(panelClusters.getBackground());
			g.fillRect(0, 0, width, height);

			// initialise cursor
			int xPos = 10;
			int yPos = 10;

			// draw clusters
			for (Cluster cluster : soinnm.getClusterSet()) {
				if (cluster.getNodes().size() > 1) {

					BufferedImage image = new BufferedImage(92, 112, BufferedImage.TYPE_INT_ARGB);
					double mean[] = cluster.getMean();
					int i = 0;
					for (int y = 0; y < 112; y++) {
						for (int x = 0; x < 92; x++) {
							int grey = (int) Math.round(mean[i++] * 255);
							int rgba = new Color(grey, grey, grey, 255).getRGB();
							image.setRGB(x, y, rgba);
						}
					}
					g.drawImage(image, xPos, yPos, null);

					String text = "Cluster" + cluster.getId();
					g.setColor(Color.BLACK);
					g.drawChars(text.toCharArray(), 0, text.length(), xPos, yPos + 112 + 10);

					xPos += 92 + 10;
					if (xPos > width - 92 - 10) {
						xPos = 10;
						yPos += 112 + 10 + 10;
					}

				}

			}

		}
	}

	public static void main(String[] args) {

		// parameters

		String pathFaces = "res/ATTFaces/";
		int inputWidth = 92;
		int inputHeight = 112;
		int inputDim = inputWidth * inputHeight;
		int numPersons = 10; //40
		int numFaces = 10; //10

		int peoplePerEpoch = numPersons;
		int facesPerPerson = numFaces;
		double noiseLevel = 0.01;

		long waitTimeMillis = 100;

		// initialise
		Random random = new Random();
		long epochCount = 0;

		// create pattern sets
		LinkedList<LinkedList<double[]>> patternSets = new LinkedList<LinkedList<double[]>>();

		// loop over persons
		for (int p = 0; p < numPersons; p++) {

			// DEBUG
			System.out.print("Reading person s" + (p + 1));

			// create pattern set
			LinkedList<double[]> patternSet = new LinkedList<double[]>();

			// loop over faces
			for (int f = 0; f < numFaces; f++) {
				String pathFile = "s" + (p + 1) + "/" + (f + 1) + ".png";

				// create pattern
				double[] pattern = new double[inputDim];

				// read image
				int i = 0;
				try {
					BufferedImage imageGrey = ImageIO.read(new File(pathFaces + pathFile));
					BufferedImage imageRGB = new BufferedImage(imageGrey.getWidth(), imageGrey.getHeight(), BufferedImage.TYPE_INT_ARGB);
					imageRGB.createGraphics().drawImage(imageGrey, 0, 0, null);
					for (int y = 0; y < imageRGB.getHeight(); y++) {
						for (int x = 0; x < imageRGB.getWidth(); x++) {
							int rgb = imageRGB.getRGB(x, y);
							int red = imageRGB.getColorModel().getRed(rgb);
							pattern[i++] = (double) red / 255;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				// add pattern to pattern set
				patternSet.add(pattern);

				// DEBUG
				System.out.print(".");

			}

			// add pattern set to pattern sets
			patternSets.add(patternSet);

			// DEBUG
			System.out.println();
		}

		// create topology
		SOINNM topology = new SOINNM(inputDim);
		topology.setNoiseLevel(0.0);
		topology.setUseFixedThreshold(false);
		topology.setFixedThreshold(0.1);
		topology.setAgeDead(1000);
		topology.setConnectNewNodes(false);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(false);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(false);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.01); // 0.001
		topology.setC1Param(0.1); // 0.1
		topology.setClusterJoining(false);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(false);
		topology.setJoinToleranceAbsolute(0.1);
		topology.setJoiningIterationsMax(10);

		// create display
		DisplayFaces display = new DisplayFaces(topology);

		// input patterns
		while (true) {

			// create patterns for epoch
			LinkedList<double[]> patterns = new LinkedList<double[]>();
			for (int p = 0; p < peoplePerEpoch; p++) {
				LinkedList<double[]> patternSet = patternSets.get(p);
				for (int f = 0; f < facesPerPerson; f++) {
					double[] pattern = patternSet.get(f);
					patterns.add(pattern);
				}
			}

			// loop over patterns
			for (double[] pattern : patterns) {

				if (noiseLevel > 0.0) {

					// create noisy pattern
					double pNoisy;
					double[] patternNoisy = new double[inputDim];
					for (int i = 0; i < inputDim; i++) {
						pNoisy = pattern[i] + random.nextGaussian() * noiseLevel;
						if (pNoisy < 0.0) {
							pNoisy = 0.0;
						} else if (pNoisy > 1.0) {
							pNoisy = 1.0;
						}
						patternNoisy[i] = pNoisy;
					}

					// input pattern
					topology.input(patternNoisy);

				} else {

					// input pattern
					topology.input(pattern);
				}

				// DEBUG
				System.out.println("Nodes: " + topology.getNodeSet().size() + "   Edges: " + topology.getEdgeSet().size() + "   Clusters: " + topology.getClusterSet().size());

				// update display
				display.update();

				// wait
				try {
					Thread.sleep(waitTimeMillis);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

			}

			// increase number of epochs
			epochCount += 1;

			// DEBUG
			System.out.println("Epochs: " + epochCount);

		}

	}

}
