package tdam;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Class for reading a network state of TDAM from an XML file.
 * 
 * @author Matthias Keysermann
 *
 */
public class XMLReader {

	private static final String FILE_VERSION = "1.0";

	public void readFromFile(String filename, Network network) {
		try {

			// DEBUG
			System.out.println("Loading from XML...");

			// DEBUG
			System.out.println("Reading from file \"" + filename + "\"...");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(filename);

			// TOSAM
			Element elemTOSAM = (Element) doc.getElementsByTagName("TDAM").item(0);

			// check file version
			String fileVersion = elemTOSAM.getAttribute("fileVersion");
			if (!fileVersion.equals(FILE_VERSION)) {
				System.err.println("Wrong file version!");
				return;
			}

			// DEBUG
			System.out.println("Recreating units...");

			// clear old units
			network.getUnits().clear();

			// Units			
			Element elemUnits = (Element) elemTOSAM.getElementsByTagName("Units").item(0);
			NodeList nlUnits = elemUnits.getElementsByTagName("Unit");
			for (int i = 0; i < nlUnits.getLength(); i++) {
				Element elemUnit = (Element) nlUnits.item(i);
				long id = Long.parseLong(elemUnit.getAttribute("id"));
				Object data = elemUnit.getAttribute("data");
				double input = Double.parseDouble(elemUnit.getAttribute("input"));
				double trace = Double.parseDouble(elemUnit.getAttribute("trace"));
				double signalSum = Double.parseDouble(elemUnit.getAttribute("signalSum"));
				double signalSumOld = Double.parseDouble(elemUnit.getAttribute("signalSumOld"));
				double error = Double.parseDouble(elemUnit.getAttribute("error"));
				Unit unit = new Unit(id, data);
				unit.setInput(input);
				unit.setTrace(trace);
				unit.setSignalSum(signalSum);
				unit.setSignalSumOld(signalSumOld);
				unit.setError(error);
				network.getUnits().add(unit);
			}

			// DEBUG
			System.out.println("Recreating associations...");

			// clear old associations
			network.getAssociations().clear();

			// Associations			
			Element elemAssociations = (Element) elemTOSAM.getElementsByTagName("Associations").item(0);
			NodeList nlAssociations = elemAssociations.getElementsByTagName("Association");
			for (int i = 0; i < nlAssociations.getLength(); i++) {
				Element elemAssociation = (Element) nlAssociations.item(i);
				long id = Long.parseLong(elemAssociation.getAttribute("id"));
				long srcUnitId = Long.parseLong(elemAssociation.getAttribute("srcUnitId"));
				long dstUnitId = Long.parseLong(elemAssociation.getAttribute("dstUnitId"));
				double strength = Double.parseDouble(elemAssociation.getAttribute("strength"));
				Unit srcUnit = null;
				Unit dstUnit = null;
				for (Unit unit : network.getUnits()) {
					if (unit.getId() == srcUnitId) {
						srcUnit = unit;
					}
					if (unit.getId() == dstUnitId) {
						dstUnit = unit;
					}
				}
				Association association = new Association(id, srcUnit, dstUnit);
				association.setStrength(strength);
				network.getAssociations().add(association);
				// associationsIn & associationsOut
				srcUnit.getAssociationsOut().add(association);
				dstUnit.getAssociationsIn().add(association);
			}

			// DEBUG
			System.out.println("Setting variables...");

			// Variables
			Element elemVariables = (Element) elemTOSAM.getElementsByTagName("Variables").item(0);
			network.setNextUnitId(Long.parseLong(elemVariables.getAttribute("nextUnitId")));
			network.setNextAssociationId(Long.parseLong(elemVariables.getAttribute("nextAssociationId")));

			// DEBUG
			System.out.println("Loading from XML finished!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
