package soinnm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;

/**
 * Class for reading a topology state of M-SOINN from an XML file.
 * 
 * @author Matthias Keysermann
 *
 */
public class XMLReader {

	private static final String FILE_VERSION = "1.6";

	public void readFromFile(String filename, SOINNM soinnm) {
		try {

			// DEBUG
			System.out.println("Loading from XML...");

			// DEBUG
			System.out.println("Reading from file \"" + filename + "\"...");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(filename);

			// SOINNM
			Element elemSOINNM = (Element) doc.getElementsByTagName("SOINNM").item(0);

			// check file version
			String fileVersion = elemSOINNM.getAttribute("fileVersion");
			if (!fileVersion.equals(FILE_VERSION)) {
				System.err.println("Wrong file version!");
				return;
			}

			// check input dimensionality
			int inputDim = Integer.parseInt(elemSOINNM.getAttribute("inputDim"));
			if (inputDim != soinnm.getInputDim()) {
				System.err.println("Wrong input dimensionality!");
				return;
			}

			// DEBUG
			System.out.println("Setting parameters...");

			// Parameters
			Element elemParameters = (Element) elemSOINNM.getElementsByTagName("Parameters").item(0);
			soinnm.setNoiseLevel(Double.parseDouble(elemParameters.getAttribute("noiseLevel")));
			soinnm.setUseFixedThreshold(Boolean.parseBoolean(elemParameters.getAttribute("useFixedThreshold")));
			soinnm.setFixedThreshold(Double.parseDouble(elemParameters.getAttribute("fixedThreshold")));
			soinnm.setMinimumThreshold(Double.parseDouble(elemParameters.getAttribute("minimumThreshold")));
			soinnm.setAgeDead(Integer.parseInt(elemParameters.getAttribute("ageDead")));
			soinnm.setConnectNewNodes(Boolean.parseBoolean(elemParameters.getAttribute("connectNewNodes")));
			soinnm.setLambda(Integer.parseInt(elemParameters.getAttribute("lambda")));
			soinnm.setEdgeMaxRemoval(Boolean.parseBoolean(elemParameters.getAttribute("edgeMaxRemoval")));
			soinnm.setNodeNumSignalsMinRemoval(Boolean.parseBoolean(elemParameters.getAttribute("nodeNumSignalsMinRemoval")));
			soinnm.setReduceErrorInsertion(Boolean.parseBoolean(elemParameters.getAttribute("reduceErrorInsertion")));
			soinnm.setSmallClusterRemoval(Boolean.parseBoolean(elemParameters.getAttribute("smallClusterRemoval")));
			soinnm.setC2Param(Double.parseDouble(elemParameters.getAttribute("c2Param")));
			soinnm.setC1Param(Double.parseDouble(elemParameters.getAttribute("c1Param")));
			soinnm.setClusterJoining(Boolean.parseBoolean(elemParameters.getAttribute("clusterJoining")));
			soinnm.setJoinTolerance(Double.parseDouble(elemParameters.getAttribute("joinTolerance")));
			soinnm.setUseAbsoluteJoinTolerance(Boolean.parseBoolean(elemParameters.getAttribute("useAbsoluteJoinTolerance")));
			soinnm.setJoinToleranceAbsolute(Double.parseDouble(elemParameters.getAttribute("joinToleranceAbsolute")));
			soinnm.setJoiningIterationsMax(Integer.parseInt(elemParameters.getAttribute("joiningIterationsMax")));

			// DEBUG
			System.out.println("Recreating nodes...");

			// clear old nodes
			soinnm.getNodeSet().clear();

			// Nodes			
			Element elemNodes = (Element) elemSOINNM.getElementsByTagName("Nodes").item(0);
			NodeList nlNodes = elemNodes.getElementsByTagName("Node");
			for (int i = 0; i < nlNodes.getLength(); i++) {
				Element elemNode = (Element) nlNodes.item(i);
				long id = Long.parseLong(elemNode.getAttribute("id"));
				Element elemPattern = (Element) elemNode.getElementsByTagName("Pattern").item(0);
				StringTokenizer strTokenizer = new StringTokenizer(elemPattern.getTextContent());
				double[] pattern = new double[inputDim];
				for (int j = 0; j < pattern.length; j++) {
					pattern[j] = Double.parseDouble((String) strTokenizer.nextElement());
				}
				double threshold = Double.parseDouble(elemNode.getAttribute("threshold"));
				double error = Double.parseDouble(elemNode.getAttribute("error"));
				long numSignals = Long.parseLong(elemNode.getAttribute("numSignals"));
				String label = elemNode.getAttribute("label");

				// create node
				Node node = new Node(id, pattern);
				node.setThreshold(threshold);
				node.setError(error);
				node.setNumSignals(numSignals);
				node.setLabel(label);

				// add node
				soinnm.getNodeSet().add(node);
			}

			// DEBUG
			System.out.println("Recreating edges...");

			// clear old edges
			soinnm.getEdgeSet().clear();

			// Edges
			Element elemEdges = (Element) elemSOINNM.getElementsByTagName("Edges").item(0);
			NodeList nlEdges = elemEdges.getElementsByTagName("Edge");
			for (int i = 0; i < nlEdges.getLength(); i++) {
				Element elemEdge = (Element) nlEdges.item(i);
				long id = Long.parseLong(elemEdge.getAttribute("id"));
				long age = Long.parseLong(elemEdge.getAttribute("age"));
				long nodeAId = Long.parseLong(elemEdge.getAttribute("nodeAId"));
				long nodeBId = Long.parseLong(elemEdge.getAttribute("nodeBId"));

				// find nodes
				Node nodeA = null;
				Node nodeB = null;
				for (Node node : soinnm.getNodeSet()) {
					if (node.getId() == nodeAId) {
						nodeA = node;
					}
					if (node.getId() == nodeBId) {
						nodeB = node;
					}
				}

				// create edge
				Edge edge = new Edge(id, nodeA, nodeB);
				edge.setAge(age);

				// add edge
				soinnm.getEdgeSet().add(edge);
			}

			// DEBUG
			System.out.println("Recreating clusters...");

			// clear old clusters
			soinnm.getClusterSet().clear();

			// Clusters
			Element elemClusters = (Element) elemSOINNM.getElementsByTagName("Clusters").item(0);
			NodeList nlClusters = elemClusters.getElementsByTagName("Cluster");
			for (int i = 0; i < nlClusters.getLength(); i++) {
				Element elemCluster = (Element) nlClusters.item(i);
				long id = Long.parseLong(elemCluster.getAttribute("id"));

				// create cluster
				LinkedList<Node> nodes = new LinkedList<Node>();
				Cluster cluster = new Cluster(id, nodes);

				// add nodes
				NodeList nlNodeRefs = elemCluster.getElementsByTagName("NodeRef");
				for (int j = 0; j < nlNodeRefs.getLength(); j++) {
					Element elemNodeRef = (Element) nlNodeRefs.item(j);
					long nodeId = Long.parseLong(elemNodeRef.getAttribute("nodeId"));

					// find node
					for (Node node : soinnm.getNodeSet()) {
						if (node.getId() == nodeId) {
							nodes.add(node);
							break;
						}
					}
				}

				// add cluster
				soinnm.getClusterSet().add(cluster);
			}

			// DEBUG
			System.out.println("Setting variables...");

			// Variables
			Element elemVariables = (Element) elemSOINNM.getElementsByTagName("Variables").item(0);
			soinnm.setNextNodeId(Long.parseLong(elemVariables.getAttribute("nextNodeId")));
			soinnm.setNextEdgeId(Long.parseLong(elemVariables.getAttribute("nextEdgeId")));
			soinnm.setNextClusterId(Long.parseLong(elemVariables.getAttribute("nextClusterId")));
			soinnm.setNumInputs(Long.parseLong(elemVariables.getAttribute("numInputs")));
			/* not included:
			activatedCluster
			splitClusters
			joinedClusters
			removedClusters
			*/

			// DEBUG
			System.out.println("Loading from XML finished!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertFromFile(String filename, SOINNM soinnm) {
		try {

			// DEBUG
			System.out.println("Inserting from XML...");

			// DEBUG
			System.out.println("Reading from file \"" + filename + "\"...");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(filename);

			// SOINNM
			Element elemSOINNM = (Element) doc.getElementsByTagName("SOINNM").item(0);

			// check file version
			String fileVersion = elemSOINNM.getAttribute("fileVersion");
			if (!fileVersion.equals(FILE_VERSION)) {
				System.err.println("Wrong file version!");
				return;
			}

			// check input dimensionality
			int inputDim = Integer.parseInt(elemSOINNM.getAttribute("inputDim"));
			if (inputDim != soinnm.getInputDim()) {
				System.err.println("Wrong input dimensionality!");
				return;
			}

			// DEBUG
			System.out.println("Skipping parameters...");

			// DEBUG
			System.out.println("Inserting nodes...");

			// create id mapping
			HashMap<Long, Long> nodeIdMap = new HashMap<Long, Long>();

			// Nodes
			Element elemNodes = (Element) elemSOINNM.getElementsByTagName("Nodes").item(0);
			NodeList nlNodes = elemNodes.getElementsByTagName("Node");
			for (int i = 0; i < nlNodes.getLength(); i++) {
				Element elemNode = (Element) nlNodes.item(i);
				long id = Long.parseLong(elemNode.getAttribute("id"));
				Element elemPattern = (Element) elemNode.getElementsByTagName("Pattern").item(0);
				StringTokenizer strTokenizer = new StringTokenizer(elemPattern.getTextContent());
				double[] pattern = new double[inputDim];
				for (int j = 0; j < pattern.length; j++) {
					pattern[j] = Double.parseDouble((String) strTokenizer.nextElement());
				}
				double threshold = Double.parseDouble(elemNode.getAttribute("threshold"));
				double error = Double.parseDouble(elemNode.getAttribute("error"));
				long numSignals = Long.parseLong(elemNode.getAttribute("numSignals"));

				// store id mapping
				long nextNodeId = soinnm.getNextNodeId();
				nodeIdMap.put(id, nextNodeId);

				// create node				
				Node node = new Node(nextNodeId, pattern);
				node.setThreshold(threshold);
				node.setError(error);
				node.setNumSignals(numSignals);

				// add node
				soinnm.getNodeSet().add(node);

				// increase next node id
				nextNodeId++;
				soinnm.setNextNodeId(nextNodeId);

				/*
				// increase number of inputs
				long numInputs = soinnm.getNumInputs();
				numInputs++;
				soinnm.setNumInputs(numInputs);
				*/
			}

			// DEBUG
			System.out.println("Inserting edges...");

			// Edges
			Element elemEdges = (Element) elemSOINNM.getElementsByTagName("Edges").item(0);
			NodeList nlEdges = elemEdges.getElementsByTagName("Edge");
			for (int i = 0; i < nlEdges.getLength(); i++) {
				Element elemEdge = (Element) nlEdges.item(i);
				//long id = Long.parseLong(elemEdge.getAttribute("id"));
				long age = Long.parseLong(elemEdge.getAttribute("age"));
				long nodeAId = Long.parseLong(elemEdge.getAttribute("nodeAId"));
				long nodeBId = Long.parseLong(elemEdge.getAttribute("nodeBId"));

				// use mapped id
				nodeAId = nodeIdMap.get(nodeAId);
				nodeBId = nodeIdMap.get(nodeBId);

				// find nodes
				Node nodeA = null;
				Node nodeB = null;
				for (Node node : soinnm.getNodeSet()) {
					if (node.getId() == nodeAId) {
						nodeA = node;
					}
					if (node.getId() == nodeBId) {
						nodeB = node;
					}
				}

				// create edge
				long nextEdgeId = soinnm.getNextEdgeId();
				Edge edge = new Edge(nextEdgeId, nodeA, nodeB);
				edge.setAge(age);

				// add edge
				soinnm.getEdgeSet().add(edge);

				// increase next edge id
				nextEdgeId++;
				soinnm.setNextEdgeId(nextEdgeId);
			}

			// DEBUG
			System.out.println("Inserting clusters...");

			// Clusters
			Element elemClusters = (Element) elemSOINNM.getElementsByTagName("Clusters").item(0);
			NodeList nlClusters = elemClusters.getElementsByTagName("Cluster");
			for (int i = 0; i < nlClusters.getLength(); i++) {
				Element elemCluster = (Element) nlClusters.item(i);
				//long id = Long.parseLong(elemCluster.getAttribute("id"));

				// create cluster
				long nextClusterId = soinnm.getNextClusterId();
				LinkedList<Node> nodes = new LinkedList<Node>();
				Cluster cluster = new Cluster(nextClusterId, nodes);

				// add nodes
				NodeList nlNodeRefs = elemCluster.getElementsByTagName("NodeRef");
				for (int j = 0; j < nlNodeRefs.getLength(); j++) {
					Element elemNodeRef = (Element) nlNodeRefs.item(j);
					long nodeId = Long.parseLong(elemNodeRef.getAttribute("nodeId"));

					// use mapped id
					nodeId = nodeIdMap.get(nodeId);

					// find node
					for (Node node : soinnm.getNodeSet()) {
						if (node.getId() == nodeId) {
							nodes.add(node);
							break;
						}
					}
				}
				soinnm.getClusterSet().add(cluster);

				// increase next cluster id
				nextClusterId++;
				soinnm.setNextClusterId(nextClusterId);
			}

			// DEBUG
			System.out.println("Skipping variables...");

			// DEBUG
			System.out.println("Inserting from XML finished!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
