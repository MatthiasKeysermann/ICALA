package tosam;

/**
 * Class for an association in the network.
 * <p>
 * An association stores an id to identify it, stores the source unit and the
 * destination unit (i.e. connected units), has a weight, a signal and a
 * learning rate.
 * <p>
 * The weight can decay.
 * 
 * @author Matthias Keysermann
 *
 */
public class Association {

	public static final double WEIGHT_MIN = -1.0;

	public static final double WEIGHT_MAX = 1.0;

	public static final double EXTINCTION_STRENGTH = 0.8; // 0.8

	public static final double CORRECTION_STRENGTH = 0.0; // 0.01

	private long id;

	private Unit src;

	private Unit dst;

	private double learningRate;

	private static final double LEARNING_RATE_DEFAULT = 0.04; // 0.04

	private double weight;

	private double signal;

	public Association(long id, Unit src, Unit dst) {
		this.id = id;
		this.src = src;
		this.dst = dst;
		learningRate = LEARNING_RATE_DEFAULT;
		weight = 0;
		signal = 0;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Unit getSrc() {
		return src;
	}

	public void setSrc(Unit src) {
		this.src = src;
	}

	public Unit getDst() {
		return dst;
	}

	public void setDst(Unit dst) {
		this.dst = dst;
	}

	public double getLearningRate() {
		return learningRate;
	}

	public void setLearningRate(double learningRate) {
		this.learningRate = learningRate;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {

		// check limits
		if (weight < WEIGHT_MIN) {
			this.weight = WEIGHT_MIN;
		} else if (weight > WEIGHT_MAX) {
			this.weight = WEIGHT_MAX;
		} else {
			this.weight = weight;
		}

	}

	public double getSignal() {
		return signal;
	}

	public void setSignal(double signal) {
		this.signal = signal;
	}

	public void decayWeight() {

		// exponential decay
		double deltaWeight = weight * -0.000001;

		// update weight
		setWeight(weight + deltaWeight);

	}

	public void updateWeight() {

		// initialise
		double loadSrc = src.getLoad();
		double loadDst = dst.getLoad();
		double actSrc = src.getActivation();
		double actDst = dst.getActivation();

		// source load triggers learning
		double contributionSrc = loadSrc;

		/*
		// sigmoidal shaping
		factorSrc = Math.pow((1.0 - loadSrc), 4) - 2 * Math.pow((1.0 - loadSrc), 2) + 1;
		// exponentiate/strengthen
		factorSrc = Math.pow(factorSrc, 0.5);
		*/
		// amplify
		contributionSrc = Unit.LOAD_MAX - Math.pow(Unit.LOAD_MAX - loadSrc, 2);

		// destination load determines whether learning is excitatory or inhibitory
		double contributionDst = loadDst * (Unit.LOAD_MAX + EXTINCTION_STRENGTH) - EXTINCTION_STRENGTH;

		// exponentiate/strengthen
		contributionDst = Math.pow(contributionDst, 15);

		// combine multiplicatively
		double deltaWeight = contributionSrc * contributionDst;

		// weaken updates for strong weights
		deltaWeight *= Association.WEIGHT_MAX - Math.abs(weight);

		// error correction & consolidation during recall
		double factorCorrection = (actDst - actSrc) * weight;

		// combine additively
		deltaWeight += CORRECTION_STRENGTH * factorCorrection;

		/*
		// blocking effect
		double weightSum = 0;
		for (Association association : dst.getAssociationsIn()) {
			weightSum += association.getWeight() * association.getSrc().getLoad(); // consider only active stimuli
		}
		double factorBlocking = Math.max(0, actDst - weightSum);
		deltaWeight *= factorBlocking;
		*/

		// update weight
		setWeight(weight + deltaWeight * learningRate);

	}

	public String toString() {
		String str = "Association";
		str += "  |  id=" + id;
		str += "  |  srcId=" + src.getId();
		str += "  |  dstId=" + dst.getId();
		str += "  |  weight=" + String.format("%.3f", weight);
		str += "  |  signal=" + String.format("%.3f", signal);
		return str;
	}

}
