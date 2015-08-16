package soinnm;

import java.util.HashMap;
import java.util.LinkedList;

import soinnm.Node;

/**
 * Class for a cluster in the topology.
 * <p>
 * A cluster stores an id to identify it, holds a list of nodes that belong to
 * this clusters, a real-valued mean vector, a real-valued weighted mean vector
 * and a real-valued prototype vector.
 * <p>
 * Provides methods for computing the mean, weighted mean and prototype, and for
 * retrieving the prototype node by either lowest accummulated error or by the
 * highest number of signals.
 * <p>
 * Provides a method for retrieving the label by majority vote.
 * 
 * @author Matthias Keysermann
 *
 */
public class Cluster {

	private long id;

	private LinkedList<Node> nodes;

	private double[] mean;

	private double[] weightedMean;

	private double[] prototype;

	private static enum PROTOTYPE_METHOD {
		BY_NUM_SIGNALS, BY_ERROR
	};

	private PROTOTYPE_METHOD prototypeMethod = PROTOTYPE_METHOD.BY_NUM_SIGNALS;

	public Cluster(long id, LinkedList<Node> nodes) {

		// set id
		this.id = id;

		// set nodes
		this.nodes = nodes;

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public LinkedList<Node> getNodes() {
		return nodes;
	}

	public void setNodes(LinkedList<Node> nodes) {
		this.nodes = nodes;
	}

	public double[] getMean() {
		if (mean == null) {
			computeMean();
		}
		return mean;
	}

	public void computeMean() {
		if (nodes.size() > 0) {
			int inputDim = nodes.getFirst().getPattern().length;
			double[] sum = new double[inputDim];
			double[] pattern;
			for (Node node : nodes) {
				pattern = node.getPattern();
				for (int i = 0; i < inputDim; i++) {
					sum[i] += pattern[i];
				}
			}
			double[] mean = new double[inputDim];
			for (int i = 0; i < inputDim; i++) {
				mean[i] = sum[i] / nodes.size();
			}
			this.mean = mean;
		} else {
			this.mean = null;
		}
	}

	public void unsetMean() {
		mean = null;
	}

	public double[] getVariance() {
		if (nodes.size() > 0) {
			// initialise
			int inputDim = nodes.getFirst().getPattern().length;
			double[] sum = new double[inputDim];
			double[] pattern;
			// calculate mean
			double[] mean = getMean();
			// calculate variance
			for (int i = 0; i < inputDim; i++) {
				sum[i] = 0;
			}
			for (Node node : nodes) {
				pattern = node.getPattern();
				for (int i = 0; i < inputDim; i++) {
					sum[i] += (pattern[i] - mean[i]) * (pattern[i] - mean[i]);
				}
			}
			double[] var = new double[inputDim];
			for (int i = 0; i < inputDim; i++) {
				var[i] = sum[i] / nodes.size();
			}
			return var;
		}
		return null;
	}

	public double[] getMin() {
		if (nodes.size() > 0) {
			int inputDim = nodes.getFirst().getPattern().length;
			double[] min = new double[inputDim];
			for (int i = 0; i < inputDim; i++) {
				min[i] = Double.MAX_VALUE;
			}
			double[] pattern;
			for (Node node : nodes) {
				pattern = node.getPattern();
				for (int i = 0; i < inputDim; i++) {
					if (pattern[i] < min[i]) {
						min[i] = pattern[i];
					}
				}
			}
			return min;
		}
		return null;
	}

	public double[] getMax() {
		if (nodes.size() > 0) {
			int inputDim = nodes.getFirst().getPattern().length;
			double[] max = new double[inputDim];
			for (int i = 0; i < inputDim; i++) {
				max[i] = Double.MIN_VALUE;
			}
			double[] pattern;
			for (Node node : nodes) {
				pattern = node.getPattern();
				for (int i = 0; i < inputDim; i++) {
					if (pattern[i] > max[i]) {
						max[i] = pattern[i];
					}
				}
			}
			return max;
		}
		return null;
	}

	public double[] getWeightedMean() {
		if (weightedMean == null) {
			computeWeightedMean();
		}
		return weightedMean;
	}

	public void computeWeightedMean() {
		if (nodes.size() > 0) {
			int inputDim = nodes.getFirst().getPattern().length;
			double[] patternSum = new double[inputDim];
			double[] pattern;
			long numSignalsSum = 0;
			long numSignals;
			for (Node node : nodes) {
				pattern = node.getPattern();
				numSignals = node.getNumSignals();
				for (int i = 0; i < inputDim; i++) {
					patternSum[i] += pattern[i] * numSignals;
				}
				numSignalsSum += numSignals;
			}
			double[] mean = new double[inputDim];
			for (int i = 0; i < inputDim; i++) {
				mean[i] = patternSum[i] / numSignalsSum;
			}
			this.weightedMean = mean;
		} else {
			this.weightedMean = null;
		}
	}

	public void unsetWeightedMean() {
		weightedMean = null;
	}

	public double[] getPrototype() {
		if (prototype == null) {
			computePrototype();
		}
		return prototype;
	}

	public void computePrototype() {
		Node node = getPrototypeNode();
		if (node != null) {
			prototype = node.getPattern();
		} else {
			prototype = null;
		}
	}

	public void unsetPrototype() {
		prototype = null;
	}

	public Node getPrototypeNode() {
		switch (prototypeMethod) {
		case BY_NUM_SIGNALS:
			return getPrototypeNodeByNumSignals();
		case BY_ERROR:
			return getPrototypeNodeByError();
		}
		return null;
	}

	private Node getPrototypeNodeByNumSignals() {
		if (nodes.size() > 0) {
			long numSignalsMax = Long.MIN_VALUE;
			Node nodeMax = null;
			long numSignals;
			for (Node node : nodes) {
				numSignals = node.getNumSignals();
				if (numSignals > numSignalsMax) {
					numSignalsMax = numSignals;
					nodeMax = node;
				}
			}
			return nodeMax;
		}
		return null;
	}

	private Node getPrototypeNodeByError() {
		if (nodes.size() > 0) {
			double errorMin = Double.MAX_VALUE;
			Node nodeMin = null;
			double error;
			for (Node node : nodes) {
				error = node.getError();
				if (error < errorMin) {
					errorMin = error;
					nodeMin = node;
				}
			}
			return nodeMin;
		}
		return null;
	}

	public String getLabel() {
		return getLabelMajorityVote();
	}

	private String getLabelMajorityVote() {
		HashMap<String, Integer> labelCounts = new HashMap<String, Integer>();
		for (Node node : nodes) {
			String label = node.getLabel();
			Integer count = labelCounts.get(label);
			if (count == null) {
				labelCounts.put(label, 1);
			} else {
				labelCounts.put(label, count + 1);
			}
		}
		int countMax = 0;
		String labelMax = null;
		for (String label : labelCounts.keySet()) {
			int count = labelCounts.get(label);
			if (count > countMax) {
				countMax = count;
				labelMax = label;
			}
		}
		return labelMax;
	}

}
