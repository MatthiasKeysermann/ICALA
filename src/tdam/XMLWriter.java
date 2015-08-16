package tdam;

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
 * Class for writing a network state of TDAM to an XML file.
 * 
 * @author Matthias Keysermann
 *
 */
public class XMLWriter {

	private static final String FILE_VERSION = "1.0";

	public void writeToFile(Network network, String filename) {
		try {

			// DEBUG
			System.out.println("Saving to XML...");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			// TOSAM
			Element elemTOSAM = doc.createElement("TDAM");
			elemTOSAM.setAttribute("fileVersion", FILE_VERSION);
			doc.appendChild(elemTOSAM);

			// DEBUG
			System.out.println("Creating units element...");

			// Units
			Element elemUnits = doc.createElement("Units");
			for (Unit unit : network.getUnits()) {
				Element elemUnit = doc.createElement("Unit");
				elemUnit.setAttribute("id", String.valueOf(unit.getId()));
				elemUnit.setAttribute("data", String.valueOf(unit.getData()));
				elemUnit.setAttribute("input", String.format("%f", unit.getInput()));
				elemUnit.setAttribute("trace", String.format("%f", unit.getTrace()));
				elemUnit.setAttribute("signalSum", String.format("%f", unit.getSignalSum()));
				elemUnit.setAttribute("signalSumOld", String.format("%f", unit.getSignalSumOld()));
				elemUnit.setAttribute("error", String.format("%f", unit.getError()));
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
			for (Association association : network.getAssociations()) {
				Element elemAssociation = doc.createElement("Association");
				elemAssociation.setAttribute("id", String.valueOf(association.getId()));
				elemAssociation.setAttribute("srcUnitId", String.valueOf(association.getSrc().getId()));
				elemAssociation.setAttribute("dstUnitId", String.valueOf(association.getDst().getId()));
				elemAssociation.setAttribute("strength", String.format("%f", association.getStrength()));
				elemAssociations.appendChild(elemAssociation);
			}
			elemTOSAM.appendChild(elemAssociations);

			// DEBUG
			System.out.println("Creating variables element...");

			// Variables
			Element elemVariables = doc.createElement("Variables");
			elemVariables.setAttribute("nextUnitId", String.valueOf(network.getNextUnitId()));
			elemVariables.setAttribute("nextAssociationId", String.valueOf(network.getNextAssociationId()));
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
