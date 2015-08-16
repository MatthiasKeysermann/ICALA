package tdam;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Class for holding the network of units and associations.
 * <p>
 * Allows to insert data into the network which automatically creates
 * corresponding units and associations. The network is fully connected, i.e.
 * has associations between every pair of units. Manages unique ids for units
 * and associations.
 * <p>
 * Globally manages calculation of errors and signal sums, updating of strengths
 * and traces. Can clean up insignificant units. Can join and split data.
 * 
 * 
 * @author Matthias Keysermann
 *
 */
public class Network {

	private static final double REMOVAL_THRESHOLD_INPUT = 0.01;

	private static final double REMOVAL_THRESHOLD_TRACE = 0.01;

	private static final double REMOVAL_THRESHOLD_SIGNAL_SUM = 0.01;

	private static final double REMOVAL_THRESHOLD_STRENGTH = 0.01;

	private LinkedList<Unit> units;

	private long nextUnitId;

	private LinkedList<Association> associations;

	private long nextAssociationId;

	public Network() {
		units = new LinkedList<Unit>();
		nextUnitId = 1;
		associations = new LinkedList<Association>();
		nextAssociationId = 1;
	}

	// variables getters/setters

	public LinkedList<Unit> getUnits() {
		return units;
	}

	public long getNextUnitId() {
		return nextUnitId;
	}

	public void setNextUnitId(long nextUnitId) {
		this.nextUnitId = nextUnitId;
	}

	public void setNextAssociationId(long nextAssociationId) {
		this.nextAssociationId = nextAssociationId;
	}

	public LinkedList<Association> getAssociations() {
		return associations;
	}

	public long getNextAssociationId() {
		return nextAssociationId;
	}

	/**
	 * Clears all units and associations
	 */
	public void clear() {
		synchronized (units) {
			synchronized (associations) {
				units.clear();
				associations.clear();
			}
		}
	}

	/**
	 * Returns the unit containing the data.
	 * 
	 * @param data
	 *            data pattern to search for
	 * @return unit containing data pattern
	 */
	public Unit getUnit(Object data) {
		synchronized (units) {

			// check if data is contained in network
			for (Unit unit : units) {
				if (unit.getData().equals(data)) {
					return unit;
				}
			}

			return null;
		}
	}

	/**
	 * Inserts data into the network and returns the unit containing the data.
	 * 
	 * @param data
	 *            data pattern to insert
	 * @return unit containing data pattern
	 */
	public Unit insertData(Object data) {
		synchronized (units) {
			synchronized (associations) {

				// check if data is contained in network
				for (Unit unit : units) {
					if (unit.getData().equals(data)) {
						return unit;
					}
				}

				// create new unit
				Unit unitNew = createUnit(data);

				// create new association
				for (Unit unit : units) {

					// incoming association
					Association associationIn = createAssociation(unit, unitNew);
					associations.add(associationIn);
					unitNew.getAssociationsIn().add(associationIn);
					unit.getAssociationsOut().add(associationIn);

					// outgoing association
					Association associationOut = createAssociation(unitNew, unit);
					associations.add(associationOut);
					unitNew.getAssociationsOut().add(associationOut);
					unit.getAssociationsIn().add(associationOut);

				}

				// add new unit
				units.add(unitNew);

				return unitNew;

			}
		}
	}

	/**
	 * Deletes data from the network and returns the removed unit.
	 * 
	 * @param data
	 *            data pattern to delete
	 * @return unit containing data pattern
	 */
	public Unit deleteData(Object data) {
		synchronized (units) {
			synchronized (associations) {

				// check if data is contained in network
				for (Unit unit : units) {
					if (unit.getData().equals(data)) {

						// remove associations
						associations.removeAll(unit.getAssociationsIn());
						associations.removeAll(unit.getAssociationsOut());

						// remove unit
						units.remove(unit);

						return unit;
					}
				}

				return null;
			}
		}
	}

	/**
	 * Deletes unit and corresponding associations from the network.
	 * 
	 * @param unit
	 *            unit to delete
	 */
	public void deleteUnit(Unit unit) {
		synchronized (units) {
			synchronized (associations) {

				// remove incoming associations from outgoing associations of other units
				for (Association associationIn : unit.getAssociationsIn()) {
					associationIn.getSrc().getAssociationsOut().remove(associationIn);
				}

				// remove outgoing associations from incoming associations of other untis
				for (Association associationOut : unit.getAssociationsOut()) {
					associationOut.getDst().getAssociationsIn().remove(associationOut);
				}

				// remove associations
				associations.removeAll(unit.getAssociationsIn());
				associations.removeAll(unit.getAssociationsOut());

				// remove unit
				units.remove(unit);

			}
		}
	}

	/**
	 * Joins data within the network and returns the kept unit.
	 * 
	 * @param dataKeep
	 *            data pattern to keep
	 * @param dataDelete
	 *            data pattern to delete
	 * @return unit containing data pattern to keep
	 */
	public Unit joinData(Object dataKeep, Object dataDelete) {
		synchronized (units) {
			synchronized (associations) {

				// initialise
				Unit unitKeep = null;
				Unit unitRemove = null;

				// check if data is contained in network
				for (Unit unit : units) {
					if (unit.getData().equals(dataKeep)) {
						unitKeep = unit;
					}
					if (unit.getData().equals(dataDelete)) {
						unitRemove = unit;
					}
				}

				// equivalent data
				if (unitKeep == unitRemove) {
					return unitKeep;
				}

				// units exist
				if (unitKeep != null && unitRemove != null) {

					// join units

					// determine maximum input
					double inputMax = Math.max(unitKeep.getInput(), unitRemove.getInput());
					unitKeep.setInput(inputMax);

					// determine maximum trace
					double traceMax = Math.max(unitKeep.getTrace(), unitRemove.getTrace());
					unitKeep.setTrace(traceMax);

					// determine maximum signal sum
					double signalSumMax = Math.max(unitKeep.getSignalSum(), unitRemove.getSignalSum());
					unitKeep.setSignalSum(signalSumMax);

					// determine maximum old signal sum
					double signalSumOldMax = Math.max(unitKeep.getSignalSumOld(), unitRemove.getSignalSumOld());
					unitKeep.setSignalSumOld(signalSumOldMax);

					// determine maximum error
					double errorMax = Math.max(unitKeep.getError(), unitRemove.getError());
					unitKeep.setError(errorMax);

					// join associations

					// loop over other units
					for (Unit unit : units) {
						if (unit != unitKeep && unit != unitRemove) {

							// associations connecting with unit
							Association associationInKeep = null;
							Association associationInDelete = null;
							Association associationOutKeep = null;
							Association associationOutDelete = null;
							for (Association association : associations) {
								if (association.getSrc() == unit && association.getDst() == unitKeep) {
									associationInKeep = association;
								} else if (association.getSrc() == unit && association.getDst() == unitRemove) {
									associationInDelete = association;
								} else if (association.getSrc() == unitKeep && association.getDst() == unit) {
									associationOutKeep = association;
								} else if (association.getSrc() == unitRemove && association.getDst() == unit) {
									associationOutDelete = association;
								}
							}

							// determine maximum strength for incoming association
							double associationInStrengthMax = Math.max(associationInKeep.getStrength(), associationInDelete.getStrength());
							associationInKeep.setStrength(associationInStrengthMax);

							// determine maximum strength for outgoing association
							double associationOutStrengthMax = Math.max(associationOutKeep.getStrength(), associationOutDelete.getStrength());
							associationOutKeep.setStrength(associationOutStrengthMax);

						}
					}

					// remove associations
					associations.removeAll(unitRemove.getAssociationsIn());
					associations.removeAll(unitRemove.getAssociationsOut());

					// remove unit
					units.remove(unitRemove);

				}

				return unitKeep;
			}
		}
	}

	/**
	 * Splits data in the network and returns the new unit.
	 * 
	 * @param dataOld
	 *            existing data pattern
	 * @param dataNew
	 *            new data pattern
	 * @return unit containing new data pattern
	 */
	public Unit splitData(Object dataOld, Object dataNew) {
		synchronized (units) {
			synchronized (associations) {

				// check if data is contained in network
				for (Unit unitOld : units) {
					if (unitOld.getData().equals(dataOld)) {

						// create new unit
						Unit unitNew = insertData(dataNew);

						// copy input
						unitNew.setInput(unitOld.getInput());

						// copy trace
						unitNew.setTrace(unitOld.getTrace());

						// copy signal sum
						unitNew.setSignalSum(unitOld.getSignalSum());

						// copy old signal sum
						unitNew.setSignalSumOld(unitOld.getSignalSumOld());

						// copy error						
						unitNew.setError(unitOld.getError());

						// loop over other units
						for (Unit unit : units) {
							if (unit != unitOld && unit != unitNew) {

								// associations connecting with unit
								Association associationInOld = null;
								Association associationInNew = null;
								Association associationOutOld = null;
								Association associationOutNew = null;
								for (Association association : associations) {
									if (association.getSrc() == unit && association.getDst() == unitOld) {
										associationInOld = association;
									} else if (association.getSrc() == unit && association.getDst() == unitNew) {
										associationInNew = association;
									} else if (association.getSrc() == unitOld && association.getDst() == unit) {
										associationOutOld = association;
									} else if (association.getSrc() == unitNew && association.getDst() == unit) {
										associationOutNew = association;
									}
								}

								// copy strength for incoming association
								associationInNew.setStrength(associationInOld.getStrength());

								// copy strength for outgoing association
								associationOutNew.setStrength(associationOutOld.getStrength());

							}
						}

						// TODO: set maximum strength for association between old and new unit?

						return unitNew;
					}
				}

				return null;
			}
		}
	}

	private Unit createUnit(Object data) {
		return new Unit(nextUnitId++, data);
	}

	private Association createAssociation(Unit src, Unit dst) {
		return new Association(nextAssociationId++, src, dst);
	}

	/**
	 * Calculates the signal sum for all units.
	 */
	public void calculateSignalSums() {
		synchronized (units) {
			synchronized (associations) {
				for (Unit unit : units) {

					// sum up signal of incoming associations
					double signalSum = 0;
					for (Association associationIn : unit.getAssociationsIn()) {
						signalSum += associationIn.getStrength() * associationIn.getSrc().getInput();
					}
					unit.setSignalSum(signalSum);

				}
			}
		}
	}

	/**
	 * Resets the input for all units.
	 */
	public void resetInputs() {
		synchronized (units) {
			for (Unit unit : units) {
				unit.resetInput();
			}
		}
	}

	/**
	 * Calculates the error for all units.
	 */
	public void calculateErrors() {
		synchronized (units) {
			for (Unit unit : units) {
				unit.calculateError();
			}
		}
	}

	/**
	 * Updates the strength for all associations.
	 */
	public void updateStrengths() {
		synchronized (associations) {
			for (Association association : associations) {
				association.updateStrength();
			}
		}

	}

	/**
	 * Updates the trace for all units.
	 */
	public void updateTraces() {
		synchronized (units) {
			for (Unit unit : units) {
				unit.updateTrace();
			}
		}
	}

	/**
	 * Stores the signal sum for all units.
	 */
	public void storeSignalSums() {
		synchronized (units) {
			for (Unit unit : units) {
				unit.storeSignalSum();
			}
		}
	}

	/**
	 * Cleans up the network by removing units with low input, trace, signal sum
	 * and low strengths of connected associations in order to maintain
	 * performance.
	 */
	public void cleanUp() {
		synchronized (units) {
			synchronized (associations) {

				// loop over units
				Iterator<Unit> itUnits = units.iterator();
				while (itUnits.hasNext()) {
					Unit unit = itUnits.next();

					// initialise
					boolean removal = true;

					// check for low input, low trace and low signal sum of unit
					if (unit.getInput() > REMOVAL_THRESHOLD_INPUT || unit.getTrace() > REMOVAL_THRESHOLD_TRACE || unit.getSignalSum() > REMOVAL_THRESHOLD_SIGNAL_SUM) {
						removal = false;
					} else {

						// check for low strengths of incoming associations
						for (Association association : unit.getAssociationsIn()) {
							if (Math.abs(association.getStrength()) > REMOVAL_THRESHOLD_STRENGTH) {
								removal = false;
								break;
							}
						}

						// check for low strengths of outgoing associations
						for (Association association : unit.getAssociationsOut()) {
							if (Math.abs(association.getStrength()) > REMOVAL_THRESHOLD_STRENGTH) {
								removal = false;
								break;
							}
						}

					}

					// remove unit and associations
					if (removal) {
						associations.removeAll(unit.getAssociationsIn());
						associations.removeAll(unit.getAssociationsOut());
						itUnits.remove();
					}

				}

			}
		}
	}

}
