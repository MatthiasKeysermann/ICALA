package interactor;

import java.util.Random;

/**
 * Class for generating random real-valued vectors as inputs and writing a
 * real-valued vector to the standard output (console). Basic example of an
 * M-SOINN module.
 * 
 * @see InteractorUDP
 * 
 * @author Matthias Keysermann
 *
 */
public class ConsoleInteractor extends InteractorUDP {

	private Random random;

	public ConsoleInteractor() {
		this("ConsoleInteractor", 5);
	}

	public ConsoleInteractor(String name, int inputDim) {
		super(name, inputDim);
		random = new Random();
	}

	@Override
	protected double[] readInput() {
		double[] input = new double[getTopology().getInputDim()];
		for (int i = 0; i < input.length; i++) {
			input[i] += random.nextGaussian() * 0.1;
		}
		return input;
	}

	@Override
	protected void writeOutput(double[] pattern) {
		System.out.print("Output is");
		if (pattern != null) {
			for (int i = 0; i < pattern.length; i++) {
				System.out.print(String.format("  %.3f", pattern[i]));
			}
		}
		System.out.println();
	}

	@Override
	protected void updateUI() {
		System.out.println("Topology has");
		System.out.println("  " + getTopology().getNodeSet().size() + " nodes");
		System.out.println("  " + getTopology().getEdgeSet().size() + " edges");
		System.out.println("  " + getTopology().getClusterSet().size() + " clusters");
		System.out.println();
	}

	@Override
	protected void shutdown() {
		System.exit(0);
	}

	public static void main(String[] args) {
		ConsoleInteractor consoleInteractor = new ConsoleInteractor();
		consoleInteractor.run();
	}

}
