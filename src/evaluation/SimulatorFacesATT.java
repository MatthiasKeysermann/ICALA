package evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

import javax.imageio.ImageIO;

import soinnm.Cluster;
import soinnm.SOINNM;
import soinnm.XMLWriter;
import tosam.Simulation;
import tosam.gui.Monitor;
import interactor.gui.FacesATT;
import interactor.gui.TextLabel;

public class SimulatorFacesATT {

	private Simulation tosam;

	private FacesATT facesATT;

	private TextLabel textLabel;

	public SimulatorFacesATT() {

		// initialise
		Random random = new Random();
		int timeMultiplier = 1; // 4

		// create and start TOSAM
		//tosam = new Simulation();
		tosam = new Monitor();
		tosam.setPrintNetworkInfo(false);
		tosam.setPrintDebugInfo(false);
		tosam.setCycleTime(250 * timeMultiplier);
		tosam.getNetwork().setSignalAttraction(true);
		tosam.getNetwork().setAllowOverspreading(false);
		tosam.getNetwork().setFullyConnected(false);
		new Thread(tosam).start();

		// create interactors
		facesATT = new FacesATT();
		textLabel = new TextLabel();

		// parameters
		int numPersonsMax = 40; // 40
		int numFaces = 10; // 10
		FacesATT.Order personOrder = FacesATT.Order.SEQUENTIAL;
		FacesATT.Order faceOrder = FacesATT.Order.RANDOM;
		long timePerPerson = 60000 * timeMultiplier; // ms
		long timePerFace = 1000 * timeMultiplier; // ms
		long durationLearning = timePerPerson; // ms
		long durationBlank = 3000 * timeMultiplier; // ms
		long durationRecall = 3000 * timeMultiplier; // ms
		int numPersonsRecall = 1; // recall only after specified number of persons

		// set experiment parameters
		facesATT.setNumPersons(numPersonsMax);
		facesATT.setNumFaces(numFaces);
		facesATT.setPersonOrder(personOrder);
		facesATT.setFaceOrder(faceOrder);
		facesATT.setTimePerPerson(timePerPerson);
		facesATT.setTimePerFace(timePerFace);

		// set FacesATT parameters
		facesATT.setCycleTime(500 * timeMultiplier);
		SOINNM topologyFacesATT = facesATT.getTopology();
		topologyFacesATT.setNoiseLevel(0.0); // 0.0
		topologyFacesATT.setUseFixedThreshold(false); // true
		topologyFacesATT.setFixedThreshold(0.1); // 0.08
		topologyFacesATT.setMinimumThreshold(0.0); // 0.0
		topologyFacesATT.setAgeDead(50); // 50
		topologyFacesATT.setConnectNewNodes(false); // true
		topologyFacesATT.setLambda(50); // 50
		topologyFacesATT.setEdgeMaxRemoval(true); // true or false
		topologyFacesATT.setNodeNumSignalsMinRemoval(false); // false
		topologyFacesATT.setReduceErrorInsertion(true); // true
		topologyFacesATT.setSmallClusterRemoval(true); // true
		topologyFacesATT.setC2Param(0.001); // 0.001
		topologyFacesATT.setC1Param(0.01); // 0.01
		topologyFacesATT.setClusterJoining(true); // true
		topologyFacesATT.setJoinTolerance(1.0); // 1.0
		topologyFacesATT.setUseAbsoluteJoinTolerance(true); // true
		topologyFacesATT.setJoinToleranceAbsolute(0.1); // 0.08
		topologyFacesATT.setJoiningIterationsMax(10); // 10
		facesATT.setTransformImage(false);
		facesATT.setScaleFactorMax(0.01f); // 0.01f
		facesATT.setRotateAngleMax(1); // 1
		facesATT.setTranslateMax(10); // 10

		// set TextLabel parameters
		textLabel.setCycleTime(500 * timeMultiplier);
		SOINNM topologyTextLabel = textLabel.getTopology();
		topologyTextLabel.setNoiseLevel(0.0);
		topologyTextLabel.setUseFixedThreshold(false);
		topologyTextLabel.setFixedThreshold(0.1);
		topologyTextLabel.setMinimumThreshold(0.0);
		topologyTextLabel.setAgeDead(1000);
		topologyTextLabel.setConnectNewNodes(false);
		topologyTextLabel.setLambda(100);
		topologyTextLabel.setEdgeMaxRemoval(false);
		topologyTextLabel.setNodeNumSignalsMinRemoval(false);
		topologyTextLabel.setReduceErrorInsertion(false);
		topologyTextLabel.setSmallClusterRemoval(true);
		topologyTextLabel.setC2Param(0.0);
		topologyTextLabel.setC1Param(0.0);
		topologyTextLabel.setClusterJoining(false);
		topologyTextLabel.setJoinTolerance(1.0);
		topologyTextLabel.setUseAbsoluteJoinTolerance(false);
		topologyTextLabel.setJoinToleranceAbsolute(0.1);
		topologyTextLabel.setJoiningIterationsMax(10);

		// ignore empty inputs
		facesATT.setIgnoreEmptyInput(false);
		textLabel.setIgnoreEmptyInput(false);

		// create subdirectory
		String subdirectory = "eval/";
		File file = new File(subdirectory);
		file.mkdirs();

		// write parameters to file
		BufferedWriter bwParameters = null;
		try {
			bwParameters = new BufferedWriter(new FileWriter(subdirectory + "Parameters.txt"));
			// general
			bwParameters.write("" + "\n");
			bwParameters.write("GENERAL" + "\n");
			bwParameters.write("-------" + "\n");
			bwParameters.write("durationLearning: " + durationLearning + " ms" + "\n");
			bwParameters.write("durationBlank: " + durationBlank + " ms" + "\n");
			bwParameters.write("durationRecall: " + durationRecall + " ms" + "\n");
			bwParameters.write("number of persons max: " + numPersonsMax + "\n");
			bwParameters.write("person order: " + personOrder.toString() + "\n");
			bwParameters.write("number of faces: " + numFaces + "\n");
			bwParameters.write("face order: " + faceOrder.toString() + "\n");
			bwParameters.write("time per person: " + timePerPerson + " ms" + "\n");
			bwParameters.write("time per face: " + timePerFace + " ms" + "\n");
			bwParameters.write("number of persons recall: " + numPersonsRecall + "\n");
			bwParameters.write("" + "\n");
			// FacesATT
			bwParameters.write("" + "\n");
			bwParameters.write("FACESATT" + "\n");
			bwParameters.write("--------" + "\n");
			bwParameters.write("cycle time: " + facesATT.getCycleTime() + "\n");
			bwParameters.write("activate cluster threshold: " + facesATT.getActivateClusterThreshold() + "\n");
			bwParameters.write("image size: " + FacesATT.getImageWidth() + "x" + FacesATT.getImageHeight() + "\n");
			bwParameters.write("blur radius: " + FacesATT.getBlurRadius() + "\n");
			bwParameters.write(facesATT.getTopology().getInfoParameters());
			bwParameters.write("" + "\n");
			// TextLabel
			bwParameters.write("" + "\n");
			bwParameters.write("TEXTLABEL" + "\n");
			bwParameters.write("---------" + "\n");
			bwParameters.write("cycle time: " + textLabel.getCycleTime() + "\n");
			bwParameters.write("activate cluster threshold: " + textLabel.getActivateClusterThreshold() + "\n");
			bwParameters.write(textLabel.getTopology().getInfoParameters());
			bwParameters.write("" + "\n");
			// TOSAM
			bwParameters.write("" + "\n");
			bwParameters.write("TOSAM" + "\n");
			bwParameters.write("-----" + "\n");
			bwParameters.write("cycle time: " + tosam.getCycleTime() + "\n");
			bwParameters.write("" + "\n");
			bwParameters.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// write data to file
		BufferedWriter bwData = null;
		try {
			bwData = new BufferedWriter(new FileWriter(subdirectory + "Data.csv"));
			bwData.write("numPersons;");
			bwData.write("numFacesTotal;");
			bwData.write("nodeCount;");
			bwData.write("edgeCount;");
			bwData.write("clusterCount;");
			bwData.write("clusterCountThreshold;");
			bwData.write("countFacesTotal;");
			bwData.write("countFacesRecognised;");
			bwData.write("ratioFacesRecognised;");
			bwData.write("countFacesRecalled;");
			bwData.write("ratioFacesRecalled;");
			bwData.write("\n");
			bwData.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// empty line
		System.out.println();

		// initialisation phase
		System.out.println("INITIALISATION PHASE");
		facesATT.setUpdatePersonIndex(false); // manually change persons
		facesATT.setUpdateFaceIndex(false); // manually change faces
		facesATT.setLearning(true);
		facesATT.setRecalling(true);
		textLabel.setLearning(true);
		textLabel.setRecalling(false);

		// blank image & text
		facesATT.setPersonIndex(-1);
		facesATT.setFaceIndex(-1);
		textLabel.setTextInput("");

		// start interactors
		new Thread(facesATT).start();
		new Thread(textLabel).start();

		// blank
		System.out.println("Blanking for " + durationBlank + " ms...");
		try {
			Thread.sleep(durationBlank);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// write data to file
		try {
			bwData.write(0 + ";");
			bwData.write(0 + ";");
			bwData.write(facesATT.getTopology().getNodeSet().size() + ";");
			bwData.write(facesATT.getTopology().getEdgeSet().size() + ";");
			bwData.write(facesATT.getTopology().getClusterSet().size() + ";");
			int clusterCountThreshold = 0;
			for (Cluster cluster : facesATT.getTopology().getClusterSet()) {
				if (cluster.getNodes().size() >= facesATT.getActivateClusterThreshold()) {
					clusterCountThreshold += 1;
				}
			}
			bwData.write(clusterCountThreshold + ";");
			bwData.write(0 + ";");
			bwData.write(0 + ";");
			bwData.write(0 + ";");
			bwData.write(0 + ";");
			bwData.write(0 + ";");
			bwData.write("\n");
			bwData.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// empty line
		System.out.println();

		// loop over number of persons
		for (int numPersons = 1; numPersons <= numPersonsMax; numPersons++) {
			facesATT.setNumPersons(numPersons);
			System.out.println("Number of persons: " + numPersons);

			// calculate number of faces in total
			int numFacesTotal = numFaces * numPersons;

			/*
			 * Learning phase
			 */

			// update person index
			facesATT.setPersonIndex(numPersons - 1);

			// learning phase
			System.out.println("LEARNING PHASE");
			facesATT.setUpdatePersonIndex(false); // manually change persons
			facesATT.setUpdateFaceIndex(true); // automatically change faces
			textLabel.setLearning(true);
			textLabel.setRecalling(false);

			// resume interactors
			facesATT.resume();
			textLabel.resume();

			// learn
			System.out.println("  Learning for " + durationLearning + " ms... ");
			long time = System.currentTimeMillis();
			long timeStartLearning = System.currentTimeMillis();
			while (time - timeStartLearning < timePerPerson) {

				// update label
				textLabel.setTextInput(facesATT.getLabelText());

				// wait
				try {
					Thread.sleep(10);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// update time
				time = System.currentTimeMillis();

			}

			// pause interactors
			facesATT.pause();
			textLabel.pause();

			// update topology counts
			int nodeCount = facesATT.getTopology().getNodeSet().size();
			int edgeCount = facesATT.getTopology().getEdgeSet().size();
			int clusterCount = facesATT.getTopology().getClusterSet().size();
			int clusterCountThreshold = 0;
			for (Cluster cluster : facesATT.getTopology().getClusterSet()) {
				if (cluster.getNodes().size() >= facesATT.getActivateClusterThreshold()) {
					clusterCountThreshold += 1;
				}
			}

			// write TOSAM to XML
			tosam.XMLWriter xmlWriterTOSAM = new tosam.XMLWriter();
			xmlWriterTOSAM.writeToFile(tosam.getNetwork(), subdirectory + "TOSAM-" + numPersons + ".xml");

			// write topologies to XML
			XMLWriter xmlWriter = new XMLWriter();
			xmlWriter.writeToFile(facesATT.getTopology(), subdirectory + "FacesATT-Topology-" + numPersons + ".xml");
			xmlWriter.writeToFile(textLabel.getTopology(), subdirectory + "TextLabel-Topology-" + numPersons + ".xml");

			// write topologies to PNG
			try {
				ImageIO.write(facesATT.getClusterPanelImage(), "png", new File(subdirectory + "FacesATT-ClusterPanel-" + numPersons + ".png"));
				ImageIO.write(textLabel.getClusterPanelImage(), "png", new File(subdirectory + "TextLabel-ClusterPanel-" + numPersons + ".png"));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// empty line
			System.out.println();

			/*
			 * Recall phase
			 */

			if (numPersons % numPersonsRecall == 0) {

				// initialise random recall
				int countFacesTotal = 0;
				int countFacesRecognised = 0;
				int countFacesRecalled = 0;
				int[][] confusionMatrix = new int[numPersons][numPersons];

				// recall all faces in random order
				System.out.println("RANDOM RECALL");
				facesATT.setUpdatePersonIndex(false); // manually change persons
				facesATT.setUpdateFaceIndex(false); // manually change faces
				textLabel.setLearning(false);
				textLabel.setRecalling(true);

				// create permutation
				int[] tSet = new int[numFacesTotal];
				for (int t = 0; t < numFacesTotal; t++) {
					tSet[t] = t;
				}
				for (int n = 0; n < 100000; n++) {
					int i = random.nextInt(numFacesTotal);
					int j = random.nextInt(numFacesTotal);
					int temp = tSet[i];
					tSet[i] = tSet[j];
					tSet[j] = temp;
				}

				// resume interactors
				facesATT.resume();
				textLabel.resume();

				// loop over all faces
				for (int i = 0; i < numFacesTotal; i++) {
					int t = tSet[i];
					int p = t / numFaces;
					int f = t % numFaces;

					// blank image & text
					facesATT.setPersonIndex(-1);
					facesATT.setFaceIndex(-1);
					textLabel.setTextInput("");

					// blank
					System.out.println("  Blanking for " + durationBlank + " ms...");
					try {
						Thread.sleep(durationBlank);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// update indeces
					facesATT.setPersonIndex(p);
					facesATT.setFaceIndex(f);

					// wait for recall
					System.out.print("  Recalling person " + (p + 1) + " (face " + (f + 1) + ") for " + durationRecall + " ms... ");
					try {
						Thread.sleep(durationRecall);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// update faces count
					countFacesTotal += 1;

					// check activated cluster
					int pRecognised = -1;
					Cluster activatedCluster = facesATT.getTopology().getActivatedCluster();
					if (activatedCluster != null) {
						String label = activatedCluster.getLabel();
						if (label != null && label.startsWith(FacesATT.PREFIX_PERSON)) {
							pRecognised = Integer.parseInt(label.substring(FacesATT.PREFIX_PERSON.length())) - 1;
						}
					}

					// update recognised count
					if (p == pRecognised) {
						countFacesRecognised += 1;
						System.out.print("recognition successful! ");
					} else {
						System.out.print("recognition failed! ");
					}

					// check output
					int pRecalled = -1;
					String output = textLabel.getOutput();
					if (output != null && output.startsWith(FacesATT.PREFIX_PERSON)) {
						pRecalled = Integer.parseInt(output.substring(FacesATT.PREFIX_PERSON.length())) - 1;
					}

					// update recalled count					
					if (p == pRecalled) {
						countFacesRecalled += 1;
						System.out.println("recall successful!");
					} else {
						System.out.println("recall failed!");
					}

					// update confusion matrix
					if (pRecalled != -1) {
						confusionMatrix[p][pRecalled] += 1;
					}
					// confusions with blank are not counted

				}

				// blank image & text
				facesATT.setPersonIndex(-1);
				facesATT.setFaceIndex(-1);
				textLabel.setTextInput("");

				// blank
				System.out.println("  Blanking for " + durationBlank + " ms...");
				try {
					Thread.sleep(durationBlank);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// pause interactors
				facesATT.pause();
				textLabel.pause();

				// finalise recall
				double ratioFacesRecognised = (double) countFacesRecognised / countFacesTotal;
				System.out.println("  Correctly recognised faces: " + String.format("%3d of %3d", countFacesRecognised, countFacesTotal) + " (" + String.format("%.2f", ratioFacesRecognised) + ")");
				double ratioFacesRecalled = (double) countFacesRecalled / countFacesTotal;
				System.out.println("  Correctly recalled   faces: " + String.format("%3d of %3d", countFacesRecalled, countFacesTotal) + " (" + String.format("%.2f", ratioFacesRecalled) + ")");

				// write data to file
				try {
					bwData.write(numPersons + ";");
					bwData.write(numFacesTotal + ";");
					bwData.write(nodeCount + ";");
					bwData.write(edgeCount + ";");
					bwData.write(clusterCount + ";");
					bwData.write(clusterCountThreshold + ";");
					bwData.write(countFacesTotal + ";");
					bwData.write(countFacesRecognised + ";");
					bwData.write(ratioFacesRecognised + ";");
					bwData.write(countFacesRecalled + ";");
					bwData.write(ratioFacesRecalled + ";");
					bwData.write("\n");
					bwData.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// write confusion matrix to file
				try {
					BufferedWriter bwConfusionMatrix = new BufferedWriter(new FileWriter(subdirectory + "ConfusionMatrix-" + numPersons + ".csv"));
					for (int p = 0; p < numPersons; p++) {
						for (int pRecalled = 0; pRecalled < numPersons; pRecalled++) {
							bwConfusionMatrix.write(confusionMatrix[p][pRecalled] + " ");
						}
						bwConfusionMatrix.write("\n");
					}
					bwConfusionMatrix.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// empty line
				System.out.println();

			}

		}

		// write data to file
		try {
			bwData.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// finished
		System.out.println("Finished evaluation!");

	}

	public static void main(String[] args) {
		new SimulatorFacesATT();
	}

}
