package tosam;

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

/**
 * Class for writing a network state of TOSAM to an XML file.
 * 
 * @author Matthias Keysermann
 *
 */
public class XMLWriter {

	private static final String FILE_VERSION = "1.3";

	public void writeToFile(Network network, String filename) {
		try {

			// DEBUG
			System.out.println("Saving to XML...");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			// TOSAM
			Element elemTOSAM = doc.createElement("TOSAM");
			elemTOSAM.setAttribute("fileVersion", FILE_VERSION);
			doc.appendChild(elemTOSAM);

			// DEBUG
			System.out.println("Creating units element...");

			// Units
			Element elemUnits = doc.createElement("Units");
			elemUnits.setAttribute("count", String.valueOf(network.getUnits().size()));
			for (Unit unit : network.getUnits()) {
				Element elemUnit = doc.createElement("Unit");
				elemUnit.setAttribute("id", String.valueOf(unit.getId()));
				elemUnit.setAttribute("data", String.valueOf(unit.getData()));
				elemUnit.setAttribute("load", String.format("%f", unit.getLoad()));
				elemUnit.setAttribute("activation", String.format("%f", unit.getActivation()));
				/* not included here:
				associationsIn
				associationsOut
				*/
				elemUnits.appendChild(elemUnit);
			}
			elemTOSAM.appendChild(elemUnits);

			// DEBUG
			System.out.println("Creating associations element...");

			// Associations
			Element elemAssociations = doc.createElement("Associations");
			elemAssociations.setAttribute("count", String.valueOf(network.getAssociations().size()));
			for (Association association : network.getAssociations()) {
				Element elemAssociation = doc.createElement("Association");
				elemAssociation.setAttribute("id", String.valueOf(association.getId()));
				elemAssociation.setAttribute("srcUnitId", String.valueOf(association.getSrc().getId()));
				elemAssociation.setAttribute("dstUnitId", String.valueOf(association.getDst().getId()));
				elemAssociation.setAttribute("learningRate", String.format("%f", association.getLearningRate()));
				elemAssociation.setAttribute("weight", String.format("%f", association.getWeight()));
				elemAssociation.setAttribute("signal", String.format("%f", association.getSignal()));
				elemAssociations.appendChild(elemAssociation);
			}
			elemTOSAM.appendChild(elemAssociations);

			// DEBUG
			System.out.println("Creating variables element...");

			// Variables
			Element elemVariables = doc.createElement("Variables");
			elemVariables.setAttribute("nextUnitId", String.valueOf(network.getNextUnitId()));
			elemVariables.setAttribute("nextAssociationId", String.valueOf(network.getNextAssociationId()));
			elemVariables.setAttribute("signalAttraction", String.valueOf(network.isSignalAttraction()));
			elemVariables.setAttribute("allowOverspreading", String.valueOf(network.isAllowOverspreading()));
			elemVariables.setAttribute("fullyConnected", String.valueOf(network.isFullyConnected()));
			elemTOSAM.appendChild(elemVariables);

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
