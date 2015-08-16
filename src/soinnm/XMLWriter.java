package soinnm;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;

/**
 * Class for writing a topology state of M-SOINN to an XML file.
 * 
 * @author Matthias Keysermann
 *
 */
public class XMLWriter {

	private static final String FILE_VERSION = "1.6";

	public void writeToFile(SOINNM soinnm, String filename) {

		try {

			// DEBUG
			System.out.println("Saving to XML...");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			// SOINNM
			Element elemSOINNM = doc.createElement("SOINNM");
			elemSOINNM.setAttribute("fileVersion", FILE_VERSION);
			elemSOINNM.setAttribute("inputDim", String.valueOf(soinnm.getInputDim()));
			doc.appendChild(elemSOINNM);

			// DEBUG
			System.out.println("Creating parameters element...");

			// Parameters
			Element elemParameters = doc.createElement("Parameters");
			elemParameters.setAttribute("noiseLevel", String.valueOf(soinnm.getNoiseLevel()));
			elemParameters.setAttribute("useFixedThreshold", String.valueOf(soinnm.isUseFixedThreshold()));
			elemParameters.setAttribute("fixedThreshold", String.valueOf(soinnm.getFixedThreshold()));
			elemParameters.setAttribute("minimumThreshold", String.valueOf(soinnm.getMinimumThreshold()));
			elemParameters.setAttribute("ageDead", String.valueOf(soinnm.getAgeDead()));
			elemParameters.setAttribute("connectNewNodes", String.valueOf(soinnm.isConnectNewNodes()));
			elemParameters.setAttribute("lambda", String.valueOf(soinnm.getLambda()));
			elemParameters.setAttribute("edgeMaxRemoval", String.valueOf(soinnm.isEdgeMaxRemoval()));
			elemParameters.setAttribute("nodeNumSignalsMinRemoval", String.valueOf(soinnm.isNodeNumSignalsMinRemoval()));
			elemParameters.setAttribute("reduceErrorInsertion", String.valueOf(soinnm.isReduceErrorInsertion()));
			elemParameters.setAttribute("smallClusterRemoval", String.valueOf(soinnm.isSmallClusterRemoval()));
			elemParameters.setAttribute("c2Param", String.valueOf(soinnm.getC2Param()));
			elemParameters.setAttribute("c1Param", String.valueOf(soinnm.getC1Param()));
			elemParameters.setAttribute("clusterJoining", String.valueOf(soinnm.isClusterJoining()));
			elemParameters.setAttribute("joinTolerance", String.valueOf(soinnm.getJoinTolerance()));
			elemParameters.setAttribute("useAbsoluteJoinTolerance", String.valueOf(soinnm.isUseAbsoluteJoinTolerance()));
			elemParameters.setAttribute("joinToleranceAbsolute", String.valueOf(soinnm.getJoinToleranceAbsolute()));
			elemParameters.setAttribute("joiningIterationsMax", String.valueOf(soinnm.getJoiningIterationsMax()));
			elemSOINNM.appendChild(elemParameters);

			// DEBUG
			System.out.println("Creating nodes element...");

			// Nodes
			Element elemNodes = doc.createElement("Nodes");
			for (Node node : soinnm.getNodeSet()) {
				Element elemNode = doc.createElement("Node");
				elemNode.setAttribute("id", String.valueOf(node.getId()));
				elemNode.setAttribute("threshold", String.format("%f", node.getThreshold()));
				elemNode.setAttribute("error", String.format("%f", node.getError()));
				elemNode.setAttribute("numSignals", String.valueOf(node.getNumSignals()));
				elemNode.setAttribute("label", String.valueOf(node.getLabel()));
				Element elemPattern = doc.createElement("Pattern");
				StringBuffer stringBuffer = new StringBuffer();
				for (double pattern : node.getPattern()) {
					stringBuffer.append(String.format("%f ", pattern));
				}
				elemPattern.setTextContent(stringBuffer.toString());
				elemNode.appendChild(elemPattern);
				elemNodes.appendChild(elemNode);
			}
			elemSOINNM.appendChild(elemNodes);

			// DEBUG
			System.out.println("Creating edges element...");

			// Edges
			Element elemEdges = doc.createElement("Edges");
			for (Edge edge : soinnm.getEdgeSet()) {
				Element elemEdge = doc.createElement("Edge");
				elemEdge.setAttribute("id", String.valueOf(edge.getId()));
				elemEdge.setAttribute("age", String.valueOf(edge.getAge()));
				elemEdge.setAttribute("nodeAId", String.valueOf(edge.getNodeA().getId()));
				elemEdge.setAttribute("nodeBId", String.valueOf(edge.getNodeB().getId()));
				elemEdges.appendChild(elemEdge);
			}
			elemSOINNM.appendChild(elemEdges);

			// DEBUG
			System.out.println("Creating clusters element...");

			// Clusters
			Element elemClusters = doc.createElement("Clusters");
			for (Cluster cluster : soinnm.getClusterSet()) {
				Element elemCluster = doc.createElement("Cluster");
				elemCluster.setAttribute("id", String.valueOf(cluster.getId()));
				for (Node node : cluster.getNodes()) {
					Element elemNodeRef = doc.createElement("NodeRef");
					elemNodeRef.setAttribute("nodeId", String.valueOf(node.getId()));
					elemCluster.appendChild(elemNodeRef);
				}
				elemClusters.appendChild(elemCluster);
			}
			elemSOINNM.appendChild(elemClusters);

			// DEBUG
			System.out.println("Creating variables element...");

			// Variables
			Element elemVariables = doc.createElement("Variables");
			elemVariables.setAttribute("nextNodeId", String.valueOf(soinnm.getNextNodeId()));
			elemVariables.setAttribute("nextEdgeId", String.valueOf(soinnm.getNextEdgeId()));
			elemVariables.setAttribute("nextClusterId", String.valueOf(soinnm.getNextClusterId()));
			elemVariables.setAttribute("numInputs", String.valueOf(soinnm.getNumInputs()));
			elemSOINNM.appendChild(elemVariables);
			/* not included:
			activatedCluster
			splitClusters
			joinedClusters
			removedClusters
			*/

			// DEBUG
			System.out.println("Writing to file \"" + filename + "\"...");

			// write to xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filename));
			transformer.transform(source, result);

			// DEBUG
			System.out.println("Saving to XML finished!");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
