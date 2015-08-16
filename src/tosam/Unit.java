package tosam;

import java.util.LinkedList;

/**
 * Class for a unit in the network.
 * <p>
 * A unit stores an id to identify it, can hold any kind of data pattern, has an
 * input load and an activation level. Also stores all incoming and all outgoing
 * associations for faster access.
 * <p>
 * The input load can leak and the activation level can decay.
 * 
 * @author Matthias Keysermann
 *
 */
public class Unit {

	private long id;

	private Object data;

	private double load;

	public static final double LOAD_MIN = 0.0;

	public static final double LOAD_MAX = 1.0;

	public static final double ACTIVATION_THRESHOLD = LOAD_MAX;

	private double activation;

	public static final double ACTIVATION_MIN = 0.0;

	public static final double ACTIVATION_MAX = 1.0;

	private LinkedList<Association> associationsIn;

	private LinkedList<Association> associationsOut;

	public Unit(long id, Object data) {
		this.id = id;
		this.data = data;
		activation = ACTIVATION_MIN;
		associationsIn = new LinkedList<Association>();
		associationsOut = new LinkedList<Association>();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public double getLoad() {
		return load;
	}

	public void setLoad(double load) {

		// check limits
		if (load < LOAD_MIN) {
			this.load = LOAD_MIN;
		} else if (load > LOAD_MAX) {
			this.load = LOAD_MAX;
		} else {
			this.load = load;
		}

		// check for firing
		if (this.load >= ACTIVATION_THRESHOLD) {
			setActivation(ACTIVATION_MAX);
		}

		/*
		// build up activation
		addActivation(this.load * 0.1);
		*/

	}

	public double getActivation() {
		return activation;
	}

	public void setActivation(double activation) {

		// check limits
		if (activation < ACTIVATION_MIN) {
			this.activation = ACTIVATION_MIN;
		} else if (activation > ACTIVATION_MAX) {
			this.activation = ACTIVATION_MAX;
		} else {
			this.activation = activation;
		}

	}

	public LinkedList<Association> getAssociationsIn() {
		return associationsIn;
	}

	public void setAssociationsIn(LinkedList<Association> associationsIn) {
		this.associationsIn = associationsIn;
	}

	public LinkedList<Association> getAssociationsOut() {
		return associationsOut;
	}

	public void setAssociationsOut(LinkedList<Association> associationsOut) {
		this.associationsOut = associationsOut;
	}

	public void leakLoad() {

		// exponential leakage
		double deltaLoad = load * -0.2; //-0.2

		// update load
		setLoad(load + deltaLoad);

	}

	public void addLoad(double load) {
		setLoad(this.load + load);
	}

	public void removeLoad(double load) {
		setLoad(this.load - load);
	}

	public void decayActivation() {

		// exponential decay
		double deltaActivation = activation * -0.1; //-0.01

		// initialise weakening factor
		double weakeningFactor = 1.0;

		// weaken changes for high activations (problem: 0 for activation of 1 prevents decay)
		//weakeningFactor = Math.pow(activation, 4) - 2 * Math.pow(activation, 2) + 1;

		// weaken changes for high activations (better: constant decay)
		weakeningFactor = 1.0 / (1.0 + Math.pow(Math.E, 7.0 * (activation - 0.5)));

		// exponentiate/strengthen
		//weakeningFactor = Math.pow(weakeningFactor, 1); //1

		// update activation
		setActivation(activation + deltaActivation * weakeningFactor);

	}

	public void addActivation(double activation) {
		setActivation(this.activation + activation);
	}

	public void removeActivation(double activation) {
		setActivation(this.activation - activation);
	}

	public String toString() {
		String str = "Unit";
		str += "  |  id=" + id;
		str += "  |  data=" + data;
		str += "  |  load=" + String.format("%.3f", load);
		str += "  |  activation=" + String.format("%.3f", activation);
		return str;
	}

}
