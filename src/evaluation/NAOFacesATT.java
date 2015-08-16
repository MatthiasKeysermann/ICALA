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
import interactor.gui.NAOCamera;
import interactor.gui.NAOTextToSpeech;

public class NAOFacesATT {

	private Simulation tosam;

	private NAOCamera naoCamera;

	private NAOTextToSpeech naoTextToSpeech;

	private DisplayerFacesATT displayerFacesATT;

	public NAOFacesATT() {

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
		naoCamera = new NAOCamera();
		naoTextToSpeech = new NAOTextToSpeech();
		displayerFacesATT = new DisplayerFacesATT();

		// parameters
		int numPersonsMax = 40; // 40
		int numFaces = 10; // 10
		DisplayerFacesATT.Order personOrder = DisplayerFacesATT.Order.SEQUENTIAL;
		DisplayerFacesATT.Order faceOrder = DisplayerFacesATT.Order.RANDOM;
		long timePerPerson = 180000 * timeMultiplier; // ms
		long timePerFace = 1000 * timeMultiplier; // ms
		long durationLearning = timePerPerson; // ms
		long durationBlank = 3000 * timeMultiplier; // ms
		long durationRecall = 7000 * timeMultiplier; // ms
		int numPersonsRecall = 1; // recall only after specified number of persons

		// set DisplayerFacesATT parameters
		displayerFacesATT.setNumPersons(numPersonsMax);
		displayerFacesATT.setNumFaces(numFaces);
		displayerFacesATT.setPersonOrder(personOrder);
		displayerFacesATT.setFaceOrder(faceOrder);
		displayerFacesATT.setTimePerPerson(timePerPerson);
		displayerFacesATT.setTimePerFace(timePerFace);
		displayerFacesATT.setTransformImage(true);
		displayerFacesATT.setScaleFactorMax(0.01f); // 0.01f
		displayerFacesATT.setRotateAngleMax(1); // 1
		displayerFacesATT.setTranslateMax(10); // 10

		// set NAOCamera parameters
		naoCamera.setCycleTime(500 * timeMultiplier);
		SOINNM topologyNAOCamera = naoCamera.getTopology();
		topologyNAOCamera.setNoiseLevel(0.0); // 0.0
		topologyNAOCamera.setUseFixedThreshold(true); // true
		topologyNAOCamera.setFixedThreshold(0.07); // 0.07
		topologyNAOCamera.setAgeDead(50); // 50
		topologyNAOCamera.setConnectNewNodes(true); // true
		topologyNAOCamera.setLambda(50); // 50
		topologyNAOCamera.setEdgeMaxRemoval(true); // true or false
		topologyNAOCamera.setNodeNumSignalsMinRemoval(false); // false
		topologyNAOCamera.setReduceErrorInsertion(false); // true
		topologyNAOCamera.setSmallClusterRemoval(true); // true
		topologyNAOCamera.setC2Param(0.001); // 0.001
		topologyNAOCamera.setC1Param(0.01); // 0.01
		topologyNAOCamera.setClusterJoining(true); // true
		topologyNAOCamera.setJoinTolerance(1.0); // 1.0
		topologyNAOCamera.setUseAbsoluteJoinTolerance(true); // true
		topologyNAOCamera.setJoinToleranceAbsolute(0.07); // 0.07
		topologyNAOCamera.setJoiningIterationsMax(10); // 10

		// set NAOTextToSpeech parameters
		naoTextToSpeech.setCycleTime(3000 * timeMultiplier);
		SOINNM topologyNAOTextToSpeech = naoTextToSpeech.getTopology();
		topologyNAOTextToSpeech.setNoiseLevel(0.0);
		topologyNAOTextToSpeech.setUseFixedThreshold(false);
		topologyNAOTextToSpeech.setFixedThreshold(0.1);
		topologyNAOTextToSpeech.setMinimumThreshold(0.0);
		topologyNAOTextToSpeech.setAgeDead(1000);
		topologyNAOTextToSpeech.setConnectNewNodes(false);
		topologyNAOTextToSpeech.setLambda(100);
		topologyNAOTextToSpeech.setEdgeMaxRemoval(false);
		topologyNAOTextToSpeech.setNodeNumSignalsMinRemoval(false);
		topologyNAOTextToSpeech.setReduceErrorInsertion(false);
		topologyNAOTextToSpeech.setSmallClusterRemoval(true);
		topologyNAOTextToSpeech.setC2Param(0.0);
		topologyNAOTextToSpeech.setC1Param(0.0);
		topologyNAOTextToSpeech.setClusterJoining(false);
		topologyNAOTextToSpeech.setJoinTolerance(1.0);
		topologyNAOTextToSpeech.setUseAbsoluteJoinTolerance(false);
		topologyNAOTextToSpeech.setJoinToleranceAbsolute(0.1);
		topologyNAOTextToSpeech.setJoiningIterationsMax(10);

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
			// NAOCamera
			bwParameters.write("" + "\n");
			bwParameters.write("NAOCAMERA" + "\n");
			bwParameters.write("--------" + "\n");
			bwParameters.write("cycle time: " + naoCamera.getCycleTime() + "\n");
			bwParameters.write("activate cluster threshold: " + naoCamera.getActivateClusterThreshold() + "\n");
			bwParameters.write("image size: " + NAOCamera.getImageWidth() + "x" + NAOCamera.getImageHeight() + "\n");
			bwParameters.write("blur radius: " + NAOCamera.getBlurRadius() + "\n");
			bwParameters.write(naoCamera.getTopology().getInfoParameters());
			bwParameters.write("" + "\n");
			// NAOTextToSpeech
			bwParameters.write("" + "\n");
			bwParameters.write("NAOTEXTTOSPEECH" + "\n");
			bwParameters.write("---------------" + "\n");
			bwParameters.write("cycle time: " + naoTextToSpeech.getCycleTime() + "\n");
			bwParameters.write("activate cluster threshold: " + naoTextToSpeech.getActivateClusterThreshold() + "\n");
			bwParameters.write(naoTextToSpeech.getTopology().getInfoParameters());
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
			bwData.write("countFacesCorrect;");
			bwData.write("countFacesTotal;");
			bwData.write("ratioFacesCorrect;");
			bwData.write("\n");
			bwData.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// empty line
		System.out.println();

		// initialisation phase
		System.out.println("INITIALISATION PHASE");
		displayerFacesATT.setUpdatePersonIndex(false); // manually change persons
		displayerFacesATT.setUpdateFaceIndex(false); // manually change faces
		naoCamera.setLearning(true);
		naoCamera.setRecalling(true);
		naoTextToSpeech.setLearning(true);
		naoTextToSpeech.setRecalling(false);

		// blank image & text
		displayerFacesATT.setPersonIndex(-1);
		displayerFacesATT.setFaceIndex(-1);
		naoTextToSpeech.setTextInput("");

		// start interactors
		new Thread(naoCamera).start();
		new Thread(naoTextToSpeech).start();
		new Thread(displayerFacesATT).start();

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
			bwData.write(naoCamera.getTopology().getNodeSet().size() + ";");
			bwData.write(naoCamera.getTopology().getEdgeSet().size() + ";");
			bwData.write(naoCamera.getTopology().getClusterSet().size() + ";");
			int clusterCountThreshold = 0;
			for (Cluster cluster : naoCamera.getTopology().getClusterSet()) {
				if (cluster.getNodes().size() >= naoCamera.getActivateClusterThreshold()) {
					clusterCountThreshold += 1;
				}
			}
			bwData.write(clusterCountThreshold + ";");
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
			displayerFacesATT.setNumPersons(numPersons);
			System.out.println("Number of persons: " + numPersons);

			// calculate number of faces in total
			int numFacesTotal = numFaces * numPersons;

			/*
			 * Learning phase
			 */

			// update person index
			displayerFacesATT.setPersonIndex(numPersons - 1);

			// learning phase
			System.out.println("LEARNING PHASE");
			displayerFacesATT.setUpdatePersonIndex(false); // manually change persons
			displayerFacesATT.setUpdateFaceIndex(true); // automatically change faces
			naoTextToSpeech.setLearning(true);
			naoTextToSpeech.setRecalling(false);

			// resume interactors
			naoCamera.resume();
			naoTextToSpeech.resume();
			displayerFacesATT.resume();

			// learn
			System.out.println("  Learning for " + durationLearning + " ms... ");
			long time = System.currentTimeMillis();
			long timeStartLearning = System.currentTimeMillis();
			while (time - timeStartLearning < timePerPerson) {

				// update label
				naoTextToSpeech.setTextInput(displayerFacesATT.getLabelText());

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
			naoCamera.pause();
			naoTextToSpeech.pause();
			displayerFacesATT.pause();

			// update topology counts
			int nodeCount = naoCamera.getTopology().getNodeSet().size();
			int edgeCount = naoCamera.getTopology().getEdgeSet().size();
			int clusterCount = naoCamera.getTopology().getClusterSet().size();
			int clusterCountThreshold = 0;
			for (Cluster cluster : naoCamera.getTopology().getClusterSet()) {
				if (cluster.getNodes().size() >= naoCamera.getActivateClusterThreshold()) {
					clusterCountThreshold += 1;
				}
			}

			// write TOSAM to XML
			tosam.XMLWriter xmlWriterTOSAM = new tosam.XMLWriter();
			xmlWriterTOSAM.writeToFile(tosam.getNetwork(), subdirectory + "TOSAM-" + numPersons + ".xml");

			// write topologies to XML
			XMLWriter xmlWriter = new XMLWriter();
			xmlWriter.writeToFile(naoCamera.getTopology(), subdirectory + "NAOCamera-Topology-" + numPersons + ".xml");
			xmlWriter.writeToFile(naoTextToSpeech.getTopology(), subdirectory + "NAOTextToSpeech-Topology-" + numPersons + ".xml");

			// write topologies to PNG
			try {
				ImageIO.write(naoCamera.getClusterPanelImage(), "png", new File(subdirectory + "NAOCamera-ClusterPanel-" + numPersons + ".png"));
				ImageIO.write(naoTextToSpeech.getClusterPanelImage(), "png", new File(subdirectory + "NAOTextToSpeech-ClusterPanel-" + numPersons + ".png"));
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
				int countFacesCorrect = 0;
				int[][] confusionMatrix = new int[numPersons][numPersons];

				// recall all faces in random order
				System.out.println("RANDOM RECALL");
				displayerFacesATT.setUpdatePersonIndex(false); // manually change persons
				displayerFacesATT.setUpdateFaceIndex(false); // manually change faces
				naoTextToSpeech.setLearning(false);
				naoTextToSpeech.setRecalling(true);

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
				naoCamera.resume();
				naoTextToSpeech.resume();
				displayerFacesATT.resume();

				// loop over all faces
				for (int i = 0; i < numFacesTotal; i++) {
					int t = tSet[i];
					int p = t / numFaces;
					int f = t % numFaces;

					// blank image & text
					displayerFacesATT.setPersonIndex(-1);
					displayerFacesATT.setFaceIndex(-1);
					naoTextToSpeech.setTextInput("");

					// blank
					System.out.println("  Blanking for " + durationBlank + " ms...");
					try {
						Thread.sleep(durationBlank);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// update indeces
					displayerFacesATT.setPersonIndex(p);
					displayerFacesATT.setFaceIndex(f);

					// wait for recall
					System.out.print("  Recalling person " + (p + 1) + " (face " + (f + 1) + ") for " + durationRecall + " ms... ");
					try {
						Thread.sleep(durationRecall);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// check output
					String output = naoTextToSpeech.getOutput();
					int pRecalled = -1;
					if (output != null && output.startsWith(FacesATT.PREFIX_PERSON)) {
						pRecalled = Integer.parseInt(output.substring(FacesATT.PREFIX_PERSON.length())) - 1;
					}

					// update counts
					countFacesTotal += 1;
					if (p == pRecalled) {
						countFacesCorrect += 1;
						System.out.println("successful!");
					} else {
						System.out.println("failed!");
					}

					// update confusion matrix
					if (pRecalled != -1) {
						confusionMatrix[p][pRecalled] += 1;
					}
					// confusions with blank are not counted

				}

				// blank image & text
				displayerFacesATT.setPersonIndex(-1);
				displayerFacesATT.setFaceIndex(-1);
				naoTextToSpeech.setTextInput("");

				// blank
				System.out.println("  Blanking for " + durationBlank + " ms...");
				try {
					Thread.sleep(durationBlank);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// pause interactors
				naoCamera.pause();
				naoTextToSpeech.pause();
				displayerFacesATT.pause();

				// finalise recall
				double ratioFacesCorrect = (double) countFacesCorrect / countFacesTotal;
				System.out.println("  Correctly identified faces: " + countFacesCorrect + " of " + countFacesTotal + " (" + String.format("%.2f", ratioFacesCorrect) + ")");

				// write data to file
				try {
					bwData.write(numPersons + ";");
					bwData.write(numFacesTotal + ";");
					bwData.write(nodeCount + ";");
					bwData.write(edgeCount + ";");
					bwData.write(clusterCount + ";");
					bwData.write(clusterCountThreshold + ";");
					bwData.write(countFacesCorrect + ";");
					bwData.write(countFacesTotal + ";");
					bwData.write(ratioFacesCorrect + ";");
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
		new NAOFacesATT();
	}

}
