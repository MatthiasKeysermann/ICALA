package soinnm.evaluation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import soinnm.Cluster;
import soinnm.Node;
import soinnm.SOINNM;
import soinnm.XMLWriter;
import util.ImagePanel;
import util.PatternPanel;
import util.WrapLayout;
import mnist.tools.MnistManager;

public class EvaluationDigits {

	private final static String MNIST_PATH = "res/MNISTDigits/";

	private final static int IMAGE_WIDTH = 28;

	private final static int IMAGE_HEIGHT = 28;

	private final static int INPUT_DIM = IMAGE_WIDTH * IMAGE_HEIGHT;

	private Random random;

	private final static int CLUSTER_COUNT_DESIRED = 10;

	private final static int DIGIT_COUNT_DESIRED = 10;

	public static enum SCENARIO {
		SEQUENTIAL, RANDOM, ORDERED_SEQUENTIAL, ORDERED_RANDOM, PERMUTATED_SEQUENTIAL, PERMUTATED_RANDOM
	};

	private final static boolean USE_DIRECTORY = true;

	private final static String SUB_DIRECTORY = "Evaluations/";

	private final static boolean SHOW_TOPOLOGY = false;

	private final static boolean SAVE_TOPOLOGY = false;

	private final static boolean SAVE_PARAMETERS = true;

	private final static boolean SAVE_DATA = true;

	private final static boolean SAVE_PLOTS = true;

	public EvaluationDigits() {

		// initialise
		random = new Random();

	}

	public void showTopology(SOINNM topology, String title, String filename) {

		// create frame
		JFrame frame = new JFrame(title);
		frame.setSize(600, 600);
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// create panel
		JPanel panel = new JPanel();
		panel.setLayout(new WrapLayout());
		frame.add(panel);

		// create scroll pane
		JScrollPane spImages = new JScrollPane(panel);
		spImages.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		spImages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		frame.add(spImages);

		// loop over clusters
		for (Cluster cluster : topology.getClusterSet()) {
			double[] pattern = cluster.getMean();
			//double[] pattern = cluster.getPrototype();
			ImagePanel ipCluster = new ImagePanel(getBufferedImage(pattern));
			//String strClusterId = String.valueOf(cluster.getId());
			String strClusterId = " ";
			String strNodes = String.valueOf(cluster.getNodes().size());
			PatternPanel ppCluster = new PatternPanel(ipCluster, strClusterId, strNodes);
			panel.add(ppCluster);
		}

		// show frame
		frame.setVisible(true);

		// DEBUG
		System.out.println("Saving panel as PNG...");

		// wait for GUI
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// save image
		BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		panel.paint(g);

		// write to PNG
		try {
			ImageIO.write(image, "png", new File(filename + "-Panel.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private BufferedImage getBufferedImage(double[] pattern) {
		BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int k = 0;
		for (int y = 0; y < IMAGE_HEIGHT; y++) {
			for (int x = 0; x < IMAGE_WIDTH; x++) {
				int value = (int) Math.round(pattern[k++]);
				if (value < 0) {
					value = 0;
				} else if (value > 255) {
					value = 255;
				}
				int rgba = new Color(value, value, value, 255).getRGB();
				image.setRGB(x, y, rgba);
			}
		}
		return image;
	}

	public void writePlotToPNG(String filename, boolean drawPlotClusters, int clusterCountMin, int clusterCountMax, double clusterCountAvg, boolean drawPlotDigits, int digitCountMin,
			int digitCountMax, double digitCountAvg) {

		// parameters
		int spacingX = 20;
		int spacingY = 20;
		int labelWidth = 40;
		int labelMin = 0;
		int labelMax = 11;
		if (drawPlotClusters) {
			labelMax = 49;
		}
		int labelStep = 1;
		int boxWidth = 20;
		int lineWidth = 40;
		int height = (labelMax + 1) * 30;
		int lineThickness = 2;
		int fontSize = 24;

		// initialise
		int width = 0;
		if (drawPlotClusters) {
			width += spacingX + lineWidth;
		}
		if (drawPlotDigits) {
			width += spacingX + lineWidth;
		}
		width += spacingX;
		double gridStepY = (double) (height - 2 * spacingY) / (labelMax - labelMin + 1);
		int xPos = 0;

		// create image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();

		// background
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);

		// grid
		g.setColor(Color.GRAY);
		for (int i = labelMin; i <= labelMax; i += labelStep) {
			int gridY = height - spacingY - (int) Math.round(i * gridStepY);
			g.drawLine(0, gridY, width, gridY);
		}

		// plot clusters
		if (drawPlotClusters) {
			xPos += spacingX;

			// box for min and max
			xPos += (lineWidth - boxWidth) / 2;
			g.setColor(Color.BLUE);
			int boxMin = height - spacingY - (int) Math.round(clusterCountMin * gridStepY);
			int boxMax = height - spacingY - (int) Math.round(clusterCountMax * gridStepY);
			g.fillRect(xPos, boxMax, boxWidth, boxMin - boxMax);

			// line for average
			xPos -= (lineWidth - boxWidth) / 2;
			g.setColor(Color.BLACK);
			int lineY = height - spacingY - (int) Math.round(clusterCountAvg * gridStepY);
			g.fillRect(xPos, lineY - lineThickness, lineWidth, 2 * lineThickness + 1);
			xPos += lineWidth;

		}

		// plot digits
		if (drawPlotDigits) {
			xPos += spacingX;

			// box for min and max
			xPos += (lineWidth - boxWidth) / 2;
			g.setColor(Color.RED);
			int boxMin = height - spacingY - (int) Math.round(digitCountMin * gridStepY);
			int boxMax = height - spacingY - (int) Math.round(digitCountMax * gridStepY);
			g.fillRect(xPos, boxMax, boxWidth, boxMin - boxMax);

			// line for average
			xPos -= (lineWidth - boxWidth) / 2;
			g.setColor(Color.BLACK);
			int lineY = height - spacingY - (int) Math.round(digitCountAvg * gridStepY);
			g.fillRect(xPos, lineY - lineThickness, lineWidth, 2 * lineThickness + 1);
			xPos += lineWidth;

		}

		// write to PNG
		try {
			ImageIO.write(image, "png", new File(filename + ".png"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// LABELS

		// initialise
		width = spacingX + labelWidth + spacingX;
		gridStepY = (double) (height - 2 * spacingY) / (labelMax - labelMin + 1);
		xPos = 0;

		// create image
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		g = image.createGraphics();

		// background
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);

		// grid
		g.setColor(Color.GRAY);
		for (int i = labelMin; i <= labelMax; i += labelStep) {
			int gridY = height - spacingY - (int) Math.round(i * gridStepY);
			g.drawLine(0, gridY, width, gridY);
		}

		//  labels
		xPos += spacingX;

		g.setColor(Color.GRAY);
		g.setFont(new Font("Serif", Font.BOLD, fontSize));
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = labelMin; i <= labelMax; i += labelStep) {
			int labelY = height - spacingY - (int) Math.round(i * gridStepY);
			g.drawString(String.valueOf(i), xPos, labelY);
		}

		xPos += labelWidth;

		// write to PNG
		try {
			ImageIO.write(image, "png", new File(filename + "-Labels" + ".png"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String getPrefixScenario(SCENARIO scenario, int numRuns, int numInputs) {
		String prefixScenario = "EvaluationDigits";
		switch (scenario) {
		case SEQUENTIAL:
			prefixScenario += "-Sequential";
			break;
		case RANDOM:
			prefixScenario += "-Random";
			break;
		case ORDERED_SEQUENTIAL:
			prefixScenario += "-OrderedSequential";
			break;
		case ORDERED_RANDOM:
			prefixScenario += "-OrderedRandom";
			break;
		case PERMUTATED_SEQUENTIAL:
			prefixScenario += "-PermutatedSequential";
			break;
		case PERMUTATED_RANDOM:
			prefixScenario += "-PermutatedRandom";
			break;
		}
		prefixScenario += "-Runs" + numRuns;
		prefixScenario += "-Inputs" + numInputs;
		return prefixScenario;
	}

	private String getSuffixParameters(SOINNM topology) {
		String suffixParameters = "";
		suffixParameters += "-ageDead" + topology.getAgeDead();
		suffixParameters += "-lambda" + topology.getLambda();
		suffixParameters += "-c2Param" + topology.getC2Param();
		suffixParameters += "-c1Param" + topology.getC1Param();
		return suffixParameters;
	}

	private String getSuffixModifications(SOINNM topology) {
		String suffixModifications = "";
		if (topology.isSmallClusterRemoval()) {
			suffixModifications += "-SCR";
		}
		if (topology.isConnectNewNodes()) {
			suffixModifications += "-CNN";
		}
		if (topology.isEdgeMaxRemoval()) {
			suffixModifications += "-EMR";
		}
		if (topology.isNodeNumSignalsMinRemoval()) {
			suffixModifications += "-NNSMR";
		}
		if (topology.isReduceErrorInsertion()) {
			suffixModifications += "-REI";
		}
		if (topology.isClusterJoining()) {
			suffixModifications += "-CJ";
		}
		return suffixModifications;
	}

	/*
	 * Evaluates a single scenario for the given topology.
	 * Topology parameters are fixed.
	 * Topology modifications are fixed.
	 */
	public Results evaluate(SCENARIO scenario, int numRuns, int numInputs, SOINNM topology) {

		// generate filename prefix
		String prefixScenario = getPrefixScenario(scenario, numRuns, numInputs);

		// generate parameters suffix
		String suffixParameters = getSuffixParameters(topology);

		// generate modifications suffix
		String suffixModifications = getSuffixModifications(topology);

		// DEBUG
		System.out.println(prefixScenario + suffixParameters + suffixModifications);

		// create directory
		String directory = "";
		if (USE_DIRECTORY) {
			directory = SUB_DIRECTORY + prefixScenario + suffixParameters + suffixModifications + "/";
			File file = new File(directory);
			file.mkdirs();
		}

		// adjust number of inputs
		switch (scenario) {
		case SEQUENTIAL:
			System.out.println("number of inputs per digit: " + "n/a");
			break;
		case RANDOM:
			System.out.println("number of inputs per digit: " + "n/a");
			break;
		case ORDERED_SEQUENTIAL:
			numInputs /= 10; // per digit
			System.out.println("number of inputs per digit: " + numInputs);
			break;
		case ORDERED_RANDOM:
			numInputs /= 10; // per digit
			System.out.println("number of inputs per digit: " + numInputs);
			break;
		case PERMUTATED_SEQUENTIAL:
			numInputs /= 10; // per digit
			System.out.println("number of inputs per digit: " + numInputs);
			break;
		case PERMUTATED_RANDOM:
			numInputs /= 10; // per digit
			System.out.println("number of inputs per digit: " + numInputs);
			break;
		}

		// save topology parameters
		if (SAVE_PARAMETERS) {
			try {
				BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(directory + prefixScenario + "-Parameters.txt"));
				bufferedWriter.write(topology.getInfoParameters());
				bufferedWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// create MNistManager
		MnistManager mnist = null;
		try {
			mnist = new MnistManager(MNIST_PATH + "train-images-idx3-ubyte", MNIST_PATH + "train-labels-idx1-ubyte");
			// DEBUG
			System.out.println("database contains " + mnist.getImages().getCount() + " instances");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// initialise
		int[] clusterCount = new int[numRuns];
		int[] digitCount = new int[numRuns];
		int index = 1;
		int maxDigit = 9;
		int[] digitMapping = new int[10];
		for (int digit = 0; digit < 10; digit++) {
			digitMapping[digit] = digit;
		}

		// loop over runs
		for (int run = 1; run <= numRuns; run++) {

			// DEBUG
			System.out.println("run " + run + " of " + numRuns);

			// clear topology
			topology.clear();

			// set maximum digit
			switch (scenario) {
			case SEQUENTIAL:
				maxDigit = 0; // no digit loop
				break;
			case RANDOM:
				maxDigit = 0; // no digit loop
				break;
			case ORDERED_SEQUENTIAL:
				maxDigit = 9; // use digit loop
				break;
			case ORDERED_RANDOM:
				maxDigit = 9; // use digit loop
				break;
			case PERMUTATED_SEQUENTIAL:
				maxDigit = 9; // use digit loop
				break;
			case PERMUTATED_RANDOM:
				maxDigit = 9; // use digit loop
				break;
			}

			// create digit permutation
			switch (scenario) {
			case SEQUENTIAL:
				break;
			case RANDOM:
				break;
			case ORDERED_SEQUENTIAL:
				break;
			case ORDERED_RANDOM:
				break;
			case PERMUTATED_SEQUENTIAL:
				for (int swaps = 0; swaps < 100; swaps++) {
					// determine positions
					int position1 = random.nextInt(10);
					int position2 = random.nextInt(10);
					// swap
					int digit1 = digitMapping[position1];
					int digit2 = digitMapping[position2];
					digitMapping[position1] = digit2;
					digitMapping[position2] = digit1;
				}
				break;
			case PERMUTATED_RANDOM:
				for (int swaps = 0; swaps < 100; swaps++) {
					// determine positions
					int position1 = random.nextInt(10);
					int position2 = random.nextInt(10);
					// swap
					int digit1 = digitMapping[position1];
					int digit2 = digitMapping[position2];
					digitMapping[position1] = digit2;
					digitMapping[position2] = digit1;
				}
				break;
			}

			// loop over digits
			for (int digit = 0; digit <= maxDigit; digit++) {

				// DEBUG
				//System.out.println("digit is " + digit);

				// reset index
				index = 0;

				// loop over inputs
				for (int input = 1; input <= numInputs; input++) {

					// DEBUG
					//System.out.println("input " + input + " of " + numInputs);

					// determine next index
					int imageLabelTemp = -1;
					switch (scenario) {
					case SEQUENTIAL:
						index = index + 1;
						break;
					case RANDOM:
						index = random.nextInt(mnist.getImages().getCount()) + 1;
						break;
					case ORDERED_SEQUENTIAL:
						do {
							index = index + 1;
							mnist.setCurrent(index);
							try {
								imageLabelTemp = mnist.readLabel();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} while (imageLabelTemp != digit);
						break;
					case ORDERED_RANDOM:
						do {
							index = random.nextInt(mnist.getImages().getCount()) + 1;
							mnist.setCurrent(index);
							try {
								imageLabelTemp = mnist.readLabel();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} while (imageLabelTemp != digit);
						break;
					case PERMUTATED_SEQUENTIAL:
						do {
							index = index + 1;
							mnist.setCurrent(index);
							try {
								imageLabelTemp = mnist.readLabel();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} while (imageLabelTemp != digitMapping[digit]);
						break;
					case PERMUTATED_RANDOM:
						do {
							index = random.nextInt(mnist.getImages().getCount()) + 1;
							mnist.setCurrent(index);
							try {
								imageLabelTemp = mnist.readLabel();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} while (imageLabelTemp != digitMapping[digit]);
						break;
					}
					mnist.setCurrent(index);

					// read image and label
					int[][] imageData = null;
					int imageLabel = -1;
					try {
						imageData = mnist.readImage();
						imageLabel = mnist.readLabel();
					} catch (Exception e) {
						e.printStackTrace();
					}

					// DEBUG
					//System.out.println("index is " + index + " label is " + imageLabel);

					// create pattern
					double[] pattern = new double[INPUT_DIM];
					int k = 0;
					for (int i = 0; i < IMAGE_HEIGHT; i++) {
						for (int j = 0; j < IMAGE_WIDTH; j++) {
							pattern[k++] = (double) imageData[i][j];
						}
					}
					String label = String.valueOf(imageLabel);

					// input pattern
					topology.input(pattern, label);

				}

			}

			// show topology
			if (SHOW_TOPOLOGY) {
				showTopology(topology, prefixScenario + "-Run" + run, directory + prefixScenario + "-Run" + run);
			}

			// save topology
			if (SAVE_TOPOLOGY) {
				XMLWriter xmlWriter = new XMLWriter();
				xmlWriter.writeToFile(topology, directory + prefixScenario + "-Run" + run + "-Topology.xml");
			}

			// store cluster count
			clusterCount[run - 1] = topology.getClusterSet().size();

			// DEBUG
			System.out.println("cluster count: " + clusterCount[run - 1]);

			// determine digit set
			HashSet<Integer> digitSet = new HashSet<Integer>();

			// determine cluster labels using majority vote
			for (Cluster cluster : topology.getClusterSet()) {

				// count labels within cluster
				int[] labelCount = new int[10];
				for (Node node : cluster.getNodes()) {
					labelCount[Integer.parseInt(node.getLabel())] += 1;
				}

				// determine maximum
				int clusterLabel = 0;
				for (int digit = 0; digit < 10; digit++) {
					if (labelCount[digit] > labelCount[clusterLabel]) {
						clusterLabel = digit;
					}
				}

				// add to digit set
				digitSet.add(clusterLabel);

			}

			// store digit count
			digitCount[run - 1] = digitSet.size();

			// DEBUG
			System.out.println("  digit count: " + digitCount[run - 1]);

		}

		// save topology data
		if (SAVE_DATA) {
			try {
				BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(directory + prefixScenario + "-Data.csv"));
				bufferedWriter.write("run; clusterCount; digitCount\n");
				for (int run = 1; run <= numRuns; run++) {
					bufferedWriter.write(run + "; " + clusterCount[run - 1] + "; " + digitCount[run - 1] + ";\n");
				}
				bufferedWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// cluster statistics
		int clusterCountMin = Integer.MAX_VALUE;
		int clusterCountMax = Integer.MIN_VALUE;
		double clusterCountAvg = 0;
		for (int run = 1; run <= numRuns; run++) {
			if (clusterCount[run - 1] < clusterCountMin) {
				clusterCountMin = clusterCount[run - 1];
			}
			if (clusterCount[run - 1] > clusterCountMax) {
				clusterCountMax = clusterCount[run - 1];
			}
			clusterCountAvg += clusterCount[run - 1];
		}
		clusterCountAvg /= numRuns;

		// digit statistics
		int digitCountMin = Integer.MAX_VALUE;
		int digitCountMax = Integer.MIN_VALUE;
		double digitCountAvg = 0;
		for (int run = 1; run <= numRuns; run++) {
			if (digitCount[run - 1] < digitCountMin) {
				digitCountMin = digitCount[run - 1];
			}
			if (digitCount[run - 1] > digitCountMax) {
				digitCountMax = digitCount[run - 1];
			}
			digitCountAvg += digitCount[run - 1];
		}
		digitCountAvg /= numRuns;

		// generate results
		Results results = new Results();
		results.clusterCountMin = clusterCountMin;
		results.clusterCountMax = clusterCountMax;
		results.clusterCountAvg = clusterCountAvg;
		results.digitCountMin = digitCountMin;
		results.digitCountMax = digitCountMax;
		results.digitCountAvg = digitCountAvg;

		// DEBUG
		System.out.println(results);

		// save plots
		if (SAVE_PLOTS) {
			writePlotToPNG(directory + prefixScenario + "-Clusters" + suffixParameters + suffixModifications, true, clusterCountMin, clusterCountMax, clusterCountAvg, false, digitCountMin,
					digitCountMax, digitCountAvg);
			writePlotToPNG(directory + prefixScenario + "-Digits" + suffixParameters + suffixModifications, false, clusterCountMin, clusterCountMax, clusterCountAvg, true, digitCountMin,
					digitCountMax, digitCountAvg);
		}

		// return results
		return results;
	}

	/*
	 * Evaluates a single scenario for the given topology.
	 * Topology parameters are varied.
	 * Topology modifications are fixed.
	 */
	public void evaluateParameters(SCENARIO scenario, int numRuns, int numInputs, SOINNM topology) {

		// set parameter ranges
		int[] lambdas = { 10, 25, 50, 100 };
		int[] ageDeads = { 10, 100, 1000 };
		double[] c2Params = { 0.01, 0.02, 0.05, 0.1 };
		double[] c1Params = { 0.1, 0.2, 0.5, 1.0 };

		// generate filename prefix
		String prefixScenario = getPrefixScenario(scenario, numRuns, numInputs);

		// generate modifications suffix
		String suffixModifications = getSuffixModifications(topology);

		// create directory
		String directory = "";
		if (USE_DIRECTORY) {
			directory = SUB_DIRECTORY + prefixScenario + suffixModifications + "/";
			File file = new File(directory);
			file.mkdirs();
		}

		// initialise
		int numCombinations = lambdas.length * ageDeads.length * c2Params.length * c1Params.length;
		int combination = 0;
		double overallScoreBest = Double.MAX_VALUE;
		int lambdaBest = -1;
		int ageDeadBest = -1;
		double c2ParamBest = -1;
		double c1ParamBest = -1;
		Results resultsBest = null;

		// save statistics
		BufferedWriter bwStatistics = null;
		try {
			bwStatistics = new BufferedWriter(new FileWriter(directory + prefixScenario + "-StatisticsParameters.csv"));
			bwStatistics.write("ageDead;lambda;c2Param;c1Param;clusterCountMin;clusterCountMax;clusterCountAvg;digitCountMin;digitCountMax;digitCountAvg;clusterScore;digitScore;overallScore" + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// loop over all combinations
		for (int ageDead : ageDeads) {
			for (int lambda : lambdas) {
				for (double c2Param : c2Params) {
					for (double c1Param : c1Params) {
						combination++;

						// DEBUG
						System.out.println("combination " + combination + " of " + numCombinations);

						// set parameters
						topology.setAgeDead(ageDead);
						topology.setLambda(lambda);
						topology.setC2Param(c2Param);
						topology.setC1Param(c1Param);

						// DEBUG
						System.out.println("ageDead = " + ageDead);
						System.out.println(" lambda = " + lambda);
						System.out.println("c2Param = " + c2Param);
						System.out.println("c1Param = " + c1Param);

						// evaluate combination
						Results results = evaluate(scenario, numRuns, numInputs, topology);

						// compute cluster score
						double clusterScore = getClusterScore(results);

						// compute digit score
						double digitScore = getDigitScore(results);

						// compute overall score
						double overallScore = getOverallScore(clusterScore, digitScore);

						// DEBUG
						System.out.println("Overall score: " + overallScore + "\n");

						// store best overall score
						if (overallScore < overallScoreBest) {
							overallScoreBest = overallScore;
							ageDeadBest = ageDead;
							lambdaBest = lambda;
							c2ParamBest = c2Param;
							c1ParamBest = c1Param;
							resultsBest = results;
						}

						// save statistics
						try {
							bwStatistics.write(ageDead + ";" + lambda + ";" + c2Param + ";" + c1Param + ";" + results.clusterCountMin + ";" + results.clusterCountMax + ";" + results.clusterCountAvg
									+ ";" + results.digitCountMin + ";" + results.digitCountMax + ";" + results.digitCountAvg + ";" + clusterScore + ";" + +digitScore + ";" + overallScore + ";"
									+ "\n");
							bwStatistics.flush();
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				}
			}
		}

		// save statistics
		try {
			bwStatistics.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// DEBUG
		System.out.println("Best overall score: " + overallScoreBest);
		System.out.println("Best ageDead: " + ageDeadBest);
		System.out.println("Best  lambda: " + lambdaBest);
		System.out.println("Best c2Param: " + c2ParamBest);
		System.out.println("Best c1Param: " + c1ParamBest);
		System.out.println("Best results:");
		System.out.println(resultsBest);

	}

	/*
	 * Evaluates a single scenario for the given topology.
	 * Topology parameters are fixed.
	 * Topology modifications are varied.
	 */
	public void evaluateModifications(SCENARIO scenario, int numRuns, int numInputs, SOINNM topology) {

		// generate filename prefix
		String prefixScenario = getPrefixScenario(scenario, numRuns, numInputs);

		// generate parameters suffix
		String suffixParameters = getSuffixParameters(topology);

		// create directory
		String directory = "";
		if (USE_DIRECTORY) {
			directory = SUB_DIRECTORY + prefixScenario + suffixParameters + "/";
			File file = new File(directory);
			file.mkdirs();
		}

		// initialise
		int numCombinations = (int) Math.pow(2, 5); // 5 variables with 2 values each 
		int combination = 0;
		double overallScoreBest = Double.MAX_VALUE;
		int varSCRBest = -1;
		int varCNNBest = -1;
		int varEMRBest = -1;
		int varNNSMRBest = -1;
		int varREIBest = -1;
		int varCJBest = -1;
		Results resultsBest = null;

		// save statistics
		BufferedWriter bwStatistics = null;
		try {
			bwStatistics = new BufferedWriter(new FileWriter(directory + prefixScenario + "-StatisticsModifications.csv"));
			bwStatistics.write("SCR;CNN;EMR;NNSMR;REI;CJ;clusterCountMin;clusterCountMax;clusterCountAvg;digitCountMin;digitCountMax;digitCountAvg;clusterScore;digitScore;overallScore" + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// loop over combinations
		int varSCR = 1;
		for (int varCNN = 0; varCNN <= 1; varCNN++) {
			for (int varEMR = 0; varEMR <= 1; varEMR++) {
				for (int varNNSMR = 0; varNNSMR <= 1; varNNSMR++) {
					for (int varREI = 0; varREI <= 1; varREI++) {
						for (int varCJ = 0; varCJ <= 1; varCJ++) {
							combination++;

							// DEBUG
							System.out.println("combination " + combination + " of " + numCombinations);

							// set modifications
							topology.setSmallClusterRemoval(varSCR == 1);
							topology.setConnectNewNodes(varCNN == 1);
							topology.setEdgeMaxRemoval(varEMR == 1);
							topology.setNodeNumSignalsMinRemoval(varNNSMR == 1);
							topology.setReduceErrorInsertion(varREI == 1);
							topology.setClusterJoining(varCJ == 1);

							// DEBUG
							System.out.println("     smallClusterRemoval = " + topology.isSmallClusterRemoval());
							System.out.println("         connectNewNodes = " + topology.isConnectNewNodes());
							System.out.println("          edgeMaxRemoval = " + topology.isEdgeMaxRemoval());
							System.out.println("nodeNumSignalsMinRemoval = " + topology.isNodeNumSignalsMinRemoval());
							System.out.println("    reduceErrorInsertion = " + topology.isReduceErrorInsertion());
							System.out.println("          clusterJoining = " + topology.isClusterJoining());

							// evaluate combination
							Results results = evaluate(scenario, numRuns, numInputs, topology);

							// compute cluster score
							double clusterScore = getClusterScore(results);

							// compute digit score
							double digitScore = getDigitScore(results);

							// compute overall score
							double overallScore = getOverallScore(clusterScore, digitScore);

							// DEBUG
							System.out.println("Overall score: " + overallScore + "\n");

							// store best overall score
							if (overallScore < overallScoreBest) {
								overallScoreBest = overallScore;
								varSCRBest = varSCR;
								varCNNBest = varCNN;
								varEMRBest = varEMR;
								varNNSMRBest = varNNSMR;
								varREIBest = varREI;
								varCJBest = varCJ;
								resultsBest = results;
							}

							// save statistics
							try {
								bwStatistics.write(varSCR + ";" + varCNN + ";" + varEMR + ";" + varNNSMR + ";" + varREI + ";" + varCJ + ";" + results.clusterCountMin + ";" + results.clusterCountMax
										+ ";" + results.clusterCountAvg + ";" + results.digitCountMin + ";" + results.digitCountMax + ";" + results.digitCountAvg + ";" + clusterScore + ";"
										+ +digitScore + ";" + overallScore + ";" + "\n");
								bwStatistics.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}

						}
					}
				}
			}
		}

		// save statistics
		try {
			bwStatistics.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// DEBUG
		System.out.println("Best overall score: " + overallScoreBest);
		System.out.println("Best   SCR: " + varSCRBest);
		System.out.println("Best   CNN: " + varCNNBest);
		System.out.println("Best   EMR: " + varEMRBest);
		System.out.println("Best NNSMR: " + varNNSMRBest);
		System.out.println("Best   REI: " + varREIBest);
		System.out.println("Best    CJ: " + varCJBest);
		System.out.println("Best results:");
		System.out.println(resultsBest);

	}

	private double getClusterScore(Results results) {

		// compute cluster score						
		double clusterScore = 1.0;

		// difference from desired cluster number
		clusterScore *= Math.abs(results.clusterCountMin - CLUSTER_COUNT_DESIRED) + 1.0;
		clusterScore *= Math.abs(results.clusterCountAvg - CLUSTER_COUNT_DESIRED) + 1.0;
		clusterScore *= Math.abs(results.clusterCountMax - CLUSTER_COUNT_DESIRED) + 1.0;

		// penalise cluster counts below desired cluster number
		if (results.clusterCountMin < CLUSTER_COUNT_DESIRED)
			clusterScore *= 10;
		if (results.clusterCountAvg < CLUSTER_COUNT_DESIRED)
			clusterScore *= 100;
		if (results.clusterCountMax < CLUSTER_COUNT_DESIRED)
			clusterScore *= 1000;

		// favour low variance
		clusterScore *= (results.clusterCountMax - results.clusterCountMin) + 1.0;

		return clusterScore;
	}

	private double getDigitScore(Results results) {

		// compute digit score						
		double digitScore = 1.0;

		// difference from desired digit number
		digitScore *= Math.abs(results.digitCountMin - DIGIT_COUNT_DESIRED) + 1.0;
		digitScore *= Math.abs(results.digitCountAvg - DIGIT_COUNT_DESIRED) + 1.0;
		digitScore *= Math.abs(results.digitCountMax - DIGIT_COUNT_DESIRED) + 1.0;

		// penalise digit counts below desired digit number
		if (results.digitCountMin < DIGIT_COUNT_DESIRED)
			digitScore *= 10;
		if (results.digitCountAvg < DIGIT_COUNT_DESIRED)
			digitScore *= 100;
		if (results.digitCountMax < DIGIT_COUNT_DESIRED)
			digitScore *= 1000;

		// favour low variance
		digitScore *= (results.digitCountMax - results.digitCountMin) + 1.0;

		return digitScore;
	}

	private double getOverallScore(double clusterScore, double digitScore) {

		// compute overall score
		double overallScore = clusterScore * digitScore;

		return overallScore;
	}

	private class Results {

		public int clusterCountMin;

		public int clusterCountMax;

		public double clusterCountAvg;

		public int digitCountMin;

		public int digitCountMax;

		public double digitCountAvg;

		public String toString() {
			String string = "";
			string += "minimum cluster count: " + clusterCountMin + "\n";
			string += "maximum cluster count: " + clusterCountMax + "\n";
			string += "average cluster count: " + clusterCountAvg + "\n";
			string += "minimum   digit count: " + digitCountMin + "\n";
			string += "maximum   digit count: " + digitCountMax + "\n";
			string += "average   digit count: " + digitCountAvg + "\n";
			return string;
		}

	}

	public static void main(String[] args) {

		// create topology
		SOINNM topology = new SOINNM(INPUT_DIM);
		topology.setNoiseLevel(0.0);
		topology.setUseFixedThreshold(false);
		topology.setFixedThreshold(50.0);
		topology.setAgeDead(100);
		topology.setConnectNewNodes(false); // CNN
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(false); // EMR
		topology.setNodeNumSignalsMinRemoval(false); // NNSMR
		topology.setReduceErrorInsertion(false); // REI
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.01);
		topology.setC1Param(0.1);
		topology.setClusterJoining(false); // CJ
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(false);
		topology.setJoinToleranceAbsolute(0.1);
		topology.setJoiningIterationsMax(10);
		topology.setPrintDebugInfo(false);

		// evaluate digits
		EvaluationDigits evaluationDigits = new EvaluationDigits();
		//evaluationDigits.evaluate(SCENARIO.SEQUENTIAL, 1, 1000, topology);
		//evaluationDigits.evaluate(SCENARIO.ORDERED_SEQUENTIAL, 1, 1000, topology);
		//evaluationDigits.evaluate(SCENARIO.PERMUTATED_SEQUENTIAL, 1, 1000, topology);

		evaluationDigits.evaluateParameters(SCENARIO.RANDOM, 100, 1000, topology);
		//evaluationDigits.evaluateModifications(SCENARIO.RANDOM, 100, 1000, topology);
		//evaluationDigits.evaluate(SCENARIO.RANDOM, 1, 1000, topology);

		//evaluationDigits.evaluateParameters(SCENARIO.ORDERED_RANDOM, 100, 1000, topology);
		//evaluationDigits.evaluateModifications(SCENARIO.ORDERED_RANDOM, 100, 1000, topology);
		//evaluationDigits.evaluate(SCENARIO.ORDERED_RANDOM, 1, 1000, topology);

		//evaluationDigits.evaluateParameters(SCENARIO.PERMUTATED_RANDOM, 100, 1000, topology);
		//evaluationDigits.evaluateModifications(SCENARIO.PERMUTATED_RANDOM, 100, 1000, topology);
		//evaluationDigits.evaluate(SCENARIO.PERMUTATED_RANDOM, 1, 1000, topology);

	}

}
