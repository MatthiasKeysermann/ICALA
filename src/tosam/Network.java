package tosam;

import java.util.LinkedList;

/**
 * Class for holding the network of units and associations.
 * <p>
 * Allows to insert data into the network which automatically creates
 * corresponding units. Allows to generate associations between units with input
 * load. The network can be fully connected, i.e. has associations between every
 * pair of units. Manages unique ids for units and associations.
 * <p>
 * Globally manages leakage of input loads, decay of activation levels, decay of
 * weights. Can clean up insignificant units. Can join and split data.
 * <p>
 * Manages the spreading of activation between units. The spreading process can
 * consider the activation level of the destination unit, i.e. signal
 * attraction, or allow overspreading of activation.
 * 
 * @author Matthias Keysermann
 *
 */
public class Network {

	public static final double REMOVAL_PERCENTAGE_ACTIVATION = 0.01;

	public static final double REMOVAL_PERCENTAGE_WEIGHT = 0.01;

	public static final double REMOVAL_PERCENTAGE_LOAD = 0.01;

	public static final double REMOVAL_THRESHOLD_ACTIVATION = Unit.ACTIVATION_MAX * REMOVAL_PERCENTAGE_ACTIVATION;

	public static final double REMOVAL_THRESHOLD_WEIGHT = Association.WEIGHT_MAX * REMOVAL_PERCENTAGE_WEIGHT;

	public static final double REMOVAL_THRESHOLD_LOAD = Unit.LOAD_MAX * REMOVAL_PERCENTAGE_LOAD;

	private LinkedList<Unit> units;

	private long nextUnitId;

	private LinkedList<Association> associations;

	private long nextAssociationId;

	private boolean signalAttraction = true; // true

	private boolean allowOverspreading = false; // false

	private boolean fullyConnected = false; // false

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

	public boolean isSignalAttraction() {
		return signalAttraction;
	}

	public void setSignalAttraction(boolean signalAttraction) {
		this.signalAttraction = signalAttraction;
	}

	public boolean isAllowOverspreading() {
		return allowOverspreading;
	}

	public void setAllowOverspreading(boolean allowOverspreading) {
		this.allowOverspreading = allowOverspreading;
	}

	public boolean isFullyConnected() {
		return fullyConnected;
	}

	public void setFullyConnected(boolean fullyConnected) {
		this.fullyConnected = fullyConnected;
	}

	/**
	 * Clears all units and associations.
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

			// check if data is contained in network
			for (Unit unit : units) {
				if (unit.getData().equals(data)) {
					return unit;
				}
			}

			// create new unit
			Unit unitNew = createUnit(data);

			// create all associations
			if (fullyConnected) {
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
			}

			// add new unit
			units.add(unitNew);

			return unitNew;

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

				// initialise
				Unit unitDelete = null;

				// check if data is contained in network
				for (Unit unit : units) {
					if (unit.getData().equals(data)) {
						unitDelete = unit;
						break;
					}
				}

				// delete unit and associations
				if (unitDelete != null) {
					deleteUnit(unitDelete);
				}

				return unitDelete;
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
				Unit unitDelete = null;

				// check if data is contained in network
				for (Unit unit : units) {
					if (unit.getData().equals(dataKeep)) {
						unitKeep = unit;
					}
					if (unit.getData().equals(dataDelete)) {
						unitDelete = unit;
					}
				}

				// equivalent data
				if (unitKeep == unitDelete) {
					return unitKeep;
				}

				// units exist
				if (unitKeep != null && unitDelete != null) {

					// join units

					// determine maximum load
					double loadMax = Math.max(unitKeep.getLoad(), unitDelete.getLoad());
					unitKeep.setLoad(loadMax);

					// determine maximum activation level
					double activationMax = Math.max(unitKeep.getActivation(), unitDelete.getActivation());
					unitKeep.setActivation(activationMax);

					// join associations

					// loop over incoming associations of unit to delete
					for (Association associationInDelete : unitDelete.getAssociationsIn()) {
						Unit unitSrc = associationInDelete.getSrc();

						// ignore association from unit to keep
						if (unitSrc != unitKeep) {

							// check if association exists for unit to keep
							Association associationInKeep = null;
							for (Association associationIn : unitKeep.getAssociationsIn()) {
								if (associationIn.getSrc() == unitSrc) {
									associationInKeep = associationIn;
									break;
								}
							}

							// unit to keep does not have association
							if (associationInKeep == null) {

								// create association								
								associationInKeep = createAssociation(unitSrc, unitKeep);
								associations.add(associationInKeep);
								unitSrc.getAssociationsOut().add(associationInKeep);
								unitKeep.getAssociationsIn().add(associationInKeep);

							}

							// determine minimum learning rate for incoming association
							double associationInLearningRateMin = Math.min(associationInKeep.getLearningRate(), associationInDelete.getLearningRate());
							associationInKeep.setLearningRate(associationInLearningRateMin);

							// determine maximum weight for incoming association
							double associationInWeightMax = Math.max(associationInKeep.getWeight(), associationInDelete.getWeight());
							associationInKeep.setWeight(associationInWeightMax);

							// determine maximum signal for incoming association
							double associationInSignalMax = Math.max(associationInKeep.getSignal(), associationInDelete.getSignal());
							associationInKeep.setSignal(associationInSignalMax);

						}

					}

					// loop over outgoing associations of unit to delete
					for (Association associationOutDelete : unitDelete.getAssociationsOut()) {
						Unit unitDst = associationOutDelete.getDst();

						// ignore association to unit to keep
						if (unitDst != unitKeep) {

							// check if association exists for unit to keep
							Association associationOutKeep = null;
							for (Association associationOut : unitKeep.getAssociationsOut()) {
								if (associationOut.getDst() == unitDst) {
									associationOutKeep = associationOut;
									break;
								}
							}

							// unit to keep does not have association
							if (associationOutKeep == null) {

								// create association								
								associationOutKeep = createAssociation(unitKeep, unitDst);
								associations.add(associationOutKeep);
								unitKeep.getAssociationsOut().add(associationOutKeep);
								unitDst.getAssociationsIn().add(associationOutKeep);

							}

							// determine minimum learning rate for outgoing association
							double associationOutLearningRateMin = Math.min(associationOutKeep.getLearningRate(), associationOutDelete.getLearningRate());
							associationOutKeep.setLearningRate(associationOutLearningRateMin);

							// determine maximum weight for outgoing association
							double associationOutWeightMax = Math.max(associationOutKeep.getWeight(), associationOutDelete.getWeight());
							associationOutKeep.setWeight(associationOutWeightMax);

							// determine maximum signal for outgoing association
							double associationOutSignalMax = Math.max(associationOutKeep.getSignal(), associationOutDelete.getSignal());
							associationOutKeep.setSignal(associationOutSignalMax);

						}

					}

					// delete unit and associations
					deleteUnit(unitDelete);

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

						// create new unit (and associations if fully-connected)
						Unit unitNew = insertData(dataNew);

						// copy input load
						unitNew.setLoad(unitOld.getLoad());

						// copy activation level						
						unitNew.setActivation(unitOld.getActivation());

						// loop over incoming associations of old unit						
						for (Association associationInOld : unitOld.getAssociationsIn()) {
							Unit unitSrc = associationInOld.getSrc();

							// ignore associations from new unit
							if (unitSrc != unitNew) {

								// check if association exists for new unit
								Association associationInNew = null;
								for (Association associationIn : unitNew.getAssociationsIn()) {
									if (associationIn.getSrc() == unitSrc) {
										associationInNew = associationIn;
										break;
									}
								}

								// new unit does not have association
								if (associationInNew == null) {

									// create association
									associationInNew = createAssociation(unitSrc, unitNew);
									associations.add(associationInNew);
									unitSrc.getAssociationsOut().add(associationInNew);
									unitNew.getAssociationsIn().add(associationInNew);

								}

								// copy learning rate for incoming association
								associationInNew.setLearningRate(associationInOld.getLearningRate());

								// copy weight for incoming association
								associationInNew.setWeight(associationInOld.getWeight());

								// copy signal for incoming association
								associationInNew.setSignal(associationInOld.getSignal());

							}

						}

						// loop over outgoing associations of old unit						
						for (Association associationOutOld : unitOld.getAssociationsOut()) {
							Unit unitDst = associationOutOld.getDst();

							// ignore associations to new unit
							if (unitDst != unitNew) {

								// check if association exists for new unit
								Association associationOutNew = null;
								for (Association associationOut : unitNew.getAssociationsOut()) {
									if (associationOut.getDst() == unitDst) {
										associationOutNew = associationOut;
										break;
									}
								}

								// new unit does not have association
								if (associationOutNew == null) {

									// create association
									associationOutNew = createAssociation(unitNew, unitDst);
									associations.add(associationOutNew);
									unitNew.getAssociationsOut().add(associationOutNew);
									unitDst.getAssociationsIn().add(associationOutNew);

								}

								// copy learning rate for outgoing association
								associationOutNew.setLearningRate(associationOutOld.getLearningRate());

								// copy weight for outgoing association
								associationOutNew.setWeight(associationOutOld.getWeight());

								// copy signal for outgoing association
								associationOutNew.setSignal(associationOutOld.getSignal());

							}

						}

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
	 * Leaks loads of units.
	 */
	public void leakLoads() {
		synchronized (units) {
			for (Unit unit : units) {
				unit.leakLoad();
			}
		}
	}

	/**
	 * Decays activations of units.
	 * <p>
	 * Resembles short-term forgetting/loss of attention.
	 */
	public void decayActivations() {
		synchronized (units) {
			for (Unit unit : units) {
				unit.decayActivation();
			}
		}
	}

	/**
	 * Decays weights of associations.
	 * <p>
	 * Resembles long-term forgetting/trace decay.
	 */
	public void decayWeights() {
		synchronized (associations) {
			for (Association association : associations) {
				association.decayWeight();
			}
		}
	}

	/**
	 * Generates associations between units with input load.
	 */
	public void generateAssociations() {
		synchronized (units) {
			synchronized (associations) {

				// only needed if not fully-connected, otherwise all
				// associations have already been created in insertData()
				if (!fullyConnected) {

					for (Unit unitA : units) {
						if (unitA.getLoad() > REMOVAL_THRESHOLD_LOAD) {

							for (Unit unitB : units) {
								if (unitB.getLoad() > REMOVAL_THRESHOLD_LOAD) {

									// no self-associations
									if (unitA != unitB) {

										// check for associations between unit A and unit B
										boolean associationsExist = false;
										for (Association association : associations) {

											// check for association from unit A to unit B
											if (association.getSrc() == unitA && association.getDst() == unitB) {
												associationsExist = true;
												break;
											}

											// check for association from unit B to unit A
											if (association.getSrc() == unitB && association.getDst() == unitA) {
												associationsExist = true;
												break;
											}

										}

										// create associations between unit A and unit B
										if (!associationsExist) {
											Association associationAB = createAssociation(unitA, unitB);
											associations.add(associationAB);
											unitA.getAssociationsOut().add(associationAB);
											unitB.getAssociationsIn().add(associationAB);
											Association associationBA = createAssociation(unitB, unitA);
											associations.add(associationBA);
											unitB.getAssociationsOut().add(associationBA);
											unitA.getAssociationsIn().add(associationBA);
										}

									}

								}
							}

						}
					}

				}

			}
		}
	}

	/**
	 * Spreads activations over associations, adds inputs to destination units.
	 */
	public void spreadActivations() {
		if (signalAttraction) {
			spreadActivationsAttraction();
		} else {
			spreadActivationsStandard();
		}
	}

	/**
	 * Spreads activations over associations, adds inputs to destination units.
	 * Ignores activation of destination unit (signal attraction).
	 */
	public void spreadActivationsStandard() {
		synchronized (units) {
			synchronized (associations) {

				// transfer activation to associations (signal)

				// loop over units
				for (Unit unit : units) {

					// store activation
					double activation = unit.getActivation();

					// sum up weights of outgoing associations
					double weightSum = 0;
					for (Association association : unit.getAssociationsOut()) {
						weightSum += Math.abs(association.getWeight());
					}

					// activation needs to be spread
					if (weightSum > 0) {

						// unit has enough activation to spread over all associations
						if (weightSum <= Unit.ACTIVATION_MAX) {
							// do not divide by sum
							weightSum = 1;
						}

						// allow spreading of more activation than unit actually has
						if (allowOverspreading) {
							// do not divide by sum
							weightSum = 1;
						}

						// distribute whole activation of unit according to weights
						for (Association association : unit.getAssociationsOut()) {
							double signal = Math.abs(association.getWeight()) / weightSum * activation;

							// sigmoidal spreading
							signal = Math.pow(1.0 - signal, 4.0) - 2.0 * Math.pow(1.0 - signal, 2.0) + 1.0; // effects: weaker decay, longer spreading
							// sigmoid function
							//signal = 1.0 / (1.0 * Math.exp(-15 * (signal - 0.5))); // effects: clear spreading for weights > 0.5
							//signal = 1.0 / (1.0 + Math.exp(-15 * (signal - 0.5)));

							// set signal
							association.setSignal(signal);
						}

					}

				}

				// transfer signals

				// loop over associations
				for (Association association : associations) {
					double signal = association.getSignal();
					//association.setSignal(0);	// do not reset if required for display

					// remove activation from source unit
					association.getSrc().removeActivation(signal);

					// add activation to destination unit
					if (association.getWeight() < 0) {
						association.getDst().addActivation(signal * -1.0);
					} else {
						association.getDst().addActivation(signal);
					}
				}

			}
		}
	}

	/**
	 * Spreads activations over associations, adds inputs to destination units.
	 * Considers activation of destination unit (signal attraction).
	 */
	public void spreadActivationsAttraction() {
		synchronized (units) {
			synchronized (associations) {

				// transfer activation to associations (signal)

				// loop over units
				for (Unit unit : units) {

					// store activation
					double activation = unit.getActivation();

					// sum up signal attraction for outgoing associations
					double attractionSum = 0;
					for (Association association : unit.getAssociationsOut()) {
						double weight = association.getWeight();
						double attraction = Math.abs(weight) * (Unit.ACTIVATION_MAX - association.getDst().getActivation());
						if (weight < 0) {
							attraction = Math.abs(weight) * association.getDst().getActivation();
						}
						attractionSum += attraction;
					}

					// activation needs to be spread
					if (attractionSum > 0) {

						// unit has enough activation to spread over all associations
						if (attractionSum <= Unit.ACTIVATION_MAX) {
							// do not divide by sum
							attractionSum = 1;
						}

						// allow spreading of more activation than unit actually has
						if (allowOverspreading) {
							// do not divide by sum
							attractionSum = 1;
						}

						// distribute whole activation of unit according to weights
						for (Association association : unit.getAssociationsOut()) {
							double weight = association.getWeight();
							double attraction = Math.abs(weight) * (Unit.ACTIVATION_MAX - association.getDst().getActivation());
							if (weight < 0) {
								attraction = Math.abs(weight) * association.getDst().getActivation();
							}
							double signal = attraction / attractionSum * activation;

							// sigmoidal spreading
							signal = Math.pow(1.0 - signal, 4.0) - 2.0 * Math.pow(1.0 - signal, 2.0) + 1.0; // effects: weaker decay, longer spreading
							// sigmoid function
							//signal = 1.0 / (1.0 * Math.exp(-15 * (signal - 0.5))); // effects: clear spreading for weights > 0.5
							//signal = 1.0 / (1.0 + Math.exp(-15 * (signal - 0.5)));

							// set signal
							association.setSignal(signal);
						}

					}

				}

				// transfer signals

				// loop over associations
				for (Association association : associations) {
					double signal = association.getSignal();
					//association.setSignal(0);	// do not reset if required for display

					// remove activation from source unit
					association.getSrc().removeActivation(signal);

					// add activation to destination unit
					if (association.getWeight() < 0) {
						association.getDst().addActivation(signal * -1.0);
					} else {
						association.getDst().addActivation(signal);
					}
				}

			}
		}
	}

	/**
	 * Updates weights of associations depending on activations of connected
	 * units.
	 * <p>
	 * Resembles learning (LTP, LTD).
	 */
	public void updateWeights() {
		synchronized (associations) {
			for (Association association : associations) {
				association.updateWeight();
			}
		}
	}

	/**
	 * Cleans up the network by removing units with low activation and low
	 * weights of connected associations in order to maintain performance.
	 */
	public void cleanUp() {
		synchronized (units) {
			synchronized (associations) {

				// initialise
				LinkedList<Unit> unitsDelete = new LinkedList<Unit>();

				// loop over units
				for (Unit unit : units) {

					// initialise
					boolean removal = true;

					// check for low input load and low activation of unit
					if (unit.getLoad() > REMOVAL_THRESHOLD_LOAD || unit.getActivation() > REMOVAL_THRESHOLD_ACTIVATION) {
						removal = false;
					} else {

						// check for low weights of incoming associations
						for (Association association : unit.getAssociationsIn()) {
							if (Math.abs(association.getWeight()) > REMOVAL_THRESHOLD_WEIGHT) {
								removal = false;
								break;
							}
						}

						// check for low weights of outgoing associations
						for (Association association : unit.getAssociationsOut()) {
							if (Math.abs(association.getWeight()) > REMOVAL_THRESHOLD_WEIGHT) {
								removal = false;
								break;
							}
						}

					}

					// add to delete list
					if (removal) {
						unitsDelete.add(unit);
					}

				}

				// delete unit and associations
				for (Unit unitDelete : unitsDelete) {
					deleteUnit(unitDelete);
				}

			}
		}
	}

}
