package soinnm;

/**
 * Class for a node in the topology.
 * <p>
 * A node stores an id to identify it, holds a real-valued pattern (vector), has
 * a similarity threshold, an accummulated error, a number of signals, and an
 * optional text label (for testing purposes).
 * 
 * @author Matthias Keysermann
 *
 */
public class Node {

	private long id;

	private double[] pattern;

	private double threshold;

	private double error;

	private long numSignals;

	private String label; // for evaluation of classification

	public Node(long id, double[] pattern) {

		// set id
		this.id = id;

		// set pattern
		this.pattern = pattern.clone();

		// initialise threshold
		threshold = 0.0;

		// initialise error
		error = 0.0;

		// initialise number of signals
		numSignals = 0;

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public double[] getPattern() {
		return pattern;
	}

	public void setPattern(double[] pattern) {
		this.pattern = pattern.clone();
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public double getError() {
		return error;
	}

	public void setError(double error) {
		this.error = error;
	}

	public long getNumSignals() {
		return numSignals;
	}

	public void setNumSignals(long numSignals) {
		this.numSignals = numSignals;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
