package soinnm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;
import soinnm.XMLReader;
import soinnm.XMLWriter;

/**
 * The M-SOINN algorithm.
 * 
 * Allows to input a new data pattern (optionally with text label assigned to
 * it). Processes the input and updates the topology accordingly. Manages unique
 * ids for nodes, edges and clusters.
 * 
 * Provides methods for getting and setting the various parameters of the
 * algorithm.
 * 
 * @author Matthias Keysermann
 *
 */
public class SOINNM {

	private boolean printDebugInfo = false;

	private Random random;

	private int inputDim;

	private LinkedList<Node> nodeSet;

	private long nextNodeId;

	private LinkedList<Edge> edgeSet;

	private long nextEdgeId;

	private LinkedList<Cluster> clusterSet;

	private long nextClusterId;

	private long numInputs; // number of inputs since last clean up

	private Cluster activatedCluster; // MK: cluster of current input

	private LinkedList<Cluster> splitClusters; // MK: split clusters (first old cluster, second new cluster)

	private LinkedList<Cluster> joinedClusters; // MK: joined clusters (first kept cluster, second removed cluster)

	private LinkedList<Cluster> removedClusters; // MK: removed clusters

	// parameters

	private double noiseLevel = 0.0; // MK: amount of noise added to input patterns

	private boolean useFixedThreshold = false; // MK: use a fixed similarity threshold

	private double fixedThreshold = 0.1; // MK: fixed similarity threshold

	private double minimumThreshold = 0.0; // MK: use minimum limit for dynamic similarity threshold

	private int ageDead = 100;

	private boolean connectNewNodes = false; // MK: allow connections from newly created nodes to existing nodes

	private int lambda = 25;

	private boolean edgeMaxRemoval = true; // MK: remove edge with maximum length

	private boolean nodeNumSignalsMinRemoval = false; // MK: remove node with lowest density

	private boolean reduceErrorInsertion = false; // insert node to reduce error

	private boolean smallClusterRemoval = true; // remove nodes with low number of neighbours

	private double c2Param = 0.001; // 0.001

	private double c1Param = 0.1; // 1.0

	private boolean clusterJoining = true; // MK: join clusters with minimum distance

	private double joinTolerance = 1.0; // MK: tolerance for comparing minimum distance with average distance

	private boolean useAbsoluteJoinTolerance = false; // MK: use absolute join tolerance value per dimension

	private double joinToleranceAbsolute = 0.1; // MK: absolute join tolerance value

	private int joiningIterationsMax = 10; // MK: limit number of iterations for cluster joining

	public SOINNM(int inputDim) {

		// set dimensionality of input
		this.inputDim = inputDim;

		// initialise
		random = new Random();

		// initialise node set
		nodeSet = new LinkedList<Node>();
		nextNodeId = 1;

		// initialise edge set
		edgeSet = new LinkedList<Edge>();
		nextEdgeId = 1;

		// initialise cluster set
		clusterSet = new LinkedList<Cluster>();
		nextClusterId = 1;

		// initialise number of inputs
		numInputs = 0;

		// MK: initialise activated cluster
		activatedCluster = null;

		// MK: initialise split clusters;
		splitClusters = new LinkedList<Cluster>();

		// MK: initialise joined clusters;
		joinedClusters = new LinkedList<Cluster>();

		// MK: initialise removed clusters
		removedClusters = new LinkedList<Cluster>();

	}

	// variables getters/setters

	public boolean isPrintDebugInfo() {
		return printDebugInfo;
	}

	public void setPrintDebugInfo(boolean printDebugInfo) {
		this.printDebugInfo = printDebugInfo;
	}

	public int getInputDim() {
		return inputDim;
	}

	public LinkedList<Node> getNodeSet() {
		return nodeSet;
	}

	public long getNextNodeId() {
		return nextNodeId;
	}

	public void setNextNodeId(long nextNodeId) {
		this.nextNodeId = nextNodeId;
	}

	public LinkedList<Edge> getEdgeSet() {
		return edgeSet;
	}

	public long getNextEdgeId() {
		return nextEdgeId;
	}

	public void setNextEdgeId(long nextEdgeId) {
		this.nextEdgeId = nextEdgeId;
	}

	public LinkedList<Cluster> getClusterSet() {
		return clusterSet;
	}

	public long getNextClusterId() {
		return nextClusterId;
	}

	public void setNextClusterId(long nextClusterId) {
		this.nextClusterId = nextClusterId;
	}

	public long getNumInputs() {
		return numInputs;
	}

	public void setNumInputs(long numInputs) {
		this.numInputs = numInputs;
	}

	public Cluster getActivatedCluster() {
		return activatedCluster;
	}

	public LinkedList<Cluster> getSplitClusters() {
		return splitClusters;
	}

	public LinkedList<Cluster> getJoinedClusters() {
		return joinedClusters;
	}

	public LinkedList<Cluster> getRemovedClusters() {
		return removedClusters;
	}

	// parameter getters/setters

	public double getNoiseLevel() {
		return noiseLevel;
	}

	public void setNoiseLevel(double noiseLevel) {
		this.noiseLevel = noiseLevel;
	}

	public boolean isUseFixedThreshold() {
		return useFixedThreshold;
	}

	public void setUseFixedThreshold(boolean useFixedThreshold) {
		this.useFixedThreshold = useFixedThreshold;
	}

	public double getFixedThreshold() {
		return fixedThreshold;
	}

	public void setFixedThreshold(double fixedThreshold) {
		this.fixedThreshold = fixedThreshold;
	}

	public double getMinimumThreshold() {
		return minimumThreshold;
	}

	public void setMinimumThreshold(double minimumThreshold) {
		this.minimumThreshold = minimumThreshold;
	}

	public int getAgeDead() {
		return ageDead;
	}

	public void setAgeDead(int ageDead) {
		this.ageDead = ageDead;
	}

	public boolean isConnectNewNodes() {
		return connectNewNodes;
	}

	public void setConnectNewNodes(boolean connectNewNodes) {
		this.connectNewNodes = connectNewNodes;
	}

	public int getLambda() {
		return lambda;
	}

	public void setLambda(int lambda) {
		this.lambda = lambda;
	}

	public boolean isEdgeMaxRemoval() {
		return edgeMaxRemoval;
	}

	public void setEdgeMaxRemoval(boolean edgeMaxRemoval) {
		this.edgeMaxRemoval = edgeMaxRemoval;
	}

	public boolean isNodeNumSignalsMinRemoval() {
		return nodeNumSignalsMinRemoval;
	}

	public void setNodeNumSignalsMinRemoval(boolean nodeNumSignalsMinRemoval) {
		this.nodeNumSignalsMinRemoval = nodeNumSignalsMinRemoval;
	}

	public boolean isReduceErrorInsertion() {
		return reduceErrorInsertion;
	}

	public void setReduceErrorInsertion(boolean reduceErrorInsertion) {
		this.reduceErrorInsertion = reduceErrorInsertion;
	}

	public boolean isSmallClusterRemoval() {
		return smallClusterRemoval;
	}

	public void setSmallClusterRemoval(boolean smallClusterRemoval) {
		this.smallClusterRemoval = smallClusterRemoval;
	}

	public double getC2Param() {
		return c2Param;
	}

	public void setC2Param(double c2Param) {
		this.c2Param = c2Param;
	}

	public double getC1Param() {
		return c1Param;
	}

	public void setC1Param(double c1Param) {
		this.c1Param = c1Param;
	}

	public boolean isClusterJoining() {
		return clusterJoining;
	}

	public void setClusterJoining(boolean clusterJoining) {
		this.clusterJoining = clusterJoining;
	}

	public double getJoinTolerance() {
		return joinTolerance;
	}

	public void setJoinTolerance(double joinTolerance) {
		this.joinTolerance = joinTolerance;
	}

	public boolean isUseAbsoluteJoinTolerance() {
		return useAbsoluteJoinTolerance;
	}

	public void setUseAbsoluteJoinTolerance(boolean useAbsoluteJoinTolerance) {
		this.useAbsoluteJoinTolerance = useAbsoluteJoinTolerance;
	}

	public double getJoinToleranceAbsolute() {
		return joinToleranceAbsolute;
	}

	public void setJoinToleranceAbsolute(double joinToleranceAbsolute) {
		this.joinToleranceAbsolute = joinToleranceAbsolute;
	}

	public int getJoiningIterationsMax() {
		return joiningIterationsMax;
	}

	public void setJoiningIterationsMax(int joiningIterationsMax) {
		this.joiningIterationsMax = joiningIterationsMax;
	}

	public void clear() {
		nodeSet.clear();
		edgeSet.clear();
		clusterSet.clear();
		numInputs = 0;
		activatedCluster = null;
		removedClusters.clear();
		joinedClusters.clear();
		splitClusters.clear();
	}

	public void createRandomNodes(int number) {
		for (int n = 0; n < number; n++) {
			double[] pattern = new double[inputDim];
			for (int i = 0; i < inputDim; i++) {
				pattern[i] = random.nextDouble();
			}
			Node node = addNode(pattern);
			node.setNumSignals(1);
		}
	}

	public void input(double[] pattern) {
		input(pattern, "");
	}

	public void input(double[] pattern, String label) {

		// check dimensionality of input pattern
		if (pattern.length != inputDim) {
			System.err.println("Input pattern has wrong dimensionality!");
			return;
		}

		// MK: add noise to pattern
		if (noiseLevel > 0) {
			for (int i = 0; i < inputDim; i++) {
				pattern[i] += random.nextGaussian() * noiseLevel;
			}
		}

		// MK: unset cluster means and prototypes
		for (Cluster cluster : clusterSet) {
			cluster.unsetMean();
			cluster.unsetWeightedMean();
			cluster.unsetPrototype();
		}

		// MK: reset activated cluster
		activatedCluster = null;

		// MK: reset split clusters
		splitClusters.clear();

		// MK: reset joined clusters
		joinedClusters.clear();

		// MK: reset removed clusters
		removedClusters.clear();

		// increase number of inputs
		numInputs += 1;

		// MK: assert a minimum of two nodes
		if (nodeSet.size() < 2) {

			// create node
			Node node = addNode(pattern);

			// set label
			node.setLabel(label);

			// MK: initialise number of signals to 1
			node.setNumSignals(1);

			// set activated cluster
			activatedCluster = getCluster(node);

			return;

		}

		// initialise distance
		double distance;

		// find nearest node s1
		Node nodeS1 = null;
		double distanceS1 = Double.MAX_VALUE;
		for (Node node : nodeSet) {
			distance = computeDistance(pattern, node.getPattern());
			if (distance < distanceS1) {
				nodeS1 = node;
				distanceS1 = distance;
			}
		}

		// find second nearest node s2
		Node nodeS2 = null;
		double distanceS2 = Double.MAX_VALUE;
		for (Node node : nodeSet) {
			if (node != nodeS1) {
				distance = computeDistance(pattern, node.getPattern());
				if (distance < distanceS2) {
					nodeS2 = node;
					distanceS2 = distance;
				}
			}
		}

		// update threshold of node s1
		updateThreshold(nodeS1);

		// update threshold of node s2
		updateThreshold(nodeS2);

		// check whether distances to nodes s1 and s2 are greater than respective thresholds
		if (distanceS1 > nodeS1.getThreshold() || distanceS2 > nodeS2.getThreshold()) {

			// create node
			Node node = addNode(pattern);

			// set label
			node.setLabel(label);

			// MK: initialise number of signals to 1
			node.setNumSignals(1);

			// MK: connect new node to an exisiting node
			if (connectNewNodes) {
				if (distanceS1 <= nodeS1.getThreshold()) {
					boolean connectNewNode = true;

					// connect only if the new node would extend the cluster
					Cluster clusterS1 = getCluster(nodeS1);
					double[] meanS1 = clusterS1.getMean();
					double distanceMeanS1 = computeDistance(meanS1, nodeS1.getPattern());
					double distanceMeanNode = computeDistance(meanS1, node.getPattern());
					if (distanceMeanS1 > distanceMeanNode) {
						connectNewNode = false;
					}

					// connect only if the distance between node and S1 is greater than average distance
					double distanceSum = 0.0;
					int distanceSumCount = 0;
					LinkedList<Node> nodesClusterS1 = clusterS1.getNodes();
					for (Node nodeA : nodesClusterS1) {
						for (Node nodeB : nodesClusterS1) {
							if (nodeA != nodeB) {
								distanceSum += computeDistance(nodeA.getPattern(), nodeB.getPattern());
								distanceSumCount += 1;
							}
						}
					}
					double distanceAvg = distanceSum / distanceSumCount;
					if (distanceS1 < distanceAvg) {
						connectNewNode = false;
					}

					if (connectNewNode) {
						addEdge(node, nodeS1);
						// DEBUG
						if (printDebugInfo) {
							System.out.println("CONNECTED NEW NODE " + node.getId() + " TO NODE " + nodeS1.getId());
						}
					}

				}
			}

			// set activated cluster
			activatedCluster = getCluster(node);

			// this can prevent clean up
			//return;

		}

		else {

			// increment age of edges with node s1
			for (Edge edge : edgeSet) {
				if (edge.getNodeA() == nodeS1 || edge.getNodeB() == nodeS1) {
					edge.setAge(edge.getAge() + 1);
				}
			}

			// check whether no edge between nodes s1 and s2 exists
			Edge edgeS1S2 = null;
			Node nodeA;
			Node nodeB;
			for (Edge edge : edgeSet) {
				nodeA = edge.getNodeA();
				nodeB = edge.getNodeB();
				if ((nodeA == nodeS1 && nodeB == nodeS2) || (nodeA == nodeS2 && nodeB == nodeS1)) {
					edgeS1S2 = edge;
					break;
				}
			}

			// create edge between nodes s1 and s2
			if (edgeS1S2 == null) {
				edgeS1S2 = addEdge(nodeS1, nodeS2);
			}

			// reset age of edge between nodes s1 and s2
			edgeS1S2.setAge(0);

			// MK: increase error of node s1
			nodeS1.setError(nodeS1.getError() + distanceS1);

			// increment number of signals of node s1
			nodeS1.setNumSignals(nodeS1.getNumSignals() + 1);

			// adjust pattern of node s1
			double epsilon1 = 1.0 / nodeS1.getNumSignals();
			double[] patternS1 = nodeS1.getPattern();
			for (int i = 0; i < inputDim; i++) {
				patternS1[i] += epsilon1 * (pattern[i] - patternS1[i]);
			}

			// adjust pattern of direct neighbours of node s1		
			double epsilon2 = 0.01 / nodeS1.getNumSignals();
			for (Node neighbour : getNeighbours(nodeS1)) {
				double[] patternNeighbour = neighbour.getPattern();
				for (int i = 0; i < inputDim; i++) {
					patternNeighbour[i] += epsilon2 * (pattern[i] - patternNeighbour[i]);
				}
			}

			// remove edges with age greater than ageDead
			Iterator<Edge> itEdgeSet = edgeSet.iterator();
			while (itEdgeSet.hasNext()) {
				Edge edge = itEdgeSet.next();
				if (edge.getAge() > ageDead) {
					itEdgeSet.remove();
					removeEdge(edge);
					// DEBUG
					if (printDebugInfo) {
						System.out.println("REMOVED AGE DEAD EDGE " + edge.getId());
					}
				}
			}

			// set activated cluster
			activatedCluster = getCluster(nodeS1);

		}

		if (numInputs >= lambda) {

			// DEBUG
			if (printDebugInfo) {
				System.out.println("CLEAN UP");
			}

			// reset number of inputs
			numInputs = 0;

			if (edgeMaxRemoval) {

				// initialise
				double edgeLength;
				double edgeLengthMax = Double.MIN_VALUE;
				Edge edgeMax = null;

				// find edge with maximum length
				for (Edge edge : edgeSet) {
					edgeLength = computeDistance(edge.getNodeA().getPattern(), edge.getNodeB().getPattern());
					if (edgeLength > edgeLengthMax) {
						edgeMax = edge;
						edgeLengthMax = edgeLength;
					}

				}

				// remove edge with maximum length
				if (edgeMax != null) {
					removeEdge(edgeMax);
					// DEBUG
					if (printDebugInfo) {
						System.out.println("REMOVED LONGEST EDGE " + edgeMax.getId());
					}
				}

			}

			if (nodeNumSignalsMinRemoval) {

				// initialise
				long numSignals;
				long numSignalsMin = Long.MAX_VALUE;
				Node nodeNumSignalsMin = null;

				// find node with minimum number of signals
				for (Node node : nodeSet) {
					numSignals = node.getNumSignals();
					if (numSignals < numSignalsMin) {
						nodeNumSignalsMin = node;
						numSignalsMin = numSignals;
					}
				}

				// remove node with minimum number of signals
				if (nodeNumSignalsMin != null) {
					removeNode(nodeNumSignalsMin);
					// DEBUG
					if (printDebugInfo) {
						System.out.println("REMOVED MINIMUM SIGNALS NODE " + nodeNumSignalsMin.getId());
					}
				}

			}

			if (reduceErrorInsertion) {

				// initialise
				double error;
				double errorMax;

				// find node q with maximum error
				Node nodeQ = null;
				errorMax = 0.0;
				for (Node node : nodeSet) {
					error = node.getError();
					if (error > errorMax) {
						nodeQ = node;
						errorMax = error;
					}
				}

				// find neighbour f of node q with maximum error
				Node nodeF = null;
				errorMax = 0.0;
				for (Node node : getNeighbours(nodeQ)) {
					error = node.getError();
					if (error > errorMax) {
						nodeF = node;
						errorMax = error;
					}
				}

				if (nodeF != null) {

					// create node r
					double[] patternR = new double[inputDim];
					double[] patternQ = nodeQ.getPattern();
					double[] patternF = nodeF.getPattern();
					for (int i = 0; i < inputDim; i++) {
						patternR[i] = (patternQ[i] + patternF[i]) / 2;
					}
					Node nodeR = addNode(patternR);

					// MK: copy label
					nodeR.setLabel(nodeQ.getLabel());

					// create edge between nodes q and r
					addEdge(nodeQ, nodeR);

					// create edge between nodes r and f
					addEdge(nodeR, nodeF);

					// remove edge between nodes q and f
					for (Edge edge : edgeSet) {
						if ((edge.getNodeA() == nodeQ && edge.getNodeB() == nodeF) || (edge.getNodeA() == nodeF && edge.getNodeB() == nodeQ)) {
							removeEdge(edge);
							break;
						}
					}

					// decrease error of q and f					
					nodeQ.setError(nodeQ.getError() * 0.5);
					nodeF.setError(nodeF.getError() * 0.5);

					// set error of r
					nodeR.setError(nodeQ.getError());

					// MK: set number of signals of r
					nodeR.setNumSignals(nodeQ.getNumSignals());

					// DEBUG
					if (printDebugInfo) {
						System.out.println("INSERTED NODE BETWEEN NODES " + nodeF.getId() + " AND " + nodeQ.getId());
					}

				}

			}

			if (smallClusterRemoval) {

				// compute average number of signals
				long numSignalsSum = 0;
				for (Node node : nodeSet) {
					numSignalsSum += node.getNumSignals();
				}
				double numSignalsAvg = (double) numSignalsSum / nodeSet.size();

				// initialise
				LinkedList<Node> nodesToRemove;

				// search for nodes with two neighbours
				nodesToRemove = new LinkedList<Node>();
				for (Node node : nodeSet) {
					int numNeighbours = getNeighbours(node).size();
					if (numNeighbours == 2) {
						if (node.getNumSignals() < c2Param * numSignalsAvg) {
							nodesToRemove.add(node);
						}
					}
				}

				// remove nodes with two neighbours
				for (Node node : nodesToRemove) {
					removeNode(node);
					// DEBUG
					if (printDebugInfo) {
						System.out.println("REMOVED TWO NEIGHBOURS NODE " + node.getId());
					}
				}

				// search for nodes with one neighbour
				nodesToRemove = new LinkedList<Node>();
				for (Node node : nodeSet) {
					int numNeighbours = getNeighbours(node).size();
					if (numNeighbours == 1) {
						if (node.getNumSignals() < c1Param * numSignalsAvg) {
							nodesToRemove.add(node);
						}
					}
				}

				// remove nodes with one neighbour
				for (Node node : nodesToRemove) {
					removeNode(node);
					// DEBUG
					if (printDebugInfo) {
						System.out.println("REMOVED ONE NEIGHBOUR NODE " + node.getId());
					}
				}

				// search for isolated nodes
				nodesToRemove = new LinkedList<Node>();
				for (Node node : nodeSet) {
					int numNeighbours = getNeighbours(node).size();
					if (numNeighbours == 0) {
						nodesToRemove.add(node);
					}
				}

				// remove isolated nodes
				for (Node node : nodesToRemove) {
					removeNode(node);
					// DEBUG
					if (printDebugInfo) {
						System.out.println("REMOVED ISOLATED NODE " + node.getId());
					}
				}

			}

			if (clusterJoining) {

				boolean continueJoining = true;
				int joiningIterations = 0;

				while (clusterSet.size() > 1 && continueJoining && joiningIterations < joiningIterationsMax) {

					// initialise
					double distanceMin = Double.MAX_VALUE;
					Cluster clusterMinA = null;
					Cluster clusterMinB = null;
					Node nodeMinA = null;
					Node nodeMinB = null;

					// determine minimum node distance between clusters
					for (Cluster clusterA : clusterSet) {
						for (Cluster clusterB : clusterSet) {
							if (clusterA != clusterB) {
								for (Node nodeA : clusterA.getNodes()) {
									for (Node nodeB : clusterB.getNodes()) {
										distance = computeDistance(nodeA.getPattern(), nodeB.getPattern());
										if (distance < distanceMin) {
											clusterMinA = clusterA;
											clusterMinB = clusterB;
											nodeMinA = nodeA;
											nodeMinB = nodeB;
											distanceMin = distance;
										}
									}
								}
							}
						}
					}

					// check whether the clusters should be joined
					boolean joinClusters = false;

					if (useAbsoluteJoinTolerance) {

						// absolute distance
						if (distanceMin < Math.sqrt(joinToleranceAbsolute * joinToleranceAbsolute * inputDim)) {
							joinClusters = true;
						}

					} else {

						// calculate average node distance for cluster A
						double distanceSumA = 0.0;
						int distanceCountA = 0;
						for (Node nodeA : clusterMinA.getNodes()) {
							for (Node nodeB : clusterMinA.getNodes()) {
								if (nodeA != nodeB) {
									distanceSumA += computeDistance(nodeA.getPattern(), nodeB.getPattern());
									distanceCountA += 1;
								}
							}
						}
						double distanceAvgA = distanceSumA / distanceCountA;

						// calculate average node distance for cluster B
						double distanceSumB = 0.0;
						int distanceCountB = 0;
						for (Node nodeA : clusterMinB.getNodes()) {
							for (Node nodeB : clusterMinB.getNodes()) {
								if (nodeA != nodeB) {
									distanceSumB += computeDistance(nodeA.getPattern(), nodeB.getPattern());
									distanceCountB += 1;
								}
							}
						}
						double distanceAvgB = distanceSumB / distanceCountB;

						// depending on average node distance
						if (distanceMin < distanceAvgA * joinTolerance && distanceMin < distanceAvgB * joinTolerance) {
							joinClusters = true;
						}

					}

					if (joinClusters) {

						// create edge to join clusters
						addEdge(nodeMinA, nodeMinB);

						// DEBUG
						if (printDebugInfo) {
							System.out.println("JOINED CLUSTERS " + clusterMinA.getId() + " AND " + clusterMinB.getId());
						}

					} else {

						// stop cluster joining
						continueJoining = false;

					}

					// increase number of joining iterations
					joiningIterations++;

				}
			}

		}
	}

	private Node addNode(double[] pattern) {

		// create node
		Node node = new Node(nextNodeId++, pattern);
		nodeSet.add(node);

		// create cluster
		LinkedList<Node> nodes = new LinkedList<Node>();
		nodes.add(node);
		addCluster(nodes);

		return node;
	}

	private boolean removeNode(Node node) {

		// remove node
		nodeSet.remove(node);

		// remove edges
		Iterator<Edge> itEdgeSet = edgeSet.iterator();
		while (itEdgeSet.hasNext()) {
			Edge edge = itEdgeSet.next();
			if (edge.getNodeA() == node || edge.getNodeB() == node) {
				itEdgeSet.remove();
				removeEdge(edge);
			}
		}

		// remove node from cluster
		Cluster cluster = getCluster(node);
		cluster.getNodes().remove(node);

		// remove empty clusters
		if (cluster.getNodes().isEmpty()) {
			removeCluster(cluster);
		}

		return true;
	}

	private Edge addEdge(Node nodeA, Node nodeB) {

		// create edge
		Edge edge = new Edge(nextEdgeId++, nodeA, nodeB);
		edgeSet.add(edge);

		// check whether clusters are different
		Cluster clusterA = getCluster(nodeA);
		Cluster clusterB = getCluster(nodeB);
		if (clusterA != clusterB) {

			// join clusters	
			LinkedList<Node> nodesA = clusterA.getNodes();
			LinkedList<Node> nodesB = clusterB.getNodes();
			if (nodesA.size() > nodesB.size()) {

				// move nodes of cluster B to cluster A
				nodesA.addAll(nodesB);

				// remove cluster B
				removeCluster(clusterB);

				// add to joined clusters
				joinedClusters.add(clusterA); // first kept cluster
				joinedClusters.add(clusterB); // second removed cluster

			} else {

				// move nodes of cluster A to cluster B
				nodesB.addAll(nodesA);

				// remove cluster A
				removeCluster(clusterA);

				// add to joined clusters
				joinedClusters.add(clusterB); // first kept cluster
				joinedClusters.add(clusterA); // second removed cluster

			}

		}

		return edge;
	}

	private boolean removeEdge(Edge edge) {

		// remove edge
		edgeSet.remove(edge);

		// check whether no other connection exists
		Node nodeA = edge.getNodeA();
		Node nodeB = edge.getNodeB();
		LinkedList<Node> connectedNodesA = getConnectedNodes(nodeA);
		LinkedList<Node> connectedNodesB = getConnectedNodes(nodeB);
		if (!connectedNodesA.contains(nodeB)) {

			// split cluster			
			if (connectedNodesA.size() > connectedNodesB.size()) {

				// keep cluster of node A
				Cluster clusterA = getCluster(nodeA);
				clusterA.getNodes().removeAll(connectedNodesB);

				// create cluster for connected nodes of node B
				Cluster clusterB = addCluster(connectedNodesB);

				// add to split clusters
				splitClusters.add(clusterA);
				splitClusters.add(clusterB);

			} else {

				// keep cluster of node B
				Cluster clusterB = getCluster(nodeB);
				clusterB.getNodes().removeAll(connectedNodesA);

				// create cluster for connected nodes of node A
				Cluster clusterA = addCluster(connectedNodesA);

				// add to split clusters
				splitClusters.add(clusterB);
				splitClusters.add(clusterA);

			}
		}

		return true;
	}

	private Cluster addCluster(LinkedList<Node> nodes) {

		// create cluster
		Cluster cluster = new Cluster(nextClusterId++, nodes);
		clusterSet.add(cluster);

		return cluster;
	}

	private boolean removeCluster(Cluster cluster) {

		// remove cluster
		clusterSet.remove(cluster);

		// add cluster to removed clusters
		removedClusters.add(cluster);

		return true;
	}

	private double computeDistance(double[] A, double[] B) {
		return euclideanDistance(A, B);
	}

	private double euclideanDistance(double[] A, double[] B) {
		double sum = 0.0;
		double difference;

		for (int i = 0; i < inputDim; i++) {
			difference = A[i] - B[i];
			sum += difference * difference;
		}

		return Math.sqrt(sum);
	}

	@SuppressWarnings("unused")
	private double cosineSimilarity(double[] A, double[] B) {
		double dotProduct = 0.0;
		double lengthA = 0.0;
		double lengthB = 0.0;

		for (int i = 0; i < inputDim; i++) {
			dotProduct += A[i] * B[i];
			lengthA += A[i] * A[i];
			lengthB += B[i] * B[i];
		}
		lengthA = Math.sqrt(lengthA);
		lengthB = Math.sqrt(lengthB);

		double cosineSimilarity = dotProduct / (lengthA * lengthB);

		return cosineSimilarity;
	}

	private void updateThreshold(Node node) {

		if (useFixedThreshold) {

			double distanceFixed = Math.sqrt(fixedThreshold * fixedThreshold * inputDim);
			node.setThreshold(distanceFixed);

		} else {

			LinkedList<Node> neighbours = getNeighbours(node);
			double[] pattern = node.getPattern();
			double distance;

			if (neighbours.size() > 0) {

				// determine maximum distance to all neighbours
				double distanceMax = Double.MIN_VALUE;
				for (Node neighbour : neighbours) {
					distance = computeDistance(pattern, neighbour.getPattern());
					if (distance > distanceMax) {
						distanceMax = distance;
					}
				}

				// set threshold
				node.setThreshold(distanceMax);

			} else {

				// determine minimum distance to all other nodes
				double distanceMin = Double.MAX_VALUE;
				for (Node nodeCurrent : nodeSet) {
					if (nodeCurrent != node) {
						distance = computeDistance(pattern, nodeCurrent.getPattern());
						if (distance < distanceMin) {
							distanceMin = distance;
						}
					}
				}

				// set threshold
				node.setThreshold(distanceMin);

			}

			// check for minimum threshold
			if (node.getThreshold() < minimumThreshold) {
				node.setThreshold(minimumThreshold);
			}
		}

	}

	private LinkedList<Node> getNeighbours(Node node) {
		LinkedList<Node> neighbours = new LinkedList<Node>();

		for (Edge edge : edgeSet) {
			if (edge.getNodeA() == node) {
				neighbours.add(edge.getNodeB());
			} else if (edge.getNodeB() == node) {
				neighbours.add(edge.getNodeA());
			}
		}

		return neighbours;
	}

	public Cluster getCluster(Node node) {

		for (Cluster cluster : clusterSet) {
			for (Node clusterNode : cluster.getNodes()) {
				if (clusterNode == node) {
					return cluster;
				}
			}
		}

		return null;
	}

	private LinkedList<Node> getConnectedNodes(Node node) {
		LinkedList<Node> connectedNodes = new LinkedList<Node>();
		connectedNodes.add(node);
		followConnectedNodes(node, connectedNodes);
		return connectedNodes;
	}

	private void followConnectedNodes(Node node, LinkedList<Node> connectedNodes) {
		LinkedList<Node> neighbours = getNeighbours(node);
		for (Node neighbour : neighbours) {
			if (!connectedNodes.contains(neighbour)) {
				connectedNodes.add(neighbour);
				followConnectedNodes(neighbour, connectedNodes);
			}
		}
	}

	public String getInfoParameters() {
		String infoParameters = "";
		infoParameters += "noiseLevel = " + noiseLevel + "\n";
		infoParameters += "useFixedThreshold = " + useFixedThreshold + "\n";
		infoParameters += "fixedThreshold = " + fixedThreshold + "\n";
		infoParameters += "minimumThreshold = " + minimumThreshold + "\n";
		infoParameters += "ageDead = " + ageDead + "\n";
		infoParameters += "connectNewNodes = " + connectNewNodes + "\n";
		infoParameters += "lambda = " + lambda + "\n";
		infoParameters += "edgeMaxRemoval = " + edgeMaxRemoval + "\n";
		infoParameters += "nodeNumSignalsMinRemoval = " + nodeNumSignalsMinRemoval + "\n";
		infoParameters += "reduceErrorInsertion = " + reduceErrorInsertion + "\n";
		infoParameters += "smallClusterRemoval = " + smallClusterRemoval + "\n";
		infoParameters += "c2Param = " + c2Param + "\n";
		infoParameters += "c1Param = " + c1Param + "\n";
		infoParameters += "clusterJoining = " + clusterJoining + "\n";
		infoParameters += "joinTolerance = " + joinTolerance + "\n";
		infoParameters += "useAbsoluteJoinTolerance = " + useAbsoluteJoinTolerance + "\n";
		infoParameters += "joinToleranceAbsolute = " + joinToleranceAbsolute + "\n";
		infoParameters += "joiningIterationsMax = " + joiningIterationsMax + "\n";
		return infoParameters;
	}

	public String getInfoTopology() {
		String infoTopology = "";
		infoTopology += "Number of nodes: " + nodeSet.size() + "\n";

		// count nodes with different number of edges
		HashMap<Integer, Integer> nodesCounts = new HashMap<Integer, Integer>();
		for (Node node : nodeSet) {
			int edgesCount = getNeighbours(node).size();
			Integer nodesCount = nodesCounts.get(edgesCount);
			if (nodesCount == null) {
				nodesCounts.put(edgesCount, 1);
			} else {
				nodesCounts.put(edgesCount, nodesCount + 1);
			}
		}
		Integer[] edgesCountsSorted = nodesCounts.keySet().toArray(new Integer[0]);
		Arrays.sort(edgesCountsSorted);
		for (Integer edgesCount : edgesCountsSorted) {
			infoTopology += "  " + nodesCounts.get(edgesCount) + " nodes with " + edgesCount + " edges" + "\n";
		}

		infoTopology += "Number of edges: " + edgeSet.size() + "\n";
		infoTopology += "Number of clusters: " + clusterSet.size() + "\n";

		// count clusters with different number of nodes
		HashMap<Integer, Integer> clusterCounts = new HashMap<Integer, Integer>();
		for (Cluster cluster : clusterSet) {
			int nodesCount = cluster.getNodes().size();
			Integer clusterCount = clusterCounts.get(nodesCount);
			if (clusterCount == null) {
				clusterCounts.put(nodesCount, 1);
			} else {
				clusterCounts.put(nodesCount, clusterCount + 1);
			}
		}
		Integer[] nodesCountsSorted = clusterCounts.keySet().toArray(new Integer[0]);
		Arrays.sort(nodesCountsSorted);
		for (Integer nodesCount : nodesCountsSorted) {
			infoTopology += "  " + clusterCounts.get(nodesCount) + " clusters with " + nodesCount + " nodes" + "\n";
		}

		return infoTopology;
	}

	public static void main(String[] args) {

		// initialise
		int inputDim = 2;
		int numEpochs = 100;
		Random random = new Random();
		double pNoisy;

		// create SOINN-M
		SOINNM soinnm = new SOINNM(inputDim);

		// create patterns
		LinkedList<double[]> patterns = new LinkedList<double[]>();
		double[] pattern1 = { 0.2, 0.2 };
		patterns.add(pattern1);
		double[] pattern2 = { 0.8, 0.2 };
		patterns.add(pattern2);
		double[] pattern3 = { 0.2, 0.8 };
		patterns.add(pattern3);
		double[] pattern4 = { 0.8, 0.8 };
		patterns.add(pattern4);

		// input patterns
		for (int e = 0; e < numEpochs; e++) {

			// DEBUG
			System.out.println("Epoch: " + e);

			for (double[] pattern : patterns) {

				// create noisy pattern
				double[] patternNoisy = new double[inputDim];
				for (int i = 0; i < inputDim; i++) {
					pNoisy = pattern[i] + random.nextGaussian() * 0.1;
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
		}

		// write to file
		XMLWriter xmlWriter = new XMLWriter();
		xmlWriter.writeToFile(soinnm, "soinnm.xml");

		// read from file
		XMLReader xmlReader = new XMLReader();
		xmlReader.readFromFile("soinnm.xml", soinnm);

		// clear topology
		soinnm.clear();

		// insert from file
		xmlReader.insertFromFile("soinnm.xml", soinnm);

		// write to file
		xmlWriter.writeToFile(soinnm, "soinnm-inserted.xml");

	}

}
