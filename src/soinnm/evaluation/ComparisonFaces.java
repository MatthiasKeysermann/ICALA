package soinnm.evaluation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jhlabs.image.GaussianFilter;

import soinnm.Cluster;
import soinnm.Node;
import soinnm.SOINNM;

public class ComparisonFaces {

	private Random random;

	private static enum SCENARIO {
		STATIONARY, NONSTATIONARY
	};

	private final static String PREFIX_DIRECTORY = "Evaluations/";

	private final static int[] INDECES = { 1, 12, 3, 14, 5, 6, 7, 8, 9, 10 };

	private final static int NUM_PERSONS = INDECES.length; // 40

	private final static int NUM_FACES = 10; // 10

	private final static int INPUT_WIDTH = 23; // 92

	private final static int INPUT_HEIGHT = 28; // 112

	private final static int BLUR_RADIUS = 4; // sigma = radius / 3

	private final static int INPUT_DIM = INPUT_WIDTH * INPUT_HEIGHT;

	private LinkedList<LinkedList<double[]>> patternSets;

	private final static boolean EVALUATE_PROTOTYPE_LABEL = true; // use cluster prototype label instead of node label for evaluation

	private void createPatterns() {

		// initialise
		random = new Random();
		String pathFaces = "res/ATTFaces/";

		// create pattern sets
		patternSets = new LinkedList<LinkedList<double[]>>();

		// loop over persons
		for (int p = 0; p < INDECES.length; p++) {

			// DEBUG
			System.out.print("Reading person s" + INDECES[p]);

			// create pattern set
			LinkedList<double[]> patternSet = new LinkedList<double[]>();

			// loop over faces
			for (int f = 0; f < NUM_FACES; f++) {
				String pathFile = "s" + (p + 1) + "/" + (f + 1) + ".png";

				// create pattern
				double[] pattern = new double[INPUT_HEIGHT * INPUT_WIDTH];
				int i = 0;
				try {
					// read image
					BufferedImage imageGrey = ImageIO.read(new File(pathFaces + pathFile));
					// convert to RGB					
					BufferedImage imageRGB = new BufferedImage(imageGrey.getWidth(), imageGrey.getHeight(), BufferedImage.TYPE_INT_ARGB);
					imageRGB.createGraphics().drawImage(imageGrey, 0, 0, null);
					// resize
					BufferedImage imageResized = new BufferedImage(INPUT_WIDTH, INPUT_HEIGHT, imageRGB.getType());
					((Graphics2D) imageResized.getGraphics()).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					imageResized.getGraphics().drawImage(imageRGB, 0, 0, INPUT_WIDTH, INPUT_HEIGHT, null);
					// blur
					BufferedImage imageBlurred = new BufferedImage(imageResized.getWidth(), imageResized.getHeight(), imageResized.getType());
					GaussianFilter gaussianFilter = new GaussianFilter(BLUR_RADIUS);
					gaussianFilter.filter(imageResized, imageBlurred);
					// fill pattern
					for (int y = 0; y < imageBlurred.getHeight(); y++) {
						for (int x = 0; x < imageBlurred.getWidth(); x++) {
							int rgb = imageBlurred.getRGB(x, y);
							int red = imageBlurred.getColorModel().getRed(rgb);
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

	}

	private BufferedImage getBufferedImage(double[] pattern, float scaleFactor) {
		BufferedImage image = new BufferedImage(INPUT_WIDTH, INPUT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int i = 0;
		for (int y = 0; y < INPUT_HEIGHT; y++) {
			for (int x = 0; x < INPUT_WIDTH; x++) {
				int grey = (int) Math.round(pattern[i++] * 255);
				int rgba = new Color(grey, grey, grey, 255).getRGB();
				image.setRGB(x, y, rgba);
			}
		}
		// scale image
		if (scaleFactor != 1.0f) {
			int scaledWidth = Math.round(INPUT_WIDTH * scaleFactor);
			int scaledHeight = Math.round(INPUT_HEIGHT * scaleFactor);
			BufferedImage imageScaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
			((Graphics2D) imageScaled.getGraphics()).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			imageScaled.getGraphics().drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
			image = imageScaled;
		}
		return image;
	}

	public void displayPatterns() {

		JFrame frame = new JFrame("Patterns");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(550, 650);
		frame.setLocation(0, 0);
		frame.setVisible(true);

		JPanel panel = new JPanel();
		frame.getContentPane().add(panel);

		for (int p = 0; p < NUM_PERSONS; p++) {
			for (int f = 0; f < NUM_FACES; f++) {
				BufferedImage image = getBufferedImage(patternSets.get(p).get(f), 2.0f);
				JLabel lbImage = new JLabel(new ImageIcon(image));
				panel.add(lbImage);
			}
		}

		panel.updateUI();

	}

	public void displayClustersMeans(SOINNM soinnm) {

		JFrame frame = new JFrame("Cluster Means");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(550, 650);
		frame.setLocation(600, 0);
		frame.setVisible(true);

		JPanel panel = new JPanel();
		frame.getContentPane().add(panel);

		for (Cluster cluster : soinnm.getClusterSet()) {
			BufferedImage image = getBufferedImage(cluster.getMean(), 2.0f);
			JLabel lbImage = new JLabel(new ImageIcon(image));
			panel.add(lbImage);
		}

		panel.updateUI();

	}

	public void displayClusterPrototypes(SOINNM soinnm) {

		JFrame frame = new JFrame("Cluster Prototypes");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(550, 650);
		frame.setLocation(1200, 0);
		frame.setVisible(true);

		JPanel panel = new JPanel();
		frame.getContentPane().add(panel);

		for (Cluster cluster : soinnm.getClusterSet()) {
			BufferedImage image = getBufferedImage(cluster.getPrototype(), 2.0f);
			JLabel lbImage = new JLabel(new ImageIcon(image));
			panel.add(lbImage);
		}

		panel.updateUI();

	}

	public void learnStationary(SOINNM soinnm, int numIterationsPerPerson) {

		int numIterations = NUM_PERSONS * numIterationsPerPerson;
		for (int i = 0; i < numIterations; i++) {

			// choose random person
			int p = random.nextInt(NUM_PERSONS);

			// choose random face
			int f = random.nextInt(NUM_FACES);

			// input pattern
			double[] pattern = patternSets.get(p).get(f);
			soinnm.input(pattern, String.valueOf(p));

		}

	}

	public void learnNonStationary(SOINNM soinnm, int numIterationsPerPerson) {

		for (int p = 0; p < NUM_PERSONS; p++) {
			for (int i = 0; i < numIterationsPerPerson; i++) {

				// choose random face
				int f = random.nextInt(NUM_FACES);

				// input pattern
				double[] pattern = patternSets.get(p).get(f);
				soinnm.input(pattern, String.valueOf(p));

			}
		}

	}

	private double euclideanDistance(double[] A, double[] B) {
		double sum = 0.0;
		double difference;

		for (int i = 0; i < INPUT_DIM; i++) {
			difference = A[i] - B[i];
			sum += difference * difference;
		}

		return Math.sqrt(sum);
	}

	private int getCountCorrect(int[][] confusionMatrix) {
		int countCorrect = 0;
		for (int i = 0; i < NUM_PERSONS; i++) {
			countCorrect += confusionMatrix[i][i];
		}
		return countCorrect;
	}

	private int getCountTotal(int[][] confusionMatrix) {
		int countTotal = 0;
		for (int i = 0; i < NUM_PERSONS; i++) {
			for (int j = 0; j < NUM_PERSONS; j++) {
				countTotal += confusionMatrix[i][j];
			}
		}
		return countTotal;
	}

	public int[][] evaluate(SOINNM soinnm) {
		int confusionMatrix[][] = new int[NUM_PERSONS][NUM_PERSONS];

		for (int p = 0; p < NUM_PERSONS; p++) {
			for (int f = 0; f < NUM_FACES; f++) {

				// find nearest node
				double distanceMin = Double.MAX_VALUE;
				Node nodeMin = null;
				for (Node node : soinnm.getNodeSet()) {
					double distance = euclideanDistance(node.getPattern(), patternSets.get(p).get(f));
					if (distance < distanceMin) {
						distanceMin = distance;
						nodeMin = node;
					}
				}

				// check if node represents given person
				if (nodeMin != null) {
					int nodeMinP = Integer.parseInt(nodeMin.getLabel());

					// use prototype label instead of node label
					if (EVALUATE_PROTOTYPE_LABEL) {

						// determine cluster of nearest node
						Cluster clusterMin = soinnm.getCluster(nodeMin);

						// check if cluster prototype represents given person
						int protoP = Integer.parseInt(clusterMin.getPrototypeNode().getLabel());

						// overwrite node label
						nodeMinP = protoP;

					}

					// update counts
					confusionMatrix[p][nodeMinP] += 1;

				}

			}
		}

		// DEBUG
		/*
		System.out.println("confusion matrix:");
		for (int i = 0; i < NUM_PERSONS; i++) {
			String line = "";
			for (int j = 0; j < NUM_PERSONS; j++) {
				line += String.format("%3d", confusionMatrix[i][j]) + " ";
			}
			System.out.println(line);
		}
		*/
		return confusionMatrix;
	}

	public void testConfigurations(SCENARIO scenario, SOINNM soinnm, int numIterationsPerPerson, int numRuns) {

		// set parameter ranges
		int[] lambdas = { 10, 50, 100 }; // { 25 }; 
		int[] ageDeads = { 10, 50, 100 }; // { 25 };  
		double[] c2Params = { 0.1 }; // { 0.01, 0.05, 0.1 }; // { 0.0 };
		double[] c1Params = { 0.1 }; // { 0.1, 0.5, 1.0 }; // { 1.0 };

		// create directory
		File file = new File(PREFIX_DIRECTORY);
		file.mkdirs();

		// write statistics to file
		String filenameStatistics = PREFIX_DIRECTORY + "ComparisonFaces";
		filenameStatistics += getSuffixScenario(scenario);
		filenameStatistics += "-Statistics";
		filenameStatistics += ".csv";
		BufferedWriter bwStatistics = null;
		try {
			bwStatistics = new BufferedWriter(new FileWriter(filenameStatistics));
			bwStatistics.write("ageDead;");
			bwStatistics.write("lambda;");
			bwStatistics.write("c2Param;");
			bwStatistics.write("c1Param;");
			bwStatistics.write("SCR;");
			bwStatistics.write("CNN;");
			bwStatistics.write("EMR;");
			bwStatistics.write("NNSMR;");
			bwStatistics.write("REI;");
			bwStatistics.write("CJ;");
			bwStatistics.write("nodeCountMin;");
			bwStatistics.write("nodeCountMax;");
			bwStatistics.write("nodeCountAvg;");
			bwStatistics.write("edgeCountMin;");
			bwStatistics.write("edgeCountMax;");
			bwStatistics.write("edgeCountAvg;");
			bwStatistics.write("clusterCountMin;");
			bwStatistics.write("clusterCountMax;");
			bwStatistics.write("clusterCountAvg;");
			bwStatistics.write("recognitionRateMin;");
			bwStatistics.write("recognitionRateMax;");
			bwStatistics.write("recognitionRateAvg;");
			bwStatistics.write("\n");
			bwStatistics.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// initialise
		int numCombinations = lambdas.length * ageDeads.length * c2Params.length * c1Params.length;
		numCombinations *= 2 * 2 * 2 * 2 * 2;
		int combination = 0;

		// loop over combinations
		for (int ageDead : ageDeads) {
			for (int lambda : lambdas) {
				for (double c2Param : c2Params) {
					for (double c1Param : c1Params) {
						int varSCR = 1;
						for (int varCNN = 0; varCNN <= 1; varCNN++) {
							for (int varEMR = 0; varEMR <= 1; varEMR++) {
								for (int varNNSMR = 0; varNNSMR <= 1; varNNSMR++) {
									for (int varREI = 0; varREI <= 1; varREI++) {
										for (int varCJ = 0; varCJ <= 1; varCJ++) {
											combination++;

											// DEBUG
											System.out.println("combination " + combination + " of " + numCombinations);

											// set parameters
											soinnm.setAgeDead(ageDead);
											soinnm.setLambda(lambda);
											soinnm.setC2Param(c2Param);
											soinnm.setC1Param(c1Param);

											// DEBUG
											System.out.println("                 ageDead = " + soinnm.getAgeDead());
											System.out.println("                  lambda = " + soinnm.getLambda());
											System.out.println("                 c2Param = " + soinnm.getC2Param());
											System.out.println("                 c1Param = " + soinnm.getC1Param());

											// set modifications
											soinnm.setSmallClusterRemoval(varSCR == 1);
											soinnm.setConnectNewNodes(varCNN == 1);
											soinnm.setEdgeMaxRemoval(varEMR == 1);
											soinnm.setNodeNumSignalsMinRemoval(varNNSMR == 1);
											soinnm.setReduceErrorInsertion(varREI == 1);
											soinnm.setClusterJoining(varCJ == 1);

											// DEBUG
											System.out.println("     smallClusterRemoval = " + soinnm.isSmallClusterRemoval());
											System.out.println("         connectNewNodes = " + soinnm.isConnectNewNodes());
											System.out.println("          edgeMaxRemoval = " + soinnm.isEdgeMaxRemoval());
											System.out.println("nodeNumSignalsMinRemoval = " + soinnm.isNodeNumSignalsMinRemoval());
											System.out.println("    reduceErrorInsertion = " + soinnm.isReduceErrorInsertion());
											System.out.println("          clusterJoining = " + soinnm.isClusterJoining());

											// test configuration
											Statistics statistics = testRuns(scenario, soinnm, numIterationsPerPerson, numRuns);

											// write statistics to file
											try {
												bwStatistics.write(ageDead + ";");
												bwStatistics.write(lambda + ";");
												bwStatistics.write(c2Param + ";");
												bwStatistics.write(c1Param + ";");
												bwStatistics.write(varSCR + ";");
												bwStatistics.write(varCNN + ";");
												bwStatistics.write(varEMR + ";");
												bwStatistics.write(varNNSMR + ";");
												bwStatistics.write(varREI + ";");
												bwStatistics.write(varCJ + ";");
												bwStatistics.write(statistics.nodeCountMin + ";");
												bwStatistics.write(statistics.nodeCountMax + ";");
												bwStatistics.write(statistics.nodeCountAvg + ";");
												bwStatistics.write(statistics.edgeCountMin + ";");
												bwStatistics.write(statistics.edgeCountMax + ";");
												bwStatistics.write(statistics.edgeCountAvg + ";");
												bwStatistics.write(statistics.clusterCountMin + ";");
												bwStatistics.write(statistics.clusterCountMax + ";");
												bwStatistics.write(statistics.clusterCountAvg + ";");
												bwStatistics.write(statistics.recognitionRateMin + ";");
												bwStatistics.write(statistics.recognitionRateMax + ";");
												bwStatistics.write(statistics.recognitionRateAvg + ";");
												bwStatistics.write("\n");
												bwStatistics.flush();
											} catch (Exception e) {
												e.printStackTrace();
											}

										}
									}
								}
							}
						}
					}
				}
			}
		}

		// write statistics to file
		try {
			bwStatistics.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Statistics testRuns(SCENARIO scenario, SOINNM soinnm, int numIterationsPerPerson, int numRuns) {

		// initialise
		int nodeCounts[] = new int[numRuns];
		int edgeCounts[] = new int[numRuns];
		int clusterCounts[] = new int[numRuns];
		double recognitionRates[] = new double[numRuns];

		// loop over runs
		for (int run = 0; run < numRuns; run++) {

			// DEBUG
			System.out.println("run " + (run + 1) + " of " + numRuns);

			// learn
			soinnm.clear();
			switch (scenario) {
			case STATIONARY:
				learnStationary(soinnm, numIterationsPerPerson);
				break;
			case NONSTATIONARY:
				learnNonStationary(soinnm, numIterationsPerPerson);
				break;
			}

			// store topology data
			int nodeCount = soinnm.getNodeSet().size();
			int edgeCount = soinnm.getEdgeSet().size();
			int clusterCount = soinnm.getClusterSet().size();
			nodeCounts[run] = nodeCount;
			edgeCounts[run] = edgeCount;
			clusterCounts[run] = clusterCount;

			// DEBUG
			//System.out.println("   node count: " + nodeCount);
			//System.out.println("   edge count: " + edgeCount);
			//System.out.println("cluster count: " + clusterCount);

			// evaluate
			int[][] confusionMatrix = evaluate(soinnm);
			int countCorrect = getCountCorrect(confusionMatrix);
			int countTotal = getCountTotal(confusionMatrix);
			double recognitionRate = 0;
			if (countTotal != 0) {
				recognitionRate = (double) countCorrect / countTotal;
			}
			recognitionRates[run] = recognitionRate;

			// DEBUG
			//System.out.println("recognition rate: " + recognitionRate);

		}

		// create directory
		File file = new File(PREFIX_DIRECTORY);
		file.mkdirs();

		// write data to file
		String filenameData = PREFIX_DIRECTORY + "ComparisonFaces";
		filenameData += getSuffixScenario(scenario);
		filenameData += "-Data";
		filenameData += getSuffixParameters(soinnm);
		filenameData += getSuffixModifications(soinnm);
		filenameData += ".csv";
		try {
			BufferedWriter bwData = new BufferedWriter(new FileWriter(filenameData));
			bwData.write("run;nodeCount;edgeCount;clusterCount;recognitionRate;" + "\n");
			for (int run = 0; run < numRuns; run++) {
				bwData.write((run + 1) + ";");
				bwData.write(nodeCounts[run] + ";");
				bwData.write(edgeCounts[run] + ";");
				bwData.write(clusterCounts[run] + ";");
				bwData.write(recognitionRates[run] + ";");
				bwData.write("\n");
			}
			bwData.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// node statistics
		int nodeCountSum = 0;
		int nodeCountMin = Integer.MAX_VALUE;
		int nodeCountMax = Integer.MIN_VALUE;
		for (int run = 0; run < numRuns; run++) {
			int nodeCount = nodeCounts[run];
			nodeCountSum += nodeCount;
			if (nodeCount < nodeCountMin) {
				nodeCountMin = nodeCount;
			}
			if (nodeCount > nodeCountMax) {
				nodeCountMax = nodeCount;
			}
		}
		double nodeCountAvg = (double) nodeCountSum / numRuns;

		// DEBUG
		System.out.println("minimum node count: " + nodeCountMin);
		System.out.println("maximum node count: " + nodeCountMax);
		System.out.println("average node count: " + nodeCountAvg);

		// edge statistics
		int edgeCountSum = 0;
		int edgeCountMin = Integer.MAX_VALUE;
		int edgeCountMax = Integer.MIN_VALUE;
		for (int run = 0; run < numRuns; run++) {
			int edgeCount = edgeCounts[run];
			edgeCountSum += edgeCount;
			if (edgeCount < edgeCountMin) {
				edgeCountMin = edgeCount;
			}
			if (edgeCount > edgeCountMax) {
				edgeCountMax = edgeCount;
			}
		}
		double edgeCountAvg = (double) edgeCountSum / numRuns;

		// DEBUG
		System.out.println("minimum edge count: " + edgeCountMin);
		System.out.println("maximum edge count: " + edgeCountMax);
		System.out.println("average edge count: " + edgeCountAvg);

		// cluster statistics
		int clusterCountSum = 0;
		int clusterCountMin = Integer.MAX_VALUE;
		int clusterCountMax = Integer.MIN_VALUE;
		for (int run = 0; run < numRuns; run++) {
			int clusterCount = clusterCounts[run];
			clusterCountSum += clusterCount;
			if (clusterCount < clusterCountMin) {
				clusterCountMin = clusterCount;
			}
			if (clusterCount > clusterCountMax) {
				clusterCountMax = clusterCount;
			}
		}
		double clusterCountAvg = (double) clusterCountSum / numRuns;

		// DEBUG
		System.out.println("minimum cluster count: " + clusterCountMin);
		System.out.println("maximum cluster count: " + clusterCountMax);
		System.out.println("average cluster count: " + clusterCountAvg);

		// recognition rate statistics
		double recognitionRateSum = 0;
		double recognitionRateMin = Double.MAX_VALUE;
		double recognitionRateMax = Double.MIN_VALUE;
		for (int run = 0; run < numRuns; run++) {
			double recognitionRate = recognitionRates[run];
			recognitionRateSum += recognitionRate;
			if (recognitionRate < recognitionRateMin) {
				recognitionRateMin = recognitionRate;
			}
			if (recognitionRate > recognitionRateMax) {
				recognitionRateMax = recognitionRate;
			}
		}
		double recognitionRateAvg = recognitionRateSum / numRuns;

		// DEBUG
		System.out.println("minimum recognition rate: " + recognitionRateMin);
		System.out.println("maximum recognition rate: " + recognitionRateMax);
		System.out.println("average recognition rate: " + recognitionRateAvg);

		// return statistics
		Statistics statistics = new Statistics();
		statistics.nodeCountMin = nodeCountMin;
		statistics.nodeCountMax = nodeCountMax;
		statistics.nodeCountAvg = nodeCountAvg;
		statistics.edgeCountMin = edgeCountMin;
		statistics.edgeCountMax = edgeCountMax;
		statistics.edgeCountAvg = edgeCountAvg;
		statistics.clusterCountMin = clusterCountMin;
		statistics.clusterCountMax = clusterCountMax;
		statistics.clusterCountAvg = clusterCountAvg;
		statistics.recognitionRateMin = recognitionRateMin;
		statistics.recognitionRateMax = recognitionRateMax;
		statistics.recognitionRateAvg = recognitionRateAvg;
		return statistics;
	}

	private String getSuffixScenario(SCENARIO scenario) {
		switch (scenario) {
		case STATIONARY:
			return "-Stationary";
		case NONSTATIONARY:
			return "-NonStationary";
		}
		return null;
	}

	private String getSuffixParameters(SOINNM soinnm) {
		String suffixParameters = "";
		suffixParameters += "-ageDead" + soinnm.getAgeDead();
		suffixParameters += "-lambda" + soinnm.getLambda();
		suffixParameters += "-c2Param" + soinnm.getC2Param();
		suffixParameters += "-c1Param" + soinnm.getC1Param();
		return suffixParameters;
	}

	private String getSuffixModifications(SOINNM soinnm) {
		String suffixModifications = "";
		if (soinnm.isSmallClusterRemoval()) {
			suffixModifications += "-SCR";
		}
		if (soinnm.isConnectNewNodes()) {
			suffixModifications += "-CNN";
		}
		if (soinnm.isEdgeMaxRemoval()) {
			suffixModifications += "-EMR";
		}
		if (soinnm.isNodeNumSignalsMinRemoval()) {
			suffixModifications += "-NNSMR";
		}
		if (soinnm.isReduceErrorInsertion()) {
			suffixModifications += "-REI";
		}
		if (soinnm.isClusterJoining()) {
			suffixModifications += "-CJ";
		}
		return suffixModifications;
	}

	private class Statistics {

		public int nodeCountMin = -1;

		public int nodeCountMax = -1;

		public double nodeCountAvg = -1;

		public int edgeCountMin = -1;

		public int edgeCountMax = -1;

		public double edgeCountAvg = -1;

		public int clusterCountMin = -1;

		public int clusterCountMax = -1;

		public double clusterCountAvg = -1;

		public double recognitionRateMin = -1;

		public double recognitionRateMax = -1;

		public double recognitionRateAvg = -1;

	}

	public static void main(String[] args) {

		ComparisonFaces comparisonFaces = new ComparisonFaces();
		comparisonFaces.createPatterns();
		//comparisonFaces.displayPatterns();

		SOINNM soinnm = new SOINNM(INPUT_DIM);
		soinnm.setNoiseLevel(0.0);
		soinnm.setUseFixedThreshold(false);
		soinnm.setFixedThreshold(0.01);
		soinnm.setAgeDead(25); // 25
		soinnm.setConnectNewNodes(false);
		soinnm.setLambda(25); // 25
		soinnm.setEdgeMaxRemoval(false);
		soinnm.setNodeNumSignalsMinRemoval(false);
		soinnm.setReduceErrorInsertion(true); // true
		soinnm.setSmallClusterRemoval(true); // true
		soinnm.setC2Param(0.1); // 0.0
		soinnm.setC1Param(0.1); // 1.0
		soinnm.setClusterJoining(false);
		soinnm.setJoinTolerance(1.0);
		soinnm.setUseAbsoluteJoinTolerance(false);
		soinnm.setJoinToleranceAbsolute(0.1);
		soinnm.setJoiningIterationsMax(10);

		int inputsPerPerson = 1000; // 1000
		//comparisonFaces.learnNonStationary(soinnm, inputsPerPerson);
		//comparisonFaces.displayClustersMeans(soinnm);
		//comparisonFaces.displayClusterPrototypes(soinnm);
		//comparisonFaces.evaluate(soinnm);

		int numRuns = 1000; // 1000
		comparisonFaces.testRuns(SCENARIO.NONSTATIONARY, soinnm, inputsPerPerson, numRuns);
		//comparisonFaces.testConfigurations(SCENARIO.NONSTATIONARY, soinnm, inputsPerPerson, numRuns);

	}

}
