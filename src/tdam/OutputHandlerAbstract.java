package tdam;

import java.util.LinkedList;

/**
 * Abstract class for providing a list of units containing a given name as a
 * first word in their data pattern.
 * 
 * @author Matthias Keysermann
 *
 */
public abstract class OutputHandlerAbstract implements OutputHandler {

	protected Simulation simulation;

	public OutputHandlerAbstract(Simulation simulation) {
		this.simulation = simulation;
	}

	public LinkedList<Unit> fetchUnits(String name) {
		LinkedList<Unit> unitsFetched = new LinkedList<Unit>();
		LinkedList<Unit> units = simulation.getNetwork().getUnits();
		synchronized (units) {
			for (Unit unit : units) {
				if (name.equals(unit.getData().toString().split(" ")[0])) {
					unitsFetched.add(unit);
				}
			}
		}
		return unitsFetched;
	}
}
