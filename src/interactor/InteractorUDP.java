package interactor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import soinnm.Cluster;
import soinnm.SOINNM;
import soinnm.XMLReader;
import soinnm.XMLWriter;

import java.io.File;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Abstract class for an interactor using the UDP protocol. Basis for an M-SOINN
 * module for reading sensory data, sending cluster data to TOSAM, requesting
 * the network state for this module and calculating the output. Allows to
 * write/read a topology state to/from an XML file.
 * <p>
 * An interactor runs continously in cycles of a given cycle time. Each cycle
 * consists of the following steps: read an input from a given source (e.g. a
 * sensor), input the data pattern to M-SOINN, send activated cluster to TOSAM,
 * send information on topology updates (split, joined, removed clusters) to
 * TOSAM, receive activation levels of this module's units, computing the output
 * pattern, writing the output pattern to a given sink (e.g. an actuator).
 * 
 * @author Matthias Keysermann
 *
 */
public abstract class InteractorUDP implements Runnable {

	private boolean running;

	private long cycleTime; // milliseconds

	private long cycleDuration; // milliseconds

	private static final long CYCLE_TIME_DEFAULT = 100; // milliseconds

	private long iterationCount;

	private long iterationCountStop;

	private String name;

	private SOINNM topology;

	private HashMap<Cluster, Double> activationMap;

	private double activationOutput;

	private int activateClusterThreshold;

	private boolean innerFeedback; // feed back output as an additional input to the topology

	private String hostName = "localhost";

	private final static int UDP_PORT_SEND = 5000; // UDP port to send data to

	private final static int UDP_PORT_RECEIVE = 5001; // UDP port to send requests to

	private final static int PACKET_SIZE = 4096; // maximum size of received data

	private DatagramSocket socketSend;

	private DatagramSocket socketReceive;

	private static final int RECEIVE_TIMEOUT = 100; // milliseconds

	protected File fileSave;

	protected File fileLoad;

	protected File fileInsert;

	public InteractorUDP(String name, int inputDim) {
		this.name = name;

		// create topology		
		topology = new SOINNM(inputDim);
		activationMap = new HashMap<Cluster, Double>();

		// create sockets
		try {
			socketSend = new DatagramSocket();
			socketReceive = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// initialise
		running = false;
		cycleTime = CYCLE_TIME_DEFAULT;
		activateClusterThreshold = 2;
		innerFeedback = false;
		iterationCount = 0;
		iterationCountStop = 0;
		fileSave = null;
		fileLoad = null;
		fileInsert = null;

		resume();
	}

	public long getCycleTime() {
		return cycleTime;
	}

	public void setCycleTime(long cycleTime) {
		this.cycleTime = cycleTime;
	}

	public long getCycleDuration() {
		return cycleDuration;
	}

	public long getIterationCount() {
		return iterationCount;
	}

	public long getIterationCountStop() {
		return iterationCountStop;
	}

	public void setIterationCountStop(long iterationCountStop) {
		this.iterationCountStop = iterationCountStop;
	}

	public SOINNM getTopology() {
		return topology;
	}

	public HashMap<Cluster, Double> getActivationMap() {
		return activationMap;
	}

	public double getActivationOutput() {
		return activationOutput;
	}

	public int getActivateClusterThreshold() {
		return activateClusterThreshold;
	}

	public void setActivateClusterThreshold(int activateClusterThreshold) {
		this.activateClusterThreshold = activateClusterThreshold;
	}

	public boolean isInnerFeedback() {
		return innerFeedback;
	}

	public void setInnerFeedback(boolean innerFeedback) {
		this.innerFeedback = innerFeedback;
	}

	public void pause() {
		running = false;
	}

	public void resume() {
		running = true;
	}

	protected abstract double[] readInput();

	protected String readLabel() {
		return "";
	}

	private void sendActivatedCluster(long clusterId) {

		// send activated cluster
		try {
			String strData = name + " " + clusterId;
			byte[] data = strData.getBytes();
			InetAddress hostAddress = InetAddress.getByName(hostName);
			DatagramPacket packet = new DatagramPacket(data, data.length, hostAddress, UDP_PORT_SEND);
			socketSend.send(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void sendSplitClusters(long clusterIdOld, long clusterIdNew) {

		// send old and new clusters
		try {
			String strData = name + " COMMAND SPLITDATA " + clusterIdOld + " " + clusterIdNew;
			byte[] data = strData.getBytes();
			InetAddress hostAddress = InetAddress.getByName(hostName);
			DatagramPacket packet = new DatagramPacket(data, data.length, hostAddress, UDP_PORT_SEND);
			socketSend.send(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void sendJoinedClusters(long clusterIdKept, long clusterIdRemoved) {

		// send kept and removed clusters
		try {
			String strData = name + " COMMAND JOINDATA " + clusterIdKept + " " + clusterIdRemoved;
			byte[] data = strData.getBytes();
			InetAddress hostAddress = InetAddress.getByName(hostName);
			DatagramPacket packet = new DatagramPacket(data, data.length, hostAddress, UDP_PORT_SEND);
			socketSend.send(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void sendRemovedCluster(long clusterId) {

		// send removed cluster
		try {
			String strData = name + " COMMAND DELETEDATA " + clusterId;
			byte[] data = strData.getBytes();
			InetAddress hostAddress = InetAddress.getByName(hostName);
			DatagramPacket packet = new DatagramPacket(data, data.length, hostAddress, UDP_PORT_SEND);
			socketSend.send(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private LinkedList<String> receiveActivations() {
		LinkedList<String> responses = new LinkedList<String>();

		// prepare request
		String request = name;

		try {

			// send request
			byte[] requestData = request.getBytes();
			InetAddress hostAddress = InetAddress.getByName(hostName);
			DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, hostAddress, UDP_PORT_RECEIVE);
			socketReceive.send(requestPacket);

			// receive response
			byte[] responseData = new byte[PACKET_SIZE];
			DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
			socketReceive.setSoTimeout(RECEIVE_TIMEOUT);
			socketReceive.receive(responsePacket);
			String response = new String(responseData).trim();

			// prepare responses
			StringTokenizer responseTokenizer = new StringTokenizer(response, "\n");
			while (responseTokenizer.hasMoreTokens()) {
				String line = responseTokenizer.nextToken();
				if (!line.equals("")) {
					responses.add(line);
				}
			}

		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println("No activation values received");
		}

		return responses;
	}

	private double[] computeOutput(LinkedList<String> responses) {

		// initialise
		activationOutput = 0.0;
		boolean outputComputed = false;
		double activationMax = 0.0;
		double activationSum = 0.0;
		activationMap.clear();

		// compute output
		double[] pattern = new double[topology.getInputDim()];
		for (String response : responses) {

			// parse response
			String[] responseSplit = response.split(" ");
			long clusterId = Long.valueOf(responseSplit[1]);
			double activation = Double.valueOf(responseSplit[2]);

			// retrieve cluster
			Cluster cluster = null;
			for (Cluster c : topology.getClusterSet()) {
				if (c.getId() == clusterId) {
					cluster = c;
				}
			}
			if (cluster == null)
				continue;

			// add mean to pattern
			double[] mean = cluster.getMean();
			for (int i = 0; i < pattern.length; i++) {
				pattern[i] += activation * mean[i];
			}
			activationSum += activation;

			// output contains an actual pattern
			outputComputed = true;

			// update activation map
			activationMap.put(cluster, activation);

			// update maximum activation
			if (activation > activationMax) {
				activationMax = activation;
			}

		}

		// check if output has been computed
		if (outputComputed) {

			// normalise pattern
			for (int i = 0; i < pattern.length; i++) {
				pattern[i] /= activationSum;
			}

			// set activation for output
			//activationOutput = activationSum / responses.size();
			activationOutput = activationMax;

			return pattern;
		}

		return null;
	}

	protected abstract void writeOutput(double[] pattern);

	private void step() {

		// process input

		// read input pattern
		double[] patternInput = readInput();
		if (patternInput != null) {

			// read label
			String label = readLabel();

			// learn topology
			topology.input(patternInput, label);

			// send activated cluster
			Cluster activatedCluster = topology.getActivatedCluster();
			if (activatedCluster != null && activatedCluster.getNodes().size() >= activateClusterThreshold) {
				sendActivatedCluster(activatedCluster.getId());
			}

			// send split clusters
			LinkedList<Cluster> splitClusters = topology.getSplitClusters();
			Iterator<Cluster> itSplitClusters = splitClusters.iterator();
			while (itSplitClusters.hasNext()) {
				Cluster clusterOld = itSplitClusters.next();
				Cluster clusterNew = itSplitClusters.next();
				sendSplitClusters(clusterOld.getId(), clusterNew.getId());
			}
			splitClusters.clear();

			// send joined clusters
			LinkedList<Cluster> joinedClusters = topology.getJoinedClusters();
			Iterator<Cluster> itJoinedClusters = joinedClusters.iterator();
			while (itJoinedClusters.hasNext()) {
				Cluster clusterKept = itJoinedClusters.next();
				Cluster clusterRemoved = itJoinedClusters.next();
				sendJoinedClusters(clusterKept.getId(), clusterRemoved.getId());
			}
			joinedClusters.clear();

			// send removed clusters
			LinkedList<Cluster> removedClusters = topology.getRemovedClusters();
			for (Cluster cluster : removedClusters) {
				sendRemovedCluster(cluster.getId());
			}
			removedClusters.clear();

		}

		// process output

		// receive activations
		LinkedList<String> activations = receiveActivations();

		// write output pattern
		double[] patternOutput = null;
		if (activations.size() > 0) {
			patternOutput = computeOutput(activations);
		}
		writeOutput(patternOutput);

		// inner feedback
		if (innerFeedback && patternOutput != null) {
			double[] patternFeedback = patternOutput.clone();
			topology.input(patternFeedback);
		}

	}

	@Override
	public void run() {

		// initialise
		long cycleStart = 0;
		long cycleEnd = 0;
		cycleDuration = 0;

		while (true) {

			if (running) {

				// measure time
				cycleStart = System.currentTimeMillis();

				// increase iteration count
				iterationCount++;

				// simulate step
				step();

				// update user interface
				updateUI();

				// save topology
				if (fileSave != null) {
					XMLWriter xmlWriter = new XMLWriter();
					xmlWriter.writeToFile(topology, fileSave.getAbsolutePath());
					fileSave = null;
				}

				// load topology
				if (fileLoad != null) {
					XMLReader xmlReader = new XMLReader();
					xmlReader.readFromFile(fileLoad.getAbsolutePath(), topology);
					fileLoad = null;
				}

				// insert topology
				if (fileInsert != null) {
					XMLReader xmlReader = new XMLReader();
					xmlReader.insertFromFile(fileInsert.getAbsolutePath(), topology);
					fileInsert = null;
				}

				// measure time
				cycleEnd = System.currentTimeMillis();
				cycleDuration = cycleEnd - cycleStart;

				// wait until cycle is finished
				if (cycleDuration < cycleTime) {
					try {
						Thread.sleep(cycleTime - cycleDuration);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.err.println("Cycle time violation with duration " + cycleDuration + " ms");
				}

				// check for stop condition
				if (iterationCountStop > 0 && iterationCount >= iterationCountStop) {
					break;
				}

			}

			else {

				try {
					Thread.sleep(cycleTime);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}

		shutdown();
	}

	protected abstract void updateUI();

	protected abstract void shutdown();

}
